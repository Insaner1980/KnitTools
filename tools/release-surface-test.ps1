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
        [string]$PassMessage
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
        -Condition ($statusMatches -and $exitMatches) `
        -Message $PassMessage `
        -Details "exit=$($result.ExitCode); expected line '$expectedLine'; output=$($result.Output)"
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

        Test-Mutation `
            -Name "record-audio" `
            -ExpectedStatus "FAIL" `
            -ExpectedCheck "permissions" `
            -PassMessage "RECORD_AUDIO mutation detected" `
            -Mutate {
                param($fixture)
                $path = Join-Path $fixture "app/src/main/AndroidManifest.xml"
                $text = Get-Content -Raw -LiteralPath $path
                Set-FileText -Path $path -Text ($text -replace '(<uses-permission android:name="android.permission.CAMERA" />)', "`$1`r`n    <uses-permission android:name=`"android.permission.RECORD_AUDIO`" />")
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
            -Mutate {
                param($fixture)
                $path = Join-Path $fixture "app/src/main/res/values/strings.xml"
                $text = Get-Content -Raw -LiteralPath $path
                $replacement = '    <string name="leaked_ravelry_secret">phase9-secret-probe</string>' + "`r`n`$1"
                Set-FileText -Path $path -Text ($text -replace '(</resources>)', $replacement)
                $env:KNITTOOLS_RAVELRY_OAUTH2_CLIENT_SECRET = "phase9-secret-probe"
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
            -Name "room-version" `
            -ExpectedStatus "FAIL" `
            -ExpectedCheck "room-schema" `
            -PassMessage "Room schema mutation detected" `
            -Mutate {
                param($fixture)
                $path = Join-Path $fixture "app/src/main/java/com/finnvek/knittools/data/local/KnitToolsDatabase.kt"
                $text = Get-Content -Raw -LiteralPath $path
                Set-FileText -Path $path -Text ($text.Replace("version = 14", "version = 15"))
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
                Set-FileText -Path $path -Text ($text.Replace("CounterLaunchTokenStore.isKnownLaunchId(this@MainActivity, launchId)", "false"))
            }
    }
} finally {
    if ($KeepTemp) {
        Write-Output "[WARN] selftest: kept temp fixture root $TempRoot"
    } elseif (Test-Path -LiteralPath $TempRoot) {
        Remove-Item -LiteralPath $TempRoot -Recurse -Force
    }
}

Write-Output "release-surface-test: $Passed passed, $Failed failed"

if ($Failed -gt 0) {
    exit 1
}

exit 0
