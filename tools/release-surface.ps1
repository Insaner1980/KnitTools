param(
    [string]$RepoRoot = (Join-Path $PSScriptRoot "..")
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

<#
KnitTools release-surface -sopimus

Tama vahti tarkistaa vain KnitToolsissa tarkoituksella paatettyja release- ja arkkitehtuurirajoja.
Se ei ole yleinen tietoturvaskanneri, eika se lue tai tulosta credential-arvoja.

Odotetut sopimusarvot, varmistettu nykyisista lahdetiedostoista:
- Manifest permissions: android.permission.INTERNET, android.permission.VIBRATE, android.permission.CAMERA.
- android.permission.RECORD_AUDIO on kielletty, koska voice/microphone-ominaisuudet on poistettu tuote-/turvapaattoksella.
- Application flags: usesCleartextTraffic=false, allowBackup=false, dataExtractionRules=@xml/data_extraction_rules,
  fullBackupContent=@xml/backup_rules, localeConfig=@xml/locales_config.
- Exported surface:
  activity .MainActivity exported=true
  receiver .widget.CounterWidgetReceiver exported=true
  receiver .widget.CounterWidgetActions exported=false
  provider androidx.core.content.FileProvider exported=false
- FileProvider rootit: files-path progress_photos -> progress_photos/ ja files-path pattern_captures -> pattern_captures/.
  yarn_photos, pattern_pdfs, broad files/cache/external roots ja external storage roots eivat kuulu jaettuun pintaan.
- Release signing gate riippuu KNITTOOLS_* signing -ymparistomuuttujista.
- Firebase Auth/Functions ja Google Services ovat sallittuja vain Ravelry-backendia varten.
  app/google-services.json saa olla paikallinen ignoroitu tiedosto, mutta se ei saa olla git-indexissa.
- debug.credentials.properties on paikallinen, ignoroitu ja git-indexin ulkopuolella.
- Sentry saa olla vain debugImplementation + app/src/debug; app/src/release on no-op eika release/main lahdekoodi saa importata io.sentrya.
- Firebase AI, ML Kit, Gemini/Google Generative AI ja voice/speech dependencyt eivat kuulu projektiin.
- Tunnettuja paikallisista tai ymparistomuuttujista luettuja Ravelry secret -arvoja ei saa esiintya lahteissa,
  resursseissa, BuildConfig/generoiduissa vakioissa, Gradle-tiedostoissa, manifesteissa, testeissa, APK:ssa tai AAB:ssa.
- Room-version luetaan @Database-annotaatiosta. Schema exportin pitaa olla paalla, N.json pitaa loytya,
  ja auto/manual-migraatiopolun pitaa ulottua varhaisimmasta exportoidusta schemasta versioon N.
- Widget counter launch on CounterLaunchTokenStore-tokenilla rajattu, ja OAuth callback ei saa muodostaa counter launchia.
- locales_config.xml ja app/src/main/res/values* kielihakemistot ovat pariteetissa; default values vastaa localea en.
#>

$RepoRoot = (Resolve-Path -LiteralPath $RepoRoot).Path
$AndroidNamespace = "http://schemas.android.com/apk/res/android"
$Results = New-Object System.Collections.Generic.List[object]

function Join-RepoPath {
    param([string]$RelativePath)
    return (Join-Path $RepoRoot $RelativePath)
}

function Read-TextFile {
    param([string]$RelativePath)
    $path = Join-RepoPath $RelativePath
    if (-not (Test-Path -LiteralPath $path)) {
        throw "Missing file: $RelativePath"
    }
    return (Get-Content -Raw -LiteralPath $path)
}

function Read-XmlFile {
    param([string]$RelativePath)
    [xml](Read-TextFile $RelativePath)
}

function Get-AndroidAttribute {
    param(
        [System.Xml.XmlNode]$Node,
        [string]$Name
    )

    if ($null -eq $Node) {
        return $null
    }

    $value = $Node.GetAttribute($Name, $AndroidNamespace)
    if ([string]::IsNullOrEmpty($value)) {
        return $null
    }

    return $value
}

function Get-LineNumber {
    param(
        [string]$RelativePath,
        [string]$Pattern
    )

    $path = Join-RepoPath $RelativePath
    if (-not (Test-Path -LiteralPath $path)) {
        return $null
    }

    $lines = Get-Content -LiteralPath $path
    for ($index = 0; $index -lt $lines.Count; $index++) {
        if ($lines[$index] -match $Pattern) {
            return ($index + 1)
        }
    }

    return $null
}

function Add-CheckResult {
    param(
        [ValidateSet("PASS", "WARN", "FAIL")]
        [string]$Status,
        [string]$Check,
        [string]$Message,
        [string]$RelativePath = "",
        [Nullable[int]]$Line = $null
    )

    $lineText = "[$Status] $Check`: $Message"
    if ($Status -ne "PASS" -and -not [string]::IsNullOrWhiteSpace($RelativePath)) {
        $lineText += " in $RelativePath"
        if ($null -ne $Line) {
            $lineText += " (line $Line)"
        }
    }

    Write-Output $lineText
    $Results.Add(
        [pscustomobject]@{
            Status = $Status
            Check = $Check
            Message = $Message
            Path = $RelativePath
            Line = $Line
        }
    ) | Out-Null
}

function Add-Pass {
    param([string]$Check, [string]$Message)
    Add-CheckResult -Status "PASS" -Check $Check -Message $Message
}

function Add-Warn {
    param([string]$Check, [string]$Message, [string]$RelativePath = "", [Nullable[int]]$Line = $null)
    Add-CheckResult -Status "WARN" -Check $Check -Message $Message -RelativePath $RelativePath -Line $Line
}

function Add-Fail {
    param([string]$Check, [string]$Message, [string]$RelativePath = "", [Nullable[int]]$Line = $null)
    Add-CheckResult -Status "FAIL" -Check $Check -Message $Message -RelativePath $RelativePath -Line $Line
}

function Compare-SetValues {
    param(
        [string[]]$Actual,
        [string[]]$Expected
    )

    $actualSorted = @($Actual | Sort-Object -Unique)
    $expectedSorted = @($Expected | Sort-Object -Unique)
    [pscustomobject]@{
        Missing = @($expectedSorted | Where-Object { $_ -notin $actualSorted })
        Extra = @($actualSorted | Where-Object { $_ -notin $expectedSorted })
        Matches = (@(Compare-Object -ReferenceObject $expectedSorted -DifferenceObject $actualSorted).Count -eq 0)
    }
}

function Get-CodeLines {
    param([string]$RelativePath)

    $path = Join-RepoPath $RelativePath
    if (-not (Test-Path -LiteralPath $path)) {
        return @()
    }

    $rawLines = Get-Content -LiteralPath $path
    $result = @()
    for ($index = 0; $index -lt $rawLines.Count; $index++) {
        $code = ($rawLines[$index] -replace '//.*$', '') -replace '#.*$', ''
        $result += [pscustomobject]@{
            Number = $index + 1
            Text = $code
            Raw = $rawLines[$index]
        }
    }

    return $result
}

function Get-RelativeFiles {
    param(
        [string]$RelativeDirectory,
        [string]$Filter = "*"
    )

    $directory = Join-RepoPath $RelativeDirectory
    if (-not (Test-Path -LiteralPath $directory)) {
        return @()
    }

    return @(
        Get-ChildItem -LiteralPath $directory -Recurse -File -Filter $Filter |
            ForEach-Object {
                $_.FullName.Substring($RepoRoot.Length + 1).Replace("\", "/")
            }
    )
}

function Get-RelativeFilesForRoots {
    param([string[]]$RelativeRoots)

    $files = @()
    foreach ($relativeRoot in $RelativeRoots) {
        $path = Join-RepoPath $relativeRoot
        if (-not (Test-Path -LiteralPath $path)) {
            continue
        }

        if ((Get-Item -LiteralPath $path).PSIsContainer) {
            $files += @(
                Get-ChildItem -LiteralPath $path -Recurse -File |
                    ForEach-Object {
                        $_.FullName.Substring($RepoRoot.Length + 1).Replace("\", "/")
                    }
            )
        } else {
            $files += $relativeRoot.Replace("\", "/")
        }
    }

    return @($files | Sort-Object -Unique)
}

function Invoke-Git {
    param([string[]]$Arguments)

    $output = & git -C $RepoRoot @Arguments 2>$null
    [pscustomobject]@{
        ExitCode = $LASTEXITCODE
        Output = @($output)
    }
}

function Test-ManifestPermissions {
    $check = "permissions"
    $relativePath = "app/src/main/AndroidManifest.xml"

    try {
        $xml = Read-XmlFile $relativePath
        $permissionNodes = @($xml.DocumentElement.SelectNodes("uses-permission"))
        $actual = @(
            $permissionNodes |
                ForEach-Object { Get-AndroidAttribute -Node $_ -Name "name" } |
                Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
        )
        $expected = @(
            "android.permission.INTERNET",
            "android.permission.VIBRATE",
            "android.permission.CAMERA"
        )
        $comparison = Compare-SetValues -Actual $actual -Expected $expected

        if ($comparison.Matches) {
            Add-Pass -Check $check -Message "exact set matches contract"
            return
        }

        $parts = @()
        if ("android.permission.RECORD_AUDIO" -in $actual) {
            $parts += "RECORD_AUDIO is forbidden because voice features were removed by product decision"
        }
        if ($comparison.Missing.Count -gt 0) {
            $parts += "missing: $($comparison.Missing -join ', ')"
        }
        if ($comparison.Extra.Count -gt 0) {
            $parts += "unexpected: $($comparison.Extra -join ', ')"
        }

        $line = Get-LineNumber -RelativePath $relativePath -Pattern "RECORD_AUDIO|uses-permission"
        Add-Fail -Check $check -Message ($parts -join "; ") -RelativePath $relativePath -Line $line
    } catch {
        Add-Fail -Check $check -Message $_.Exception.Message -RelativePath $relativePath
    }
}

function Test-ManifestFlags {
    $check = "manifest-flags"
    $relativePath = "app/src/main/AndroidManifest.xml"

    try {
        $xml = Read-XmlFile $relativePath
        $application = $xml.DocumentElement.SelectSingleNode("application")
        $expected = @(
            [pscustomobject]@{ Name = "usesCleartextTraffic"; Value = "false" },
            [pscustomobject]@{ Name = "allowBackup"; Value = "false" },
            [pscustomobject]@{ Name = "dataExtractionRules"; Value = "@xml/data_extraction_rules" },
            [pscustomobject]@{ Name = "fullBackupContent"; Value = "@xml/backup_rules" },
            [pscustomobject]@{ Name = "localeConfig"; Value = "@xml/locales_config" }
        )

        $problems = @()
        $firstLine = $null
        foreach ($item in $expected) {
            $actual = Get-AndroidAttribute -Node $application -Name $item.Name
            if ($actual -ne $item.Value) {
                $problems += "$($item.Name) expected '$($item.Value)' but was '$actual'"
                if ($null -eq $firstLine) {
                    $firstLine = Get-LineNumber -RelativePath $relativePath -Pattern $item.Name
                }
            }
        }

        if ($problems.Count -eq 0) {
            Add-Pass -Check $check -Message "release and privacy attributes match contract"
        } else {
            Add-Fail -Check $check -Message ($problems -join "; ") -RelativePath $relativePath -Line $firstLine
        }
    } catch {
        Add-Fail -Check $check -Message $_.Exception.Message -RelativePath $relativePath
    }
}

function Test-ExportedSurface {
    $check = "exported-components"
    $relativePath = "app/src/main/AndroidManifest.xml"

    try {
        $xml = Read-XmlFile $relativePath
        $application = $xml.DocumentElement.SelectSingleNode("application")
        $expected = @{
            "activity|.MainActivity" = "true"
            "receiver|.widget.CounterWidgetReceiver" = "true"
            "receiver|.widget.CounterWidgetActions" = "false"
            "provider|androidx.core.content.FileProvider" = "false"
        }

        $components = @{}
        foreach ($tag in @("activity", "receiver", "service", "provider")) {
            foreach ($node in @($application.SelectNodes($tag))) {
                $name = Get-AndroidAttribute -Node $node -Name "name"
                if ([string]::IsNullOrWhiteSpace($name)) {
                    continue
                }
                $key = "$tag|$name"
                $components[$key] = Get-AndroidAttribute -Node $node -Name "exported"
            }
        }

        $problems = @()
        $firstLine = $null
        foreach ($key in $expected.Keys) {
            if (-not $components.ContainsKey($key)) {
                $problems += "expected component '$key' missing"
                if ($null -eq $firstLine) {
                    $firstLine = Get-LineNumber -RelativePath $relativePath -Pattern ([regex]::Escape(($key -split "\|", 2)[1]))
                }
                continue
            }
            if ($components[$key] -ne $expected[$key]) {
                $problems += "component '$key' exported expected '$($expected[$key])' but was '$($components[$key])'"
                if ($null -eq $firstLine) {
                    $firstLine = Get-LineNumber -RelativePath $relativePath -Pattern ([regex]::Escape(($key -split "\|", 2)[1]))
                }
            }
        }

        foreach ($key in $components.Keys) {
            if ($components[$key] -eq "true" -and -not $expected.ContainsKey($key)) {
                $problems += "unexpected exported true component '$key'"
                if ($null -eq $firstLine) {
                    $firstLine = Get-LineNumber -RelativePath $relativePath -Pattern ([regex]::Escape(($key -split "\|", 2)[1]))
                }
            }
            if ($key.StartsWith("provider|") -and $components[$key] -eq "true") {
                $problems += "provider '$key' must not be exported"
                if ($null -eq $firstLine) {
                    $firstLine = Get-LineNumber -RelativePath $relativePath -Pattern ([regex]::Escape(($key -split "\|", 2)[1]))
                }
            }
        }

        if ($problems.Count -eq 0) {
            Add-Pass -Check $check -Message "exported component map matches contract"
        } else {
            Add-Fail -Check $check -Message ($problems -join "; ") -RelativePath $relativePath -Line $firstLine
        }
    } catch {
        Add-Fail -Check $check -Message $_.Exception.Message -RelativePath $relativePath
    }
}

function Test-FileProviderRoots {
    $check = "file-provider-roots"
    $manifestPath = "app/src/main/AndroidManifest.xml"
    $pathsPath = "app/src/main/res/xml/file_paths.xml"

    try {
        $manifest = Read-XmlFile $manifestPath
        $application = $manifest.DocumentElement.SelectSingleNode("application")
        $provider = @($application.SelectNodes("provider")) |
            Where-Object { (Get-AndroidAttribute -Node $_ -Name "name") -eq "androidx.core.content.FileProvider" } |
            Select-Object -First 1

        $problems = @()
        $firstPath = $pathsPath
        $firstLine = $null
        if ($null -eq $provider) {
            $problems += "FileProvider declaration missing"
            $firstPath = $manifestPath
        } else {
            $metadata = @($provider.SelectNodes("meta-data")) |
                Where-Object { (Get-AndroidAttribute -Node $_ -Name "name") -eq "android.support.FILE_PROVIDER_PATHS" } |
                Select-Object -First 1
            $resource = Get-AndroidAttribute -Node $metadata -Name "resource"
            if ($resource -ne "@xml/file_paths") {
                $problems += "FileProvider metadata expected '@xml/file_paths' but was '$resource'"
                $firstPath = $manifestPath
                $firstLine = Get-LineNumber -RelativePath $manifestPath -Pattern "FILE_PROVIDER_PATHS|file_paths"
            }
        }

        $xml = Read-XmlFile $pathsPath
        $expected = @{
            "files-path|progress_photos|progress_photos/" = $true
            "files-path|pattern_captures|pattern_captures/" = $true
        }
        $actual = @{}
        foreach ($node in @($xml.DocumentElement.ChildNodes | Where-Object { $_.NodeType -eq [System.Xml.XmlNodeType]::Element })) {
            $key = "$($node.LocalName)|$($node.GetAttribute("name"))|$($node.GetAttribute("path"))"
            $actual[$key] = $true

            $name = $node.GetAttribute("name")
            $path = $node.GetAttribute("path")
            if ($node.LocalName -in @("root-path", "external-path", "external-files-path", "external-cache-path")) {
                $problems += "broad root '$($node.LocalName)' is not allowed"
            }
            if ($path -in @("", ".", "./", "/", "../")) {
                $problems += "broad path '$path' is not allowed for root '$name'"
            }
            if ($key -notin $expected.Keys) {
                $problems += "unexpected root '$name' path '$path'"
                if ($null -eq $firstLine) {
                    $firstLine = Get-LineNumber -RelativePath $pathsPath -Pattern ([regex]::Escape($name))
                }
            }
        }

        foreach ($key in $expected.Keys) {
            if (-not $actual.ContainsKey($key)) {
                $problems += "missing expected root '$key'"
            }
        }

        if ($problems.Count -eq 0) {
            Add-Pass -Check $check -Message "FileProvider roots match contract"
        } else {
            Add-Fail -Check $check -Message ($problems -join "; ") -RelativePath $firstPath -Line $firstLine
        }
    } catch {
        Add-Fail -Check $check -Message $_.Exception.Message -RelativePath $pathsPath
    }
}

function Test-ReleaseGates {
    $check = "release-gates"
    $relativePath = "app/build.gradle.kts"

    try {
        $text = Read-TextFile $relativePath
        $requiredAnchors = @(
            "releaseSigningEnvNames",
            "releaseSigningAvailable",
            "requiredReleaseEnv",
            "signingConfigs",
            "appReleaseArtifactTasks",
            "missingSigningEnvNames",
            "Release build estetty",
            "VerifyGoogleServicesJsonTask",
            "verifyGoogleServicesJson",
            "firebaseConfiguredArtifactTaskNames",
            "KNITTOOLS_GOOGLE_SERVICES_JSON_BASE64"
        )

        $legacyRavelryAnchors = @(@(
            "releaseRavelryEnvNames",
            "embeddedRavelryCredentialsAllowed",
            "KNITTOOLS_ALLOW_EMBEDDED_RAVELRY_SECRETS",
            "KNITTOOLS_RAVELRY_BASIC_AUTH_USER",
            "KNITTOOLS_RAVELRY_BASIC_AUTH_PASSWORD",
            "KNITTOOLS_RAVELRY_OAUTH2_CLIENT_ID",
            "KNITTOOLS_RAVELRY_OAUTH2_CLIENT_SECRET"
        ) | Where-Object { $text.Contains($_) })

        $missing = @($requiredAnchors | Where-Object { -not $text.Contains($_) })
        if ($missing.Count -eq 0 -and $legacyRavelryAnchors.Count -eq 0) {
            Add-Pass -Check $check -Message "release signing and Firebase config gates match contract"
        } else {
            $parts = @()
            if ($missing.Count -gt 0) {
                $parts += "missing gate anchors: $($missing -join ', ')"
            }
            if ($legacyRavelryAnchors.Count -gt 0) {
                $parts += "legacy Android Ravelry credential gate anchors remain: $($legacyRavelryAnchors -join ', ')"
            }

            $line = Get-LineNumber -RelativePath $relativePath -Pattern "releaseSigningEnvNames|VerifyGoogleServicesJsonTask|KNITTOOLS_RAVELRY|KNITTOOLS_ALLOW_EMBEDDED_RAVELRY_SECRETS"
            Add-Fail -Check $check -Message ($parts -join "; ") -RelativePath $relativePath -Line $line
        }
    } catch {
        Add-Fail -Check $check -Message $_.Exception.Message -RelativePath $relativePath
    }
}

function Test-FirebaseBoundary {
    $check = "firebase-boundary"
    $googleServicesPath = "app/google-services.json"
    $gitignorePath = ".gitignore"
    $firstPath = "app/build.gradle.kts"
    $firstLine = $null

    try {
        $problems = @()
        $trackedGoogleServices = Invoke-Git -Arguments @("ls-files", "--", $googleServicesPath)
        if ($trackedGoogleServices.ExitCode -eq 0 -and $trackedGoogleServices.Output.Count -gt 0) {
            $problems += "app/google-services.json is tracked by git"
            $firstPath = $googleServicesPath
        }

        $ignoredGoogleServices = Invoke-Git -Arguments @("check-ignore", "--no-index", "-q", "--", $googleServicesPath)
        if ($ignoredGoogleServices.ExitCode -ne 0) {
            $problems += ".gitignore does not cover app/google-services.json"
            $firstPath = $gitignorePath
            $firstLine = Get-LineNumber -RelativePath $gitignorePath -Pattern "google-services\.json"
        }

        $appGradle = Read-TextFile "app/build.gradle.kts"
        $catalog = Read-TextFile "gradle/libs.versions.toml"
        $requiredAnchors = @(
            [pscustomobject]@{ Path = "app/build.gradle.kts"; Text = $appGradle; Anchor = 'apply(plugin = "com.google.gms.google-services")' },
            [pscustomobject]@{ Path = "app/build.gradle.kts"; Text = $appGradle; Anchor = "implementation(platform(libs.firebase.bom))" },
            [pscustomobject]@{ Path = "app/build.gradle.kts"; Text = $appGradle; Anchor = "implementation(libs.firebase.auth)" },
            [pscustomobject]@{ Path = "app/build.gradle.kts"; Text = $appGradle; Anchor = "implementation(libs.firebase.functions)" },
            [pscustomobject]@{ Path = "gradle/libs.versions.toml"; Text = $catalog; Anchor = 'firebase-auth = { group = "com.google.firebase", name = "firebase-auth" }' },
            [pscustomobject]@{ Path = "gradle/libs.versions.toml"; Text = $catalog; Anchor = 'firebase-functions = { group = "com.google.firebase", name = "firebase-functions" }' },
            [pscustomobject]@{ Path = "gradle/libs.versions.toml"; Text = $catalog; Anchor = 'google-services = { id = "com.google.gms.google-services"' }
        )

        foreach ($anchor in $requiredAnchors) {
            if (-not $anchor.Text.Contains($anchor.Anchor)) {
                $problems += "missing Firebase/Google Services anchor '$($anchor.Anchor)'"
                if ($null -eq $firstLine) {
                    $firstPath = $anchor.Path
                    $firstLine = Get-LineNumber -RelativePath $anchor.Path -Pattern "firebase|google-services|GoogleServices"
                }
            }
        }

        if ($problems.Count -gt 0) {
            Add-Fail -Check $check -Message ($problems -join "; ") -RelativePath $firstPath -Line $firstLine
        } else {
            Add-Pass -Check $check -Message "Firebase Auth/Functions and ignored Google Services config match contract"
        }
    } catch {
        Add-Fail -Check $check -Message $_.Exception.Message -RelativePath $firstPath -Line $firstLine
    }
}

function Test-DebugCredentialsIgnored {
    $check = "debug-credentials"
    $relativePath = ".gitignore"

    try {
        $tracked = Invoke-Git -Arguments @("ls-files", "--", "debug.credentials.properties")
        if ($tracked.ExitCode -ne 0) {
            Add-Fail -Check $check -Message "git ls-files failed" -RelativePath $relativePath
            return
        }
        if ($tracked.Output.Count -gt 0) {
            Add-Fail -Check $check -Message "debug.credentials.properties is tracked by git" -RelativePath "debug.credentials.properties"
            return
        }

        $ignored = Invoke-Git -Arguments @("check-ignore", "--no-index", "-q", "--", "debug.credentials.properties")
        if ($ignored.ExitCode -ne 0) {
            $line = Get-LineNumber -RelativePath $relativePath -Pattern "debug\.credentials\.properties"
            Add-Fail -Check $check -Message ".gitignore does not cover debug.credentials.properties" -RelativePath $relativePath -Line $line
            return
        }

        Add-Pass -Check $check -Message "debug credentials are ignored and untracked"
    } catch {
        Add-Fail -Check $check -Message $_.Exception.Message -RelativePath $relativePath
    }
}

function Test-SentryBoundary {
    $check = "sentry-boundary"
    $problems = @()
    $firstPath = "app/build.gradle.kts"
    $firstLine = $null

    try {
        $gradleFiles = @(
            "build.gradle.kts",
            "app/build.gradle.kts",
            "baselineprofile/build.gradle.kts"
        ) | Where-Object { Test-Path -LiteralPath (Join-RepoPath $_) }

        foreach ($file in $gradleFiles) {
            foreach ($line in Get-CodeLines $file) {
                if ($line.Text -match '^\s*(implementation|api|releaseImplementation|runtimeOnly|releaseRuntimeOnly|compileOnly|releaseCompileOnly)\s*\(.*(sentry|io\.sentry|libs\.sentry)') {
                    $problems += "release-visible Sentry dependency in $file"
                    if ($null -eq $firstLine) {
                        $firstPath = $file
                        $firstLine = $line.Number
                    }
                }
            }
        }

        foreach ($file in Get-RelativeFiles -RelativeDirectory "app/src/main" -Filter "*.kt") {
            foreach ($line in Get-CodeLines $file) {
                if ($line.Text -match 'io\.sentry|SentryAndroid') {
                    $problems += "main source imports or initializes Sentry in $file"
                    if ($null -eq $firstLine) {
                        $firstPath = $file
                        $firstLine = $line.Number
                    }
                }
            }
        }

        foreach ($file in Get-RelativeFiles -RelativeDirectory "app/src/release" -Filter "*.kt") {
            foreach ($line in Get-CodeLines $file) {
                if ($line.Text -match 'io\.sentry|SentryAndroid') {
                    $problems += "release source imports or initializes Sentry in $file"
                    if ($null -eq $firstLine) {
                        $firstPath = $file
                        $firstLine = $line.Number
                    }
                }
            }
        }

        $releaseInit = "app/src/release/java/com/finnvek/knittools/SentryInit.kt"
        if (-not (Test-Path -LiteralPath (Join-RepoPath $releaseInit))) {
            Add-Warn -Check $check -Message "release no-op SentryInit.kt not found; manual review needed" -RelativePath $releaseInit
            return
        }

        if ($problems.Count -gt 0) {
            Add-Fail -Check $check -Message ($problems -join "; ") -RelativePath $firstPath -Line $firstLine
        } else {
            Add-Pass -Check $check -Message "Sentry remains debug-only with release no-op source"
        }
    } catch {
        Add-Fail -Check $check -Message $_.Exception.Message -RelativePath $firstPath -Line $firstLine
    }
}

function Test-ForbiddenDependencies {
    $check = "forbidden-dependencies"
    $problems = @()
    $firstPath = "app/build.gradle.kts"
    $firstLine = $null

    try {
        $gradleFiles = @(
            "build.gradle.kts",
            "app/build.gradle.kts",
            "baselineprofile/build.gradle.kts",
            "gradle/libs.versions.toml"
        ) | Where-Object { Test-Path -LiteralPath (Join-RepoPath $_) }

        $denyPatterns = @(
            [pscustomobject]@{ Name = "Firebase AI dependency"; Pattern = 'firebase[-.]ai|firebase\.ai|firebase-ai|com\.google\.firebase.*ai' },
            [pscustomobject]@{ Name = "ML Kit dependency"; Pattern = 'com\.google\.mlkit|mlkit|ml-kit|play-services-mlkit' },
            [pscustomobject]@{ Name = "Gemini or Google Generative AI dependency"; Pattern = 'generativeai|generative-ai|gemini|google-ai-client' },
            [pscustomobject]@{ Name = "voice or speech dependency"; Pattern = 'speechrecognizer|speech-recognition|voice-command|text-to-speech|texttospeech|androidx\.speech' }
        )

        foreach ($file in $gradleFiles) {
            foreach ($line in Get-CodeLines $file) {
                foreach ($deny in $denyPatterns) {
                    if ($line.Text -match $deny.Pattern) {
                        $problems += "$($deny.Name) found in $file"
                        if ($null -eq $firstLine) {
                            $firstPath = $file
                            $firstLine = $line.Number
                        }
                    }
                }

                if ($line.Text -match '^\s*(implementation|api|runtimeOnly|compileOnly|debugImplementation|releaseImplementation|testImplementation|androidTestImplementation)\s*\(\s*libs\.firebase\.([A-Za-z0-9_.-]+)') {
                    $alias = $Matches[2]
                    if ($alias -notin @("bom", "auth", "functions")) {
                        $problems += "unapproved Firebase dependency alias '$alias' found in $file"
                        if ($null -eq $firstLine) {
                            $firstPath = $file
                            $firstLine = $line.Number
                        }
                    }
                }

                if ($file -eq "gradle/libs.versions.toml" -and $line.Text -match '^\s*(firebase[-A-Za-z0-9_.]*)\s*=') {
                    $key = $Matches[1]
                    if ($key -notin @("firebaseBom", "firebase-bom", "firebase-auth", "firebase-functions")) {
                        $problems += "unapproved Firebase catalog entry '$key' found in $file"
                        if ($null -eq $firstLine) {
                            $firstPath = $file
                            $firstLine = $line.Number
                        }
                    }
                }

                if ($line.Text -match 'com\.google\.firebase' -and $line.Text -notmatch 'firebase-(bom|auth|functions)') {
                    $problems += "unapproved direct Firebase dependency found in $file"
                    if ($null -eq $firstLine) {
                        $firstPath = $file
                        $firstLine = $line.Number
                    }
                }
            }
        }

        if ($problems.Count -gt 0) {
            Add-Fail -Check $check -Message ($problems -join "; ") -RelativePath $firstPath -Line $firstLine
        } else {
            Add-Pass -Check $check -Message "forbidden platform dependencies are absent"
        }
    } catch {
        Add-Fail -Check $check -Message $_.Exception.Message -RelativePath $firstPath -Line $firstLine
    }
}

function Get-KnownRavelrySecretValues {
    $secretNames = @(
        "KNITTOOLS_RAVELRY_BASIC_AUTH_USER",
        "KNITTOOLS_RAVELRY_BASIC_AUTH_PASSWORD",
        "KNITTOOLS_RAVELRY_OAUTH2_CLIENT_ID",
        "KNITTOOLS_RAVELRY_OAUTH2_CLIENT_SECRET",
        "RAVELRY_CLIENT_ID",
        "RAVELRY_CLIENT_SECRET"
    )

    $values = New-Object System.Collections.Generic.List[string]
    foreach ($name in $secretNames) {
        $value = [Environment]::GetEnvironmentVariable($name)
        if (-not [string]::IsNullOrWhiteSpace($value)) {
            $values.Add($value) | Out-Null
        }
    }

    $debugCredentialsPath = Join-RepoPath "debug.credentials.properties"
    if (Test-Path -LiteralPath $debugCredentialsPath) {
        $properties = New-Object System.Collections.Specialized.OrderedDictionary
        $rawLines = Get-Content -LiteralPath $debugCredentialsPath
        foreach ($line in $rawLines) {
            if ($line -notmatch '^\s*([^#!=:\s][^!=:]*)\s*[=:]\s*(.*)\s*$') {
                continue
            }

            $key = $Matches[1].Trim()
            $value = $Matches[2].Trim()
            if ($key -match '(?i)ravelry' -and $key -match '(?i)(secret|password|client|auth|token)' -and -not [string]::IsNullOrWhiteSpace($value)) {
                $values.Add($value) | Out-Null
            }
        }
    }

    return @(
        $values |
            Where-Object {
                $_.Length -ge 8 -and
                    $_ -notmatch '^(?i)(example|placeholder|dummy|test|knitter|secret|password|client)$'
            } |
            Sort-Object -Unique
    )
}

function Test-TextFileContainsSecret {
    param(
        [string]$RelativePath,
        [string[]]$Secrets
    )

    $path = Join-RepoPath $RelativePath
    try {
        $text = [System.IO.File]::ReadAllText($path)
        foreach ($secret in $Secrets) {
            if ($text.Contains($secret)) {
                return $true
            }
        }
    } catch {
        return $false
    }

    return $false
}

function Test-BinaryFileContainsSecret {
    param(
        [string]$RelativePath,
        [string[]]$Secrets
    )

    $path = Join-RepoPath $RelativePath
    if (-not (Test-Path -LiteralPath $path)) {
        return $false
    }

    $bytes = [System.IO.File]::ReadAllBytes($path)
    foreach ($secret in $Secrets) {
        $needle = [System.Text.Encoding]::UTF8.GetBytes($secret)
        if ($needle.Length -eq 0 -or $bytes.Length -lt $needle.Length) {
            continue
        }

        for ($index = 0; $index -le $bytes.Length - $needle.Length; $index++) {
            $matched = $true
            for ($offset = 0; $offset -lt $needle.Length; $offset++) {
                if ($bytes[$index + $offset] -ne $needle[$offset]) {
                    $matched = $false
                    break
                }
            }

            if ($matched) {
                return $true
            }
        }
    }

    return $false
}

function Test-KnownRavelrySecrets {
    $check = "known-ravelry-secrets"

    try {
        $secrets = @(Get-KnownRavelrySecretValues)
        if ($secrets.Count -eq 0) {
            Add-Pass -Check $check -Message "no known local or environment Ravelry secret values were provided for comparison"
            return
        }

        $textFiles = Get-RelativeFilesForRoots @(
            "app/src/main",
            "app/src/test",
            "app/src/androidTest",
            "app/build/generated",
            "build.gradle.kts",
            "settings.gradle.kts",
            "app/build.gradle.kts",
            "baselineprofile/build.gradle.kts",
            "gradle/libs.versions.toml",
            "gradle/verification-metadata.xml",
            "functions/src",
            "functions/lib",
            "functions/package.json",
            "firebase.json",
            "firestore.rules"
        )

        $binaryFiles = Get-RelativeFilesForRoots @(
            "app/build/outputs/apk",
            "app/build/outputs/bundle"
        )

        $matches = @()
        foreach ($file in $textFiles) {
            if (Test-TextFileContainsSecret -RelativePath $file -Secrets $secrets) {
                $matches += $file
            }
        }
        foreach ($file in $binaryFiles) {
            if (Test-BinaryFileContainsSecret -RelativePath $file -Secrets $secrets) {
                $matches += $file
            }
        }

        if ($matches.Count -gt 0) {
            $firstPath = $matches[0]
            Add-Fail -Check $check -Message "known Ravelry secret value found in $($matches.Count) checked file(s); value redacted" -RelativePath $firstPath
        } else {
            Add-Pass -Check $check -Message "known Ravelry secret values are absent from checked sources, resources, generated constants, tests, APKs, and bundles"
        }
    } catch {
        Add-Fail -Check $check -Message $_.Exception.Message
    }
}

function Test-MigrationPath {
    param(
        [int]$Start,
        [int]$Target,
        [object[]]$Edges
    )

    $queue = New-Object System.Collections.Queue
    $visited = @{}
    $queue.Enqueue($Start)
    $visited["$Start"] = $true

    while ($queue.Count -gt 0) {
        $current = [int]$queue.Dequeue()
        if ($current -eq $Target) {
            return $true
        }

        foreach ($edge in $Edges | Where-Object { $_.From -eq $current }) {
            $next = [int]$edge.To
            if (-not $visited.ContainsKey("$next")) {
                $visited["$next"] = $true
                $queue.Enqueue($next)
            }
        }
    }

    return $false
}

function Test-RoomSchema {
    $check = "room-schema"
    $sourceRoot = "app/src/main/java"
    $firstPath = $sourceRoot
    $firstLine = $null

    try {
        $databaseFiles = @(
            Get-RelativeFiles -RelativeDirectory $sourceRoot -Filter "*.kt" |
                Where-Object { (Read-TextFile $_).Contains("@Database") }
        )

        if ($databaseFiles.Count -eq 0) {
            Add-Fail -Check $check -Message "@Database class not found" -RelativePath $sourceRoot
            return
        }

        if ($databaseFiles.Count -gt 1) {
            Add-Warn -Check $check -Message "multiple @Database classes found; manual review needed" -RelativePath $sourceRoot
            return
        }

        $databasePath = $databaseFiles[0]
        $firstPath = $databasePath
        $databaseText = Read-TextFile $databasePath
        if ($databaseText -notmatch '(?s)@Database\s*\(.*?version\s*=\s*(\d+)') {
            Add-Fail -Check $check -Message "database version could not be parsed" -RelativePath $databasePath -Line (Get-LineNumber -RelativePath $databasePath -Pattern "@Database")
            return
        }
        $version = [int]$Matches[1]

        $problems = @()
        if ($databaseText -notmatch '(?s)@Database\s*\(.*?exportSchema\s*=\s*true') {
            $problems += "exportSchema=true missing"
            $firstLine = Get-LineNumber -RelativePath $databasePath -Pattern "exportSchema"
        }

        $packageName = ""
        if ($databaseText -match 'package\s+([A-Za-z0-9_.]+)') {
            $packageName = $Matches[1]
        }
        $className = ""
        if ($databaseText -match 'abstract\s+class\s+([A-Za-z0-9_]+)\s*:\s*RoomDatabase') {
            $className = $Matches[1]
        }
        if ([string]::IsNullOrWhiteSpace($packageName) -or [string]::IsNullOrWhiteSpace($className)) {
            Add-Fail -Check $check -Message "database package/class could not be parsed" -RelativePath $databasePath
            return
        }

        $schemaRelativeDir = "app/schemas/$packageName.$className"
        $schemaDir = Join-RepoPath $schemaRelativeDir
        if (-not (Test-Path -LiteralPath $schemaDir)) {
            Add-Fail -Check $check -Message "schema directory missing for $packageName.$className" -RelativePath $schemaRelativeDir
            return
        }

        $schemaVersions = @(
            Get-ChildItem -LiteralPath $schemaDir -File -Filter "*.json" |
                ForEach-Object {
                    if ($_.BaseName -match '^\d+$') {
                        [int]$_.BaseName
                    }
                } |
                Sort-Object -Unique
        )

        if ($version -notin $schemaVersions) {
            $problems += "schema export $version.json missing"
            if ($null -eq $firstLine) {
                $firstPath = $schemaRelativeDir
            }
        }

        if ($schemaVersions.Count -eq 0) {
            Add-Fail -Check $check -Message "no exported schema json files found" -RelativePath $schemaRelativeDir
            return
        }

        $edges = @()
        foreach ($match in [regex]::Matches($databaseText, 'AutoMigration\s*\(\s*from\s*=\s*(\d+)\s*,\s*to\s*=\s*(\d+)')) {
            $edges += [pscustomobject]@{ From = [int]$match.Groups[1].Value; To = [int]$match.Groups[2].Value }
        }

        foreach ($file in Get-RelativeFiles -RelativeDirectory $sourceRoot -Filter "*.kt") {
            $text = Read-TextFile $file
            foreach ($match in [regex]::Matches($text, 'KnitToolsDatabase\.MIGRATION_(\d+)_(\d+)')) {
                $edges += [pscustomobject]@{ From = [int]$match.Groups[1].Value; To = [int]$match.Groups[2].Value }
            }
        }

        $edges = @($edges | Sort-Object From, To -Unique)
        $startVersion = [int]($schemaVersions | Measure-Object -Minimum).Minimum
        if ($startVersion -gt 1) {
            Add-Warn -Check $check -Message "schema history starts at $startVersion; path checked from first exported schema only" -RelativePath $schemaRelativeDir
            return
        }

        if (-not (Test-MigrationPath -Start $startVersion -Target $version -Edges $edges)) {
            $problems += "migration path from $startVersion to $version is incomplete"
            if ($null -eq $firstLine) {
                $firstLine = Get-LineNumber -RelativePath $databasePath -Pattern "MIGRATION_|AutoMigration"
            }
        }

        if ($problems.Count -eq 0) {
            Add-Pass -Check $check -Message "version $version schema export and migration path are aligned"
        } else {
            Add-Fail -Check $check -Message ($problems -join "; ") -RelativePath $firstPath -Line $firstLine
        }
    } catch {
        Add-Fail -Check $check -Message $_.Exception.Message -RelativePath $firstPath -Line $firstLine
    }
}

function Test-WidgetOAuthBoundary {
    $check = "widget-oauth-boundary"
    $mainPath = "app/src/main/java/com/finnvek/knittools/MainActivity.kt"
    $requestPath = "app/src/main/java/com/finnvek/knittools/ui/navigation/CounterLaunchRequest.kt"

    try {
        $mainText = Read-TextFile $mainPath
        $requestText = Read-TextFile $requestPath
        $warnings = @()
        $firstPath = $mainPath
        $firstLine = $null

        $anchors = @(
            [pscustomobject]@{ Path = $mainPath; Text = $mainText; Pattern = "CounterLaunchTokenStore.isKnownLaunchId"; Message = "token store validation symbol not found" },
            [pscustomobject]@{ Path = $mainPath; Text = $mainText; Pattern = "CounterLaunchTokenStore.issueLaunchId"; Message = "token issue symbol not found" },
            [pscustomobject]@{ Path = $requestPath; Text = $requestText; Pattern = "if (intentData.isOAuthCallback) return null"; Message = "OAuth callback null guard not found" },
            [pscustomobject]@{ Path = $requestPath; Text = $requestText; Pattern = "if (!intentData.isTrustedCounterLaunch) return null"; Message = "trusted launch guard not found" }
        )

        foreach ($anchor in $anchors) {
            if (-not $anchor.Text.Contains($anchor.Pattern)) {
                $warnings += $anchor.Message
                if ($null -eq $firstLine) {
                    $firstPath = $anchor.Path
                    $firstLine = Get-LineNumber -RelativePath $anchor.Path -Pattern ([regex]::Escape(($anchor.Pattern -split "\.", 2)[0]))
                }
            }
        }

        $start = $mainText.IndexOf("private fun handleOAuthCallbackIfNeeded")
        $end = $mainText.IndexOf("private fun clearOAuthCallbackIntent")
        if ($start -ge 0 -and $end -gt $start) {
            $oauthBody = $mainText.Substring($start, $end - $start)
            if ($oauthBody -match 'toCounterLaunchRequest|CounterLaunchRequest\s*\(|createCounterLaunchIntent') {
                $line = Get-LineNumber -RelativePath $mainPath -Pattern "handleOAuthCallbackIfNeeded"
                Add-Fail -Check $check -Message "OAuth callback handling directly touches counter launch construction path" -RelativePath $mainPath -Line $line
                return
            }
        } else {
            $warnings += "OAuth callback handler bounds could not be parsed"
            if ($null -eq $firstLine) {
                $firstLine = Get-LineNumber -RelativePath $mainPath -Pattern "OAuth|oauth"
            }
        }

        if ($warnings.Count -gt 0) {
            Add-Warn -Check $check -Message (($warnings -join "; ") + "; static drift detector only") -RelativePath $firstPath -Line $firstLine
        } else {
            Add-Pass -Check $check -Message "token gate anchors and OAuth null-guard present"
        }
    } catch {
        Add-Warn -Check $check -Message ("manual review needed: " + $_.Exception.Message) -RelativePath $mainPath
    }
}

function Convert-ValuesDirectoryToLocale {
    param([string]$DirectoryName)

    if ($DirectoryName -eq "values") {
        return "en"
    }
    if (-not $DirectoryName.StartsWith("values-")) {
        return $null
    }

    $qualifier = $DirectoryName.Substring("values-".Length)
    if ($qualifier -match '^([a-z]{2,3})(-r([A-Z]{2}))?$') {
        if ($Matches[3]) {
            return "$($Matches[1])-r$($Matches[3])"
        }
        return $Matches[1]
    }

    if ($qualifier -match '^b\+(.+)$') {
        return ($Matches[1] -replace '\+', '-')
    }

    return $null
}

function Test-LocaleParity {
    $check = "locale-parity"
    $localePath = "app/src/main/res/xml/locales_config.xml"
    $resPath = "app/src/main/res"

    try {
        $xml = Read-XmlFile $localePath
        $declared = @(
            $xml.DocumentElement.SelectNodes("locale") |
                ForEach-Object { Get-AndroidAttribute -Node $_ -Name "name" } |
                Where-Object { -not [string]::IsNullOrWhiteSpace($_) } |
                Sort-Object -Unique
        )

        $resDir = Join-RepoPath $resPath
        $actual = @(
            Get-ChildItem -LiteralPath $resDir -Directory -Filter "values*" |
                ForEach-Object { Convert-ValuesDirectoryToLocale -DirectoryName $_.Name } |
                Where-Object { -not [string]::IsNullOrWhiteSpace($_) } |
                Sort-Object -Unique
        )

        $comparison = Compare-SetValues -Actual $actual -Expected $declared
        if ($comparison.Matches) {
            Add-Pass -Check $check -Message "locale declarations match language resource directories"
            return
        }

        $parts = @()
        if ($comparison.Missing.Count -gt 0) {
            $parts += "declared locale without resources: $($comparison.Missing -join ', ')"
        }
        if ($comparison.Extra.Count -gt 0) {
            $parts += "language resource directory without declaration: $($comparison.Extra -join ', ')"
        }

        $line = Get-LineNumber -RelativePath $localePath -Pattern "locale"
        Add-Fail -Check $check -Message ($parts -join "; ") -RelativePath $localePath -Line $line
    } catch {
        Add-Fail -Check $check -Message $_.Exception.Message -RelativePath $localePath
    }
}

Test-ManifestPermissions
Test-ManifestFlags
Test-ExportedSurface
Test-FileProviderRoots
Test-ReleaseGates
Test-FirebaseBoundary
Test-DebugCredentialsIgnored
Test-SentryBoundary
Test-ForbiddenDependencies
Test-KnownRavelrySecrets
Test-RoomSchema
Test-WidgetOAuthBoundary
Test-LocaleParity

$passed = @($Results | Where-Object { $_.Status -eq "PASS" }).Count
$warned = @($Results | Where-Object { $_.Status -eq "WARN" }).Count
$failed = @($Results | Where-Object { $_.Status -eq "FAIL" }).Count

Write-Output "release-surface: $passed passed, $warned warned, $failed failed"

if ($failed -gt 0) {
    exit 1
}

exit 0
