param(
    [switch]$KeepTemp
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$ScriptPath = Join-Path $PSScriptRoot "release-surface.ps1"
$TempRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("knittools-release-surface-" + [Guid]::NewGuid().ToString("N"))
$Passed = 0
$Failed = 0
$OriginalRavelryClientSecret = [Environment]::GetEnvironmentVariable("KNITTOOLS_RAVELRY_OAUTH2_CLIENT_SECRET", "Process")
$OriginalRavelrySecondarySecret = [Environment]::GetEnvironmentVariable("RAVELRY_CLIENT_SECRET", "Process")

function Add-SelfTestResult {
    param(
        [bool]$Condition,
        [string]$Message,
        [string]$Details = ""
    )

    if ($Condition) {
        $script:Passed++
        Write-Output "[PASS] selftest: $Message"
    } else {
        $script:Failed++
        if ($Details) {
            Write-Output "[FAIL] selftest: $Message - $Details"
        } else {
            Write-Output "[FAIL] selftest: $Message"
        }
    }
}

function Copy-FixtureFile {
    param(
        [string]$RelativePath,
        [string]$TargetRoot
    )

    $source = Join-Path $RepoRoot $RelativePath
    if (-not (Test-Path -LiteralPath $source)) {
        throw "Fixture source missing: $RelativePath"
    }

    $target = Join-Path $TargetRoot $RelativePath
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $target) | Out-Null
    Copy-Item -LiteralPath $source -Destination $target -Force
}

function New-Fixture {
    param(
        [string]$Name
    )

    $fixture = Join-Path $TempRoot $Name
    New-Item -ItemType Directory -Force -Path $fixture | Out-Null

    $files = @(
        ".gitignore",
        "build.gradle.kts",
        "app/build.gradle.kts",
        "baselineprofile/build.gradle.kts",
        "gradle/libs.versions.toml",
        "app/src/main/AndroidManifest.xml",
        "app/src/main/res/xml/file_paths.xml",
        "app/src/main/res/xml/locales_config.xml",
        "app/src/main/java/com/finnvek/knittools/MainActivity.kt",
        "app/src/main/java/com/finnvek/knittools/data/local/KnitToolsDatabase.kt",
        "app/src/main/java/com/finnvek/knittools/data/storage/CounterLaunchTokenStore.kt",
        "app/src/main/java/com/finnvek/knittools/di/DatabaseModule.kt",
        "app/src/main/java/com/finnvek/knittools/ui/navigation/CounterLaunchRequest.kt",
        "app/src/main/java/com/finnvek/knittools/widget/CounterWidget.kt",
        "app/src/debug/java/com/finnvek/knittools/SentryInit.kt",
        "app/src/release/java/com/finnvek/knittools/SentryInit.kt"
    )

    foreach ($file in $files) {
        Copy-FixtureFile -RelativePath $file -TargetRoot $fixture
    }

    $schemaSource = Join-Path $RepoRoot "app/schemas"
    $schemaTarget = Join-Path $fixture "app/schemas"
    New-Item -ItemType Directory -Force -Path $schemaTarget | Out-Null
    Copy-Item -LiteralPath $schemaSource -Destination (Join-Path $fixture "app") -Recurse -Force

    $resSource = Join-Path $RepoRoot "app/src/main/res"
    $resTarget = Join-Path $fixture "app/src/main/res"
    Get-ChildItem -LiteralPath $resSource -Directory -Filter "values*" |
        ForEach-Object {
            Copy-Item -LiteralPath $_.FullName -Destination $resTarget -Recurse -Force
        }

    & git -C $fixture init -q | Out-Null
    & git -C $fixture -c core.autocrlf=false add . 2>$null | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "git add failed for fixture $Name"
    }

    return $fixture
}

function Set-FileText {
    param(
        [string]$Path,
        [string]$Text
    )

    Set-Content -LiteralPath $Path -Value $Text -Encoding UTF8
}

function Restore-ProcessEnvironmentVariable {
    param(
        [string]$Name,
        [AllowNull()]$Value
    )

    if ($null -eq $Value) {
        Remove-Item -LiteralPath "Env:\$Name" -ErrorAction SilentlyContinue
    } else {
        [Environment]::SetEnvironmentVariable($Name, $Value, "Process")
    }
}

function Run-ReleaseSurface {
    param(
        [string]$Fixture
    )

    $pwsh = (Get-Process -Id $PID).Path
    $output = & $pwsh -NoProfile -ExecutionPolicy Bypass -File $ScriptPath -RepoRoot $Fixture 2>&1

    [pscustomobject]@{
        ExitCode = $LASTEXITCODE
        Output = ($output -join "`n")
    }
}

function Test-Mutation {
    param(
        [string]$Name,
        [scriptblock]$Mutate,
        [string]$ExpectedStatus,
        [string]$ExpectedCheck,
        [string]$PassMessage,
        [string]$ForbiddenOutput = "",
        [string]$ExpectedDetail = ""
    )

    $fixture = New-Fixture -Name $Name
    & $Mutate $fixture
    $result = Run-ReleaseSurface -Fixture $fixture
    $expectedLine = "[$ExpectedStatus] $ExpectedCheck" + ":"
    $statusMatches = $result.Output.Contains($expectedLine)
    $exitMatches =
        if ($ExpectedStatus -eq "FAIL") {
            $result.ExitCode -eq 1
        } else {
            $result.ExitCode -eq 0
        }

    Add-SelfTestResult `
        -Condition ($statusMatches -and $exitMatches -and
            (!$ForbiddenOutput -or !$result.Output.Contains($ForbiddenOutput)) -and
            (!$ExpectedDetail -or $result.Output.Contains($ExpectedDetail))) `
        -Message $PassMessage `
        -Details "exit=$($result.ExitCode); expected line '$expectedLine'"
}

function Test-SecretScanReadFailure {
    $fixture = New-Fixture -Name "secret-scan-read-failure"
    $probe = "phase9-secret-probe"
    $relativePath = "app/src/main/$probe-read-failure.txt"
    $path = Join-Path $fixture $relativePath
    Set-FileText -Path $path -Text "locked fixture file"

    $secretName = "KNITTOOLS_RAVELRY_OAUTH2_CLIENT_SECRET"
    $previousSecret = [Environment]::GetEnvironmentVariable($secretName, "Process")
    $stream = $null
    try {
        [Environment]::SetEnvironmentVariable($secretName, $probe, "Process")
        $stream = [System.IO.File]::Open(
            $path,
            [System.IO.FileMode]::Open,
            [System.IO.FileAccess]::ReadWrite,
            [System.IO.FileShare]::None
        )
        $result = Run-ReleaseSurface -Fixture $fixture
        $expectedLine = "[FAIL] known-ravelry-secrets:"
        $expectedMessage = "Unable to read checked file: app/src/main/[redacted]-read-failure.txt"
        Add-SelfTestResult `
            -Condition ($result.ExitCode -eq 1 -and $result.Output.Contains($expectedLine) -and $result.Output.Contains($expectedMessage) -and !$result.Output.Contains($probe)) `
            -Message "known Ravelry secret scan fails closed when a checked file cannot be read" `
            -Details "exit=$($result.ExitCode); expected line '$expectedLine'; expected message '$expectedMessage'"
    } finally {
        if ($null -ne $stream) {
            $stream.Dispose()
        }
        Restore-ProcessEnvironmentVariable -Name $secretName -Value $previousSecret
    }
}

function Test-GitReadFailure {
    $fixture = New-Fixture -Name "git-read-failure"
    Remove-Item -LiteralPath (Join-Path $fixture ".git") -Recurse -Force
    $result = Run-ReleaseSurface -Fixture $fixture
    $expectedLine = "[FAIL] firebase-boundary:"
    $expectedMessage = "git ls-files failed for app/google-services.json"
    Add-SelfTestResult `
        -Condition ($result.ExitCode -eq 1 -and $result.Output.Contains($expectedLine) -and $result.Output.Contains($expectedMessage)) `
        -Message "Firebase boundary fails closed when git tracked-file lookup fails" `
        -Details "exit=$($result.ExitCode); expected line '$expectedLine'; expected message '$expectedMessage'; output=$($result.Output)"
}

try {
    New-Item -ItemType Directory -Force -Path $TempRoot | Out-Null

    if (-not (Test-Path -LiteralPath $ScriptPath)) {
        Add-SelfTestResult -Condition $false -Message "release-surface script exists" -Details $ScriptPath
    } else {
        $baseline = Run-ReleaseSurface -Fixture (New-Fixture -Name "baseline")
        Add-SelfTestResult `
            -Condition ($baseline.ExitCode -eq 0 -and -not $baseline.Output.Contains("[FAIL]")) `
            -Message "baseline fixture passes" `
            -Details "exit=$($baseline.ExitCode); output=$($baseline.Output)"

        Test-SecretScanReadFailure
        Test-GitReadFailure

        Test-Mutation -Name "sdk23-permission" -ExpectedStatus "FAIL" -ExpectedCheck "permissions" -PassMessage "SDK-specific forbidden permission is detected" -Mutate {
            param($fixture)
            $path = Join-Path $fixture "app/src/main/AndroidManifest.xml"
            $text = Get-Content -Raw -LiteralPath $path
            Set-FileText $path ($text.Replace("<application", '<uses-permission-sdk-23 android:name="android.permission.RECORD_AUDIO" /><application'))
        }
        Test-Mutation -Name "direct-sentry-plugin" -ExpectedStatus "FAIL" -ExpectedCheck "sentry-boundary" -PassMessage "direct Sentry plugin is detected" -Mutate {
            param($fixture)
            Add-Content -LiteralPath (Join-Path $fixture "app/build.gradle.kts") -Value 'plugins { id("io.sentry.android.gradle") }'
        }
        Test-Mutation -Name "aliased-sentry-plugin" -ExpectedStatus "FAIL" -ExpectedCheck "sentry-boundary" -PassMessage "renamed catalog Sentry plugin is detected" -Mutate {
            param($fixture)
            Add-Content -LiteralPath (Join-Path $fixture "gradle/libs.versions.toml") -Value 'diagnostics = { id = "io.sentry.android.gradle", version = "1.0" }'
            Add-Content -LiteralPath (Join-Path $fixture "app/build.gradle.kts") -Value 'plugins { alias(libs.plugins.diagnostics) }'
        }
        Test-Mutation -Name "room-branch-hole" -ExpectedStatus "FAIL" -ExpectedCheck "room-schema" -PassMessage "intermediate exported schema must reach current version" -Mutate {
            param($fixture)
            $path = Join-Path $fixture "app/src/main/java/com/finnvek/knittools/data/local/KnitToolsDatabase.kt"
            $text = Get-Content -Raw -LiteralPath $path
            $mutated = $text.Replace("AutoMigration(from = 1, to = 2)", "AutoMigration(from = 1, to = 3)").Replace("AutoMigration(from = 2, to = 3),", "")
            if ($text -ceq $mutated) { throw "Expected auto-migrations are absent" }
            Set-FileText $path $mutated
        }
        Test-Mutation -Name "room-documented-annotation" -ExpectedStatus "PASS" -ExpectedCheck "room-schema" -PassMessage "KDoc does not count as a Room declaration" -Mutate {
            param($fixture)
            Set-FileText (Join-Path $fixture "app/src/main/java/DatabaseNote.kt") '/** The application uses @Database for persistence. */'
        }
        $databaseRelativePath = "app/src/main/java/com/finnvek/knittools/data/local/KnitToolsDatabase.kt"
        $databaseSource = Get-Content -Raw -LiteralPath (Join-Path $RepoRoot $databaseRelativePath)
        $databaseVersion = [regex]::Match($databaseSource, '(?m)^const val KNITTOOLS_DATABASE_VERSION\s*=\s*(\d+)\s*$')
        if (-not $databaseVersion.Success) { throw "KnitTools database version not found" }
        $migrationDetail = "migration path from 25 to $($databaseVersion.Groups[1].Value) is incomplete"
        Test-Mutation -Name "room-greedy-dead-end" -ExpectedStatus "FAIL" -ExpectedCheck "room-schema" -ExpectedDetail $migrationDetail -PassMessage "Room selects the highest eligible upgrade edge without backtracking" -Mutate {
            param($fixture)
            $path = Join-Path $fixture $databaseRelativePath
            $text = Get-Content -Raw -LiteralPath $path
            $text = $text.Replace("MIGRATION_25_26,", "MIGRATION_25_26,`n                    MIGRATION_25_27,").
                Replace("MIGRATION_26_27,", "MIGRATION_26_28,").Replace("MIGRATION_27_28,", "")
            Set-FileText $path $text
            Remove-Item -LiteralPath (Join-Path $fixture "app/schemas/com.finnvek.knittools.data.local.KnitToolsDatabase/27.json")
        }
        Test-Mutation -Name "missing-widget-source" -ExpectedStatus "FAIL" -ExpectedCheck "widget-oauth-boundary" -PassMessage "unreadable widget boundary fails closed" -Mutate {
            param($fixture)
            Remove-Item -LiteralPath (Join-Path $fixture "app/src/main/java/com/finnvek/knittools/ui/navigation/CounterLaunchRequest.kt")
        }

        Test-Mutation `
            -Name "camera-permission" `
            -ExpectedStatus "FAIL" `
            -ExpectedCheck "permissions" `
            -PassMessage "unnecessary CAMERA permission mutation detected" `
            -Mutate {
                param($fixture)
                $path = Join-Path $fixture "app/src/main/AndroidManifest.xml"
                $text = Get-Content -Raw -LiteralPath $path
                Set-FileText -Path $path -Text ($text -replace '(<uses-permission android:name="android.permission.VIBRATE" />)', "`$1`r`n    <uses-permission android:name=`"android.permission.CAMERA`" />")
            }

        Test-Mutation `
            -Name "record-audio" `
            -ExpectedStatus "FAIL" `
            -ExpectedCheck "permissions" `
            -PassMessage "RECORD_AUDIO mutation detected" `
            -Mutate {
                param($fixture)
                $path = Join-Path $fixture "app/src/main/AndroidManifest.xml"
                $text = Get-Content -Raw -LiteralPath $path
                Set-FileText -Path $path -Text ($text -replace '(<uses-permission android:name="android.permission.VIBRATE" />)', "`$1`r`n    <uses-permission android:name=`"android.permission.RECORD_AUDIO`" />")
            }

        Test-Mutation `
            -Name "allow-backup" `
            -ExpectedStatus "FAIL" `
            -ExpectedCheck "manifest-flags" `
            -PassMessage "allowBackup mutation detected" `
            -Mutate {
                param($fixture)
                $path = Join-Path $fixture "app/src/main/AndroidManifest.xml"
                $text = Get-Content -Raw -LiteralPath $path
                Set-FileText -Path $path -Text ($text.Replace('android:allowBackup="false"', 'android:allowBackup="true"'))
            }

        Test-Mutation `
            -Name "exported-widget-action" `
            -ExpectedStatus "FAIL" `
            -ExpectedCheck "exported-components" `
            -PassMessage "exported widget action mutation detected" `
            -Mutate {
                param($fixture)
                $path = Join-Path $fixture "app/src/main/AndroidManifest.xml"
                $text = Get-Content -Raw -LiteralPath $path
                $pattern = '(?s)(android:name="\.widget\.CounterWidgetActions"\s+android:exported=")false"'
                Set-FileText -Path $path -Text ([regex]::Replace($text, $pattern, '${1}true"', 1))
            }

        Test-Mutation `
            -Name "file-provider-root" `
            -ExpectedStatus "FAIL" `
            -ExpectedCheck "file-provider-roots" `
            -PassMessage "FileProvider root mutation detected" `
            -Mutate {
                param($fixture)
                $path = Join-Path $fixture "app/src/main/res/xml/file_paths.xml"
                $text = Get-Content -Raw -LiteralPath $path
                $replacement = '    <files-path name="yarn_photos" path="yarn_photos/" />' + "`r`n`$1"
                Set-FileText -Path $path -Text ($text -replace '(</paths>)', $replacement)
            }

        Test-Mutation -Name "release-source-export" -ExpectedStatus "FAIL" -ExpectedCheck "exported-components" -ExpectedDetail "app/src/release/AndroidManifest.xml" -PassMessage "release source overlay cannot add an exported receiver" -Mutate {
            param($fixture)
            Set-FileText (Join-Path $fixture "app/src/release/AndroidManifest.xml") @'
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application><receiver android:name=".UnexpectedReceiver" android:exported="true" /></application>
</manifest>
'@
        }
        $builtFixture = New-Fixture -Name "packaged-release-exports"
        $builtRelativePath = "app/build/intermediates/packaged_manifests/release/processReleaseManifestForPackage/AndroidManifest.xml"
        $builtPath = Join-Path $builtFixture $builtRelativePath
        New-Item -ItemType Directory -Force -Path (Split-Path -Parent $builtPath) | Out-Null
        $builtManifest = (Get-Content -Raw -LiteralPath (Join-Path $builtFixture "app/src/main/AndroidManifest.xml")).
            Replace('android:name=".MainActivity"', 'android:name="com.finnvek.knittools.MainActivity"').
            Replace('android:name=".widget.', 'android:name="com.finnvek.knittools.widget.')
        $guarded = '<service android:name="androidx.work.impl.background.systemjob.SystemJobService" android:exported="true" android:permission="android.permission.BIND_JOB_SERVICE" />'
        Set-FileText $builtPath ($builtManifest.Replace("</application>", "$guarded</application>"))
        $builtResult = Run-ReleaseSurface -Fixture $builtFixture
        Add-SelfTestResult -Condition ($builtResult.ExitCode -eq 0 -and $builtResult.Output.Contains($builtRelativePath)) -Message "packaged manifest accepts an expected permission-guarded dependency service"
        Set-FileText $builtPath ($builtManifest.Replace("</application>", ($guarded -replace ' android:permission="[^"]+"', '') + "</application>"))
        $builtResult = Run-ReleaseSurface -Fixture $builtFixture
        Add-SelfTestResult -Condition ($builtResult.ExitCode -eq 1 -and
            $builtResult.Output.Contains("[FAIL] exported-components:") -and $builtResult.Output.Contains($builtRelativePath)) -Message "packaged manifest catches an exported service without its permission guard"

        Test-Mutation -Name "local-only-ignore" -ExpectedStatus "FAIL" -ExpectedCheck "firebase-boundary" -ExpectedDetail "[FAIL] debug-credentials:" -PassMessage "Git's local excludes cannot replace tracked ignore rules" -Mutate {
            param($fixture)
            $ignore = Join-Path $fixture ".gitignore"
            $rules = Get-Content -LiteralPath $ignore | Where-Object { $_ -notmatch 'google-services\.json|debug\.credentials\.properties' }
            Set-Content -LiteralPath $ignore -Value $rules
            Add-Content -LiteralPath (Join-Path $fixture ".git/info/exclude") -Value @(
                '/debug.credentials.properties', 'app/google-services.json', 'app/src/*/google-services.json'
            )
        }

        Test-Mutation `
            -Name "google-services" `
            -ExpectedStatus "FAIL" `
            -ExpectedCheck "firebase-boundary" `
            -PassMessage "tracked google-services.json mutation detected" `
            -Mutate {
                param($fixture)
                $path = Join-Path $fixture "app/google-services.json"
                New-Item -ItemType Directory -Force -Path (Split-Path -Parent $path) | Out-Null
                Set-FileText -Path $path -Text "{}"
                & git -C $fixture add -f app/google-services.json 2>$null | Out-Null
                if ($LASTEXITCODE -ne 0) {
                    throw "git add google-services.json mutation failed"
                }
            }

        Test-Mutation `
            -Name "release-gate" `
            -ExpectedStatus "FAIL" `
            -ExpectedCheck "release-gates" `
            -PassMessage "release signing gate mutation detected" `
            -Mutate {
                param($fixture)
                $path = Join-Path $fixture "app/build.gradle.kts"
                $text = Get-Content -Raw -LiteralPath $path
                Set-FileText -Path $path -Text ($text.Replace("releaseSigningEnvNames", "releaseCredentialEnvNames"))
            }

        Test-Mutation `
            -Name "tracked-debug-credentials" `
            -ExpectedStatus "FAIL" `
            -ExpectedCheck "debug-credentials" `
            -PassMessage "tracked debug credentials mutation detected" `
            -Mutate {
                param($fixture)
                $path = Join-Path $fixture "debug.credentials.properties"
                Set-FileText -Path $path -Text "test=true"
                & git -C $fixture add -f debug.credentials.properties 2>$null | Out-Null
                if ($LASTEXITCODE -ne 0) {
                    throw "git add debug.credentials.properties mutation failed"
                }
            }

        Test-Mutation `
            -Name "analytics-dependency" `
            -ExpectedStatus "FAIL" `
            -ExpectedCheck "forbidden-dependencies" `
            -PassMessage "Firebase Analytics remains forbidden after enabling Crashlytics" `
            -Mutate {
                param($fixture)
                $path = Join-Path $fixture "app/build.gradle.kts"
                $text = Get-Content -Raw -LiteralPath $path
                Set-FileText -Path $path -Text ($text + "`nimplementation(libs.firebase.analytics)`n")
            }

        Test-Mutation `
            -Name "mlkit-dependency" `
            -ExpectedStatus "FAIL" `
            -ExpectedCheck "forbidden-dependencies" `
            -PassMessage "ML Kit dependency mutation detected" `
            -Mutate {
                param($fixture)
                $path = Join-Path $fixture "gradle/libs.versions.toml"
                $text = Get-Content -Raw -LiteralPath $path
                Set-FileText -Path $path -Text ($text + "`r`nmlkitBarcode = { group = `"com.google.mlkit`", name = `"barcode-scanning`", version = `"17.3.0`" }`r`n")
            }

        Test-Mutation `
            -Name "known-ravelry-secret" `
            -ExpectedStatus "FAIL" `
            -ExpectedCheck "known-ravelry-secrets" `
            -PassMessage "known Ravelry secret value mutation detected without printing secret" `
            -ForbiddenOutput "phase9-secret-probe" `
            -Mutate {
                param($fixture)
                $path = Join-Path $fixture "app/src/main/phase9-secret-probe.txt"
                Set-FileText -Path $path -Text "phase9-secret-probe"
                $env:KNITTOOLS_RAVELRY_OAUTH2_CLIENT_SECRET = "phase9-secret-probe"
            }

        Test-Mutation -Name "known-ravelry-secret-variants" -ExpectedStatus "FAIL" -ExpectedCheck "known-ravelry-secrets" -ExpectedDetail "found in 2 checked file(s)" -ForbiddenOutput "SYNTHETIC-CASE-SECRET" -PassMessage "case-distinct known values are checked in variant sources without value output" -Mutate {
            param($fixture)
            $env:KNITTOOLS_RAVELRY_OAUTH2_CLIENT_SECRET = "synthetic-case-secret"
            $env:RAVELRY_CLIENT_SECRET = "SYNTHETIC-CASE-SECRET"
            $res = Join-Path $fixture "app/src/release/res/values"
            New-Item -ItemType Directory -Force -Path $res | Out-Null
            Set-FileText (Join-Path $res "probe1.xml") '<resources><string name="probe">synthetic-case-secret</string></resources>'
            Set-FileText (Join-Path $res "probe2.xml") '<resources><string name="probe">SYNTHETIC-CASE-SECRET</string></resources>'
        }

        Test-Mutation `
            -Name "release-sentry" `
            -ExpectedStatus "FAIL" `
            -ExpectedCheck "sentry-boundary" `
            -PassMessage "release-visible Sentry mutation detected" `
            -Mutate {
                param($fixture)
                $path = Join-Path $fixture "app/build.gradle.kts"
                $text = Get-Content -Raw -LiteralPath $path
                Set-FileText -Path $path -Text ($text -replace '(debugImplementation\(libs\.sentry\.android\.core\))', "`$1`r`n    implementation(libs.sentry.android.core)")
            }

        Test-Mutation `
            -Name "release-sentry-missing-noop" `
            -ExpectedStatus "FAIL" `
            -ExpectedCheck "sentry-boundary" `
            -PassMessage "missing release Sentry no-op mutation detected" `
            -Mutate {
                param($fixture)
                Remove-Item -LiteralPath (Join-Path $fixture "app/src/release/java/com/finnvek/knittools/SentryInit.kt") -Force
            }

        Test-Mutation `
            -Name "room-version" `
            -ExpectedStatus "FAIL" `
            -ExpectedCheck "room-schema" `
            -PassMessage "Room schema mutation detected" `
            -Mutate {
                param($fixture)
                $path = Join-Path $fixture "app/src/main/java/com/finnvek/knittools/data/local/KnitToolsDatabase.kt"
                $text = Get-Content -Raw -LiteralPath $path
                $versionPattern = [regex]'(?m)^(\s*const\s+val\s+KNITTOOLS_DATABASE_VERSION\s*=\s*)(\d+)'
                $versionMatch = $versionPattern.Match($text)
                if (-not $versionMatch.Success) {
                    throw "Room database version not found"
                }
                $nextVersion = ([int]$versionMatch.Groups[2].Value) + 1
                Set-FileText -Path $path -Text ($versionPattern.Replace($text, ('${1}' + $nextVersion), 1))
            }

        Test-Mutation `
            -Name "room-migration-edge" `
            -ExpectedStatus "FAIL" `
            -ExpectedCheck "room-schema" `
            -PassMessage "Room migration edge mutation detected" `
            -Mutate {
                param($fixture)
                $path = Join-Path $fixture "app/src/main/java/com/finnvek/knittools/data/local/KnitToolsDatabase.kt"
                $text = Get-Content -Raw -LiteralPath $path
                $mutated = [regex]::Replace($text, '(?m)^\s*MIGRATION_16_17,\s*\r?\n', "", 1)
                if ($mutated -ceq $text) {
                    throw "Room migration edge not found"
                }
                Set-FileText -Path $path -Text $mutated
            }

        Test-Mutation `
            -Name "room-migration-registration" `
            -ExpectedStatus "FAIL" `
            -ExpectedCheck "room-schema" `
            -PassMessage "Room migration registration-call mutation detected" `
            -Mutate {
                param($fixture)
                $path = Join-Path $fixture "app/src/main/java/com/finnvek/knittools/di/DatabaseModule.kt"
                $text = Get-Content -Raw -LiteralPath $path
                $mutated = $text.Replace(".addMigrations(*KnitToolsDatabase.ALL_MANUAL_MIGRATIONS)", ".alsoRegisterMigrations(*KnitToolsDatabase.ALL_MANUAL_MIGRATIONS)")
                if ($mutated -ceq $text) {
                    throw "Room migration registration call not found"
                }
                Set-FileText -Path $path -Text $mutated
            }

        Test-Mutation `
            -Name "room-migration-registration-pruned-history" `
            -ExpectedStatus "FAIL" `
            -ExpectedCheck "room-schema" `
            -PassMessage "Room migration registration remains blocking with pruned schema history" `
            -Mutate {
                param($fixture)
                $registrationPath = Join-Path $fixture "app/src/main/java/com/finnvek/knittools/di/DatabaseModule.kt"
                $registrationText = Get-Content -Raw -LiteralPath $registrationPath
                $mutated = $registrationText.Replace(
                    ".addMigrations(*KnitToolsDatabase.ALL_MANUAL_MIGRATIONS)",
                    ".alsoRegisterMigrations(*KnitToolsDatabase.ALL_MANUAL_MIGRATIONS)"
                )
                if ($mutated -ceq $registrationText) {
                    throw "Room migration registration call not found"
                }
                Set-FileText -Path $registrationPath -Text $mutated
                $firstSchema =
                    Join-Path `
                        $fixture `
                        "app/schemas/com.finnvek.knittools.data.local.KnitToolsDatabase/1.json"
                Remove-Item -LiteralPath $firstSchema -Force
            }

        Test-Mutation `
            -Name "locale-region-undeclared" `
            -ExpectedStatus "FAIL" `
            -ExpectedCheck "locale-parity" `
            -PassMessage "regional resource directory requires its BCP 47 declaration" `
            -Mutate {
                param($fixture)
                $path = Join-Path $fixture "app/src/main/res/xml/locales_config.xml"
                $text = Get-Content -Raw -LiteralPath $path
                $mutated = $text.Replace('<locale android:name="pt-BR" />', '')
                if ($mutated -ceq $text) {
                    throw "Brazilian Portuguese locale declaration not found"
                }
                Set-FileText -Path $path -Text $mutated
            }

        Test-Mutation `
            -Name "locale-extra" `
            -ExpectedStatus "FAIL" `
            -ExpectedCheck "locale-parity" `
            -PassMessage "locale resource mutation detected" `
            -Mutate {
                param($fixture)
                $path = Join-Path $fixture "app/src/main/res/values-pl/strings.xml"
                New-Item -ItemType Directory -Force -Path (Split-Path -Parent $path) | Out-Null
                Set-FileText -Path $path -Text '<?xml version="1.0" encoding="utf-8"?><resources><string name="app_name">KnitTools</string></resources>'
            }

        Test-Mutation `
            -Name "widget-token" `
            -ExpectedStatus "WARN" `
            -ExpectedCheck "widget-oauth-boundary" `
            -PassMessage "widget token-store anchor mutation detected" `
            -Mutate {
                param($fixture)
                $path = Join-Path $fixture "app/src/main/java/com/finnvek/knittools/MainActivity.kt"
                $text = Get-Content -Raw -LiteralPath $path
                $mutated = $text.Replace("CounterLaunchTokenStore.consumeLaunchId(this@MainActivity, candidateLaunchId)", "false")
                if ($mutated -ceq $text) {
                    throw "Widget launch token consumption call not found"
                }
                Set-FileText -Path $path -Text $mutated
            }

        Test-Mutation -Name "locale-configuration-qualifiers" -ExpectedStatus "PASS" -ExpectedCheck "locale-parity" -PassMessage "car mode is excluded and language tags omit their configuration qualifiers" -Mutate {
            param($fixture)
            foreach ($directory in @("values-car", "values-b+sr+Latn-night", "values-fil")) {
                New-Item -ItemType Directory -Path (Join-Path $fixture "app/src/main/res/$directory") | Out-Null
            }
            $path = Join-Path $fixture "app/src/main/res/xml/locales_config.xml"
            $text = Get-Content -Raw -LiteralPath $path
            Set-FileText $path ($text.Replace("</locale-config>", '<locale android:name="sr-Latn" /><locale android:name="fil" /></locale-config>'))
        }

        Test-Mutation `
            -Name "widget-oauth-counter-launch" `
            -ExpectedStatus "FAIL" `
            -ExpectedCheck "widget-oauth-boundary" `
            -PassMessage "OAuth callback reintroduction detected" `
            -Mutate {
                param($fixture)
                $path = Join-Path $fixture "app/src/main/AndroidManifest.xml"
                $text = Get-Content -Raw -LiteralPath $path
                $filter = '<data android:scheme="knittools" android:host="ravelry-auth-complete" />'
                Set-FileText -Path $path -Text ($text.Replace("</activity>", "$filter`r`n        </activity>"))
            }
    }
} finally {
    Restore-ProcessEnvironmentVariable -Name "RAVELRY_CLIENT_SECRET" -Value $OriginalRavelrySecondarySecret
    Restore-ProcessEnvironmentVariable `
        -Name "KNITTOOLS_RAVELRY_OAUTH2_CLIENT_SECRET" `
        -Value $OriginalRavelryClientSecret
    Add-SelfTestResult `
        -Condition ([Environment]::GetEnvironmentVariable("KNITTOOLS_RAVELRY_OAUTH2_CLIENT_SECRET", "Process") -ceq $OriginalRavelryClientSecret) `
        -Message "mutation environment restored" `
        -Details "originalNull=$($null -eq $OriginalRavelryClientSecret); currentNull=$($null -eq [Environment]::GetEnvironmentVariable('KNITTOOLS_RAVELRY_OAUTH2_CLIENT_SECRET', 'Process'))"

    if ($KeepTemp) {
        Write-Output "[WARN] selftest: kept temp fixture root $TempRoot"
    } elseif (Test-Path -LiteralPath $TempRoot) {
        $resolved = [IO.Path]::GetFullPath($TempRoot)
        $tempParent = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
        if (-not $resolved.StartsWith($tempParent, [StringComparison]::OrdinalIgnoreCase) -or
            (Get-Item -LiteralPath $resolved).Attributes.HasFlag([IO.FileAttributes]::ReparsePoint) -or
            @(Get-ChildItem -LiteralPath $resolved -Recurse -Force -Attributes ReparsePoint).Count -gt 0) {
            throw "Unsafe fixture cleanup path: $resolved"
        }
        Remove-Item -LiteralPath $TempRoot -Recurse -Force
        if (Test-Path -LiteralPath $TempRoot) { throw "Fixture cleanup failed: $TempRoot" }
    }
}

Write-Output "release-surface-test: $Passed passed, $Failed failed"

if ($Failed -gt 0) {
    exit 1
}

exit 0
