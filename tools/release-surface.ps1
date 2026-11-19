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
- Manifest permissions: android.permission.INTERNET, android.permission.VIBRATE.
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
- Google Services -konfiguraatio on sallittu vain Firebase Crashlyticsia varten; Firebase Auth/Functions eivat kuulu Androidiin.
  Firebase Crashlytics SDK ja Gradle-plugin ovat sallittuja julkaisuversion kaatumisraportointiin.
  app/google-services.json saa olla paikallinen ignoroitu tiedosto, ja debug-build voi luoda
  app/src/debug/google-services.json -placeholderin, mutta kumpikaan ei saa olla git-indexissa.
- debug.credentials.properties on paikallinen, ignoroitu ja git-indexin ulkopuolella.
- Sentry saa olla vain debugImplementation + app/src/debug; app/src/release on no-op eika release/main lahdekoodi saa importata io.sentrya.
- Firebase AI, ML Kit, Gemini/Google Generative AI ja voice/speech dependencyt eivat kuulu projektiin.
- Tunnettuja paikallisista tai ymparistomuuttujista luettuja Ravelry secret -arvoja ei saa esiintya lahteissa,
  resursseissa, BuildConfig/generoiduissa vakioissa, Gradle-tiedostoissa, manifesteissa, testeissa, APK:ssa tai AAB:ssa.
- Room-version luetaan @Database-annotaatiosta. Schema exportin pitaa olla paalla, N.json pitaa loytya,
  ja auto/manual-migraatiopolun pitaa ulottua varhaisimmasta exportoidusta schemasta versioon N.
- Widget counter launch on CounterLaunchTokenStore-tokenilla rajattu, eikä poistettua Ravelry-OAuth-callbackia saa palauttaa.
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

    $lines = @(Get-Content -LiteralPath $path)
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

    $rawLines = @(Get-Content -LiteralPath $path)
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

    $previousErrorActionPreference = $ErrorActionPreference
    try {
        # Windows PowerShell 5.1 muuntaa natiiviprosessin stderr-rivit virhetietueiksi.
        # Exit-koodi on tämän sopimuksen lähdetotuus, joten kerää tulos ilman terminating erroria.
        $ErrorActionPreference = "Continue"
        $output = & git -C $RepoRoot @Arguments 2>$null
        $exitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    [pscustomobject]@{
        ExitCode = $exitCode
        Output = @($output)
    }
}

function Test-RepositoryIgnoreRule {
    param([string]$RelativePath)

    $ignore = Invoke-Git -Arguments @("check-ignore", "--no-index", "-v", "--", $RelativePath)
    if ($ignore.ExitCode -ne 0 -or $ignore.Output.Count -ne 1) { return $false }
    if ($ignore.Output[0] -notmatch '^(?<file>(?:[^:\t]+/)?\.gitignore):\d+:[^!\t][^\t]*\t') {
        return $false
    }

    $tracked = Invoke-Git -Arguments @("ls-files", "--error-unmatch", "--", $Matches.file)
    return $tracked.ExitCode -eq 0
}

function Test-ManifestPermissions {
    $check = "permissions"
    $relativePath = "app/src/main/AndroidManifest.xml"

    try {
        $xml = Read-XmlFile $relativePath
        $permissionNodes = @($xml.DocumentElement.SelectNodes("uses-permission|uses-permission-sdk-23"))
        $actual = @(
            $permissionNodes |
                ForEach-Object { Get-AndroidAttribute -Node $_ -Name "name" } |
                Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
        )
        $expected = @(
            "android.permission.INTERNET",
            "android.permission.VIBRATE"
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
        $package = "com.finnvek.knittools"
        $expected = @{
            "activity|${package}.MainActivity" = "true"
            "receiver|${package}.widget.CounterWidgetReceiver" = "true"
            "receiver|${package}.widget.CounterWidgetActions" = "false"
            "provider|androidx.core.content.FileProvider" = "false"
        }
        $dependencyPermissions = @{
            "service|androidx.glance.appwidget.GlanceRemoteViewsService" = "android.permission.BIND_REMOTEVIEWS"
            "service|androidx.work.impl.background.systemjob.SystemJobService" = "android.permission.BIND_JOB_SERVICE"
            "receiver|androidx.work.impl.diagnostics.DiagnosticsReceiver" = "android.permission.DUMP"
            "receiver|androidx.profileinstaller.ProfileInstallReceiver" = "android.permission.DUMP"
        }

        $manifestInputs = @([pscustomobject]@{ Path = $relativePath; Overlay = $false; Built = $false })
        $overlay = "app/src/release/AndroidManifest.xml"
        if (Test-Path -LiteralPath (Join-RepoPath $overlay)) {
            $manifestInputs += [pscustomobject]@{ Path = $overlay; Overlay = $true; Built = $false }
        }
        $built = @(
            "app/build/intermediates/packaged_manifests/release/processReleaseManifestForPackage/AndroidManifest.xml",
            "app/build/intermediates/merged_manifests/release/processReleaseManifest/AndroidManifest.xml"
        ) | Where-Object { Test-Path -LiteralPath (Join-RepoPath $_) -PathType Leaf } | Select-Object -First 1
        if ($built) {
            $manifestInputs += [pscustomobject]@{ Path = $built; Overlay = $false; Built = $true }
        }

        $problems = @()
        $firstPath = $null
        $firstLine = $null
        foreach ($inputManifest in $manifestInputs) {
            $relativePath = $inputManifest.Path
            $xml = Read-XmlFile $relativePath
            $components = @{}
            $toolsNamespace = "http://schemas.android.com/tools"
            foreach ($node in @($xml.DocumentElement.SelectNodes(
                "application/activity|application/activity-alias|application/receiver|application/service|application/provider"
            ))) {
                $name = Get-AndroidAttribute -Node $node -Name "name"
                $exported = Get-AndroidAttribute -Node $node -Name "exported"
                $permission = Get-AndroidAttribute -Node $node -Name "permission"
                $nodeOperation = $node.GetAttribute("node", $toolsNamespace)
                if ($name -and $name.StartsWith(".")) {
                    $key = "$($node.LocalName)|$package$name"
                } elseif ($name -and !$name.Contains(".")) {
                    $key = "$($node.LocalName)|$package.$name"
                } else {
                    $key = "$($node.LocalName)|$name"
                }
                $components[$key] = $true
                $problem = $null
                if ($expected.ContainsKey($key)) {
                    if ($nodeOperation -in @("remove", "removeAll")) {
                        $problem = "expected component '$key' is removed"
                    } elseif ((!$inputManifest.Overlay -or $exported -or $nodeOperation -eq "replace") -and
                        $exported -ne $expected[$key]) {
                        $problem = "component '$key' exported expected '$($expected[$key])' but was '$exported'"
                    }
                } elseif ($nodeOperation -notin @("remove", "removeAll") -and $exported -and $exported -ne "false" -and
                    !($inputManifest.Built -and $exported -eq "true" -and
                        $dependencyPermissions.ContainsKey($key) -and $permission -eq $dependencyPermissions[$key])) {
                    $problem = "unexpected exported component '$key'"
                }
                if ($problem) {
                    $problems += "$problem in $relativePath"
                    if (!$firstPath) {
                        $firstPath = $relativePath
                        if ($name) {
                            $firstLine = Get-LineNumber -RelativePath $relativePath -Pattern ([regex]::Escape($name))
                        }
                    }
                }
            }
            if (!$inputManifest.Overlay) {
                foreach ($key in $expected.Keys | Where-Object { !$components.ContainsKey($_) }) {
                    $problems += "expected component '$key' missing in $relativePath"
                    if (!$firstPath) { $firstPath = $relativePath }
                }
            }
        }

        if ($problems.Count -gt 0) {
            Add-Fail -Check $check -Message ($problems -join "; ") -RelativePath $firstPath -Line $firstLine
        } elseif ($built) {
            Add-Pass -Check $check -Message "exported components match contract in source and $built"
        } else {
            Add-Pass -Check $check -Message "main and release source manifest exported components match contract (built release manifest unavailable)"
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
            "GoogleServicesJsonTaskActions.verify",
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

            $line = Get-LineNumber -RelativePath $relativePath -Pattern "releaseSigningEnvNames|GoogleServicesJsonTaskActions|KNITTOOLS_RAVELRY|KNITTOOLS_ALLOW_EMBEDDED_RAVELRY_SECRETS"
            Add-Fail -Check $check -Message ($parts -join "; ") -RelativePath $relativePath -Line $line
        }
    } catch {
        Add-Fail -Check $check -Message $_.Exception.Message -RelativePath $relativePath
    }
}

function Test-FirebaseBoundary {
    $check = "firebase-boundary"
    $googleServicesPaths = @(
        "app/google-services.json",
        "app/src/debug/google-services.json"
    )
    $gitignorePath = ".gitignore"
    $firstPath = "app/build.gradle.kts"
    $firstLine = $null

    try {
        $problems = @()
        foreach ($googleServicesPath in $googleServicesPaths) {
            $trackedGoogleServices = Invoke-Git -Arguments @("ls-files", "--", $googleServicesPath)
            if ($trackedGoogleServices.ExitCode -ne 0) {
                $problems += "git ls-files failed for $googleServicesPath"
                $firstPath = $gitignorePath
            } elseif ($trackedGoogleServices.Output.Count -gt 0) {
                $problems += "$googleServicesPath is tracked by git"
                $firstPath = $googleServicesPath
            }

            if (-not (Test-RepositoryIgnoreRule $googleServicesPath)) {
                $problems += ".gitignore does not cover $googleServicesPath"
                $firstPath = $gitignorePath
                $firstLine = Get-LineNumber -RelativePath $gitignorePath -Pattern "google-services\.json"
            }
        }

        $appGradle = Read-TextFile "app/build.gradle.kts"
        $catalog = Read-TextFile "gradle/libs.versions.toml"
        $requiredAnchors = @(
            [pscustomobject]@{ Path = "app/build.gradle.kts"; Text = $appGradle; Anchor = 'apply(plugin = "com.google.gms.google-services")' },
            [pscustomobject]@{ Path = "app/build.gradle.kts"; Text = $appGradle; Anchor = "implementation(platform(libs.firebase.bom))" },
            [pscustomobject]@{ Path = "app/build.gradle.kts"; Text = $appGradle; Anchor = "implementation(libs.firebase.crashlytics)" },
            [pscustomobject]@{ Path = "gradle/libs.versions.toml"; Text = $catalog; Anchor = 'firebase-crashlytics = { group = "com.google.firebase", name = "firebase-crashlytics" }' },
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
            Add-Pass -Check $check -Message "Firebase Crashlytics and ignored Google Services config match contract"
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

        if (-not (Test-RepositoryIgnoreRule "debug.credentials.properties")) {
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
            "baselineprofile/build.gradle.kts",
            "gradle/libs.versions.toml"
        ) | Where-Object { Test-Path -LiteralPath (Join-RepoPath $_) }

        foreach ($file in $gradleFiles) {
            foreach ($line in Get-CodeLines $file) {
                if ($line.Text -match '^\s*(implementation|api|releaseImplementation|runtimeOnly|releaseRuntimeOnly|compileOnly|releaseCompileOnly)\s*\(.*(sentry|io\.sentry|libs\.sentry)' -or
                    $line.Text -match '\bid\s*(?:\(\s*|=\s*)["'']io\.sentry(?:\.[\w.-]+)?["'']|alias\s*\(\s*libs\.plugins\.sentry\b') {
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
            $problems += "release no-op SentryInit.kt not found"
            if ($null -eq $firstLine) {
                $firstPath = $releaseInit
            }
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
                    if ($alias -notin @("bom", "crashlytics")) {
                        $problems += "unapproved Firebase dependency alias '$alias' found in $file"
                        if ($null -eq $firstLine) {
                            $firstPath = $file
                            $firstLine = $line.Number
                        }
                    }
                }

                if ($file -eq "gradle/libs.versions.toml" -and $line.Text -match '^\s*(firebase[-A-Za-z0-9_.]*)\s*=') {
                    $key = $Matches[1]
                    if ($key -notin @("firebaseBom", "firebase-bom", "firebase-crashlytics")) {
                        $problems += "unapproved Firebase catalog entry '$key' found in $file"
                        if ($null -eq $firstLine) {
                            $firstPath = $file
                            $firstLine = $line.Number
                        }
                    }
                }

                if ($line.Text -match 'com\.google\.firebase' -and $line.Text -notmatch 'firebase-(bom|crashlytics)"|com\.google\.firebase\.crashlytics"|^import com\.google\.firebase\.crashlytics\.buildtools\.gradle\.CrashlyticsExtension$') {
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

    $values = [System.Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
    foreach ($name in $secretNames) {
        $value = [Environment]::GetEnvironmentVariable($name)
        if (-not [string]::IsNullOrWhiteSpace($value)) {
            $values.Add($value) | Out-Null
        }
    }

    $debugCredentialsPath = Join-RepoPath "debug.credentials.properties"
    if (Test-Path -LiteralPath $debugCredentialsPath) {
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
            }
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
        throw "Unable to read checked file: $RelativePath"
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
        throw "Checked binary file is missing: $RelativePath"
    }

    if ([System.IO.Path]::GetExtension($path).ToLowerInvariant() -in @('.apk', '.aab')) {
        Add-Type -AssemblyName System.IO.Compression.FileSystem
        try {
            $archive = [System.IO.Compression.ZipFile]::OpenRead($path)
            try {
                foreach ($entry in $archive.Entries) {
                    $stream = $entry.Open()
                    try {
                        $reader = [System.IO.StreamReader]::new($stream, [System.Text.Encoding]::UTF8, $false)
                        try {
                            $text = $reader.ReadToEnd()
                            foreach ($secret in $Secrets) {
                                if ($secret.Length -gt 0 -and $text.IndexOf($secret, [System.StringComparison]::Ordinal) -ge 0) {
                                    return $true
                                }
                            }
                        } finally {
                            $reader.Dispose()
                        }
                    } finally {
                        $stream.Dispose()
                    }
                }
            } finally {
                $archive.Dispose()
            }
        } catch {
            throw "Unable to read checked archive: $RelativePath"
        }
    }

    $bytes = [System.IO.File]::ReadAllBytes($path)
    $byteEncoding = [System.Text.Encoding]::GetEncoding(28591)
    $haystack = $byteEncoding.GetString($bytes)
    foreach ($secret in $Secrets) {
        $needle = [System.Text.Encoding]::UTF8.GetBytes($secret)
        if ($needle.Length -eq 0 -or $bytes.Length -lt $needle.Length) {
            continue
        }
        if ($haystack.IndexOf($byteEncoding.GetString($needle), [System.StringComparison]::Ordinal) -ge 0) {
            return $true
        }
    }

    return $false
}

function Remove-SecretValuesFromDiagnostic {
    param([string]$Text, [string[]]$Secrets)

    foreach ($secret in ($Secrets | Sort-Object Length -Descending)) {
        $Text = $Text.Replace($secret, "[redacted]")
    }
    return $Text
}

function Test-KnownRavelrySecrets {
    $check = "known-ravelry-secrets"
    $secrets = @()

    try {
        $secrets = @(Get-KnownRavelrySecretValues)
        if ($secrets.Count -eq 0) {
            Add-Pass -Check $check -Message "no known local or environment Ravelry secret values were provided for comparison"
            return
        }

        $textFiles = Get-RelativeFilesForRoots @(
            "app/src",
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
            $firstPath = Remove-SecretValuesFromDiagnostic -Text $matches[0] -Secrets $secrets
            Add-Fail -Check $check -Message "known Ravelry secret value found in $($matches.Count) checked file(s); value redacted" -RelativePath $firstPath
        } else {
            Add-Pass -Check $check -Message "known Ravelry secret values are absent from checked sources, resources, generated constants, tests, APKs, and bundles"
        }
    } catch {
        $message = if ($secrets.Count -eq 0) {
            "Unable to load known Ravelry secret values for comparison"
        } else {
            Remove-SecretValuesFromDiagnostic -Text $_.Exception.Message -Secrets $secrets
        }
        Add-Fail -Check $check -Message $message
    }
}

function Test-MigrationPath {
    param(
        [int]$Start,
        [int]$Target,
        [object[]]$Edges
    )

    $current = $Start
    while ($current -lt $Target) {
        $next = $Edges |
            Where-Object { $_.From -eq $current -and $_.To -gt $current -and $_.To -le $Target } |
            Sort-Object To -Descending |
            Select-Object -First 1
        if ($null -eq $next) { return $false }
        $current = [int]$next.To
    }

    return $current -eq $Target
}

function Get-RegisteredManualMigrationEdges {
    param(
        [string]$SourceRoot,
        [string]$DatabaseText
    )

    $edges = @()
    $problems = @()
    $registrationFound = $false
    $spreadListProcessed = $false

    foreach ($file in Get-RelativeFiles -RelativeDirectory $SourceRoot -Filter "*.kt") {
        $codeText = ((Get-CodeLines $file | ForEach-Object { $_.Text }) -join "`n")
        foreach ($registration in [regex]::Matches($codeText, '(?s)\.addMigrations\s*\((.*?)\)')) {
            $registrationFound = $true
            $arguments = $registration.Groups[1].Value

            foreach ($match in [regex]::Matches($arguments, '\bKnitToolsDatabase\.MIGRATION_(\d+)_(\d+)\b')) {
                $edges += [pscustomobject]@{ From = [int]$match.Groups[1].Value; To = [int]$match.Groups[2].Value }
            }

            if ($arguments -match '\*\s*KnitToolsDatabase\.ALL_MANUAL_MIGRATIONS\b' -and -not $spreadListProcessed) {
                $spreadListProcessed = $true
                # Parseri edellyttää Array<Migration>-tyyppistä arrayOf-listaa;
                # inline Migration(...)-alkiot tai muotoilun muutos voivat katkaista suljehaun.
                $manualMigrations = [regex]::Match(
                    $DatabaseText,
                    '(?s)ALL_MANUAL_MIGRATIONS\s*:\s*Array<Migration>.*?arrayOf\s*\((.*?)\)'
                )
                if (-not $manualMigrations.Success) {
                    $problems += "ALL_MANUAL_MIGRATIONS could not be parsed"
                } else {
                    foreach ($match in [regex]::Matches($manualMigrations.Groups[1].Value, '\bMIGRATION_(\d+)_(\d+)\b')) {
                        $edges += [pscustomobject]@{ From = [int]$match.Groups[1].Value; To = [int]$match.Groups[2].Value }
                    }
                }
            }

            $unparsedArguments = [regex]::Replace(
                $arguments,
                '\bKnitToolsDatabase\.MIGRATION_\d+_\d+\b|\*\s*KnitToolsDatabase\.ALL_MANUAL_MIGRATIONS\b',
                ""
            ) -replace '[\s,]', ''
            if (-not [string]::IsNullOrWhiteSpace($unparsedArguments)) {
                $problems += "unsupported addMigrations arguments in $file"
            }
        }
    }

    if (-not $registrationFound) {
        $problems += "Room migrations are not registered with addMigrations"
    }

    [pscustomobject]@{
        Edges = @($edges | Sort-Object From, To -Unique)
        Problems = @($problems | Sort-Object -Unique)
    }
}

function Test-RoomSchema {
    $check = "room-schema"
    $sourceRoot = "app/src/main/java"
    $firstPath = $sourceRoot
    $firstLine = $null

    try {
        $declarationPattern = '(?m)^[ \t]*@(?:androidx\.room\.)?Database\s*\((?<options>(?:[^()]|(?<nested>\()|(?<-nested>\)))*)' +
            '(?(nested)(?!))\)\s*(?:(?:public|internal)\s+)?abstract\s+class\s+(?<class>[A-Za-z_][A-Za-z0-9_]*)\s*:\s*(?:androidx\.room\.)?RoomDatabase\b'
        $databases = @(
            foreach ($file in Get-RelativeFiles -RelativeDirectory $sourceRoot -Filter "*.kt") {
                $text = [string](Read-TextFile $file) -replace '(?s)/\*.*?\*/', ''
                foreach ($declaration in [regex]::Matches($text, $declarationPattern)) {
                    [pscustomobject]@{
                        Path = $file
                        Text = $text
                        Options = $declaration.Groups["options"].Value
                        Class = $declaration.Groups["class"].Value
                    }
                }
            }
        )

        if ($databases.Count -eq 0) {
            Add-Fail -Check $check -Message "@Database class not found" -RelativePath $sourceRoot
            return
        }

        if ($databases.Count -gt 1) {
            Add-Fail -Check $check -Message "multiple @Database classes found; unable to identify the expected database" -RelativePath $sourceRoot
            return
        }

        $database = $databases[0]
        $databasePath = $database.Path
        $firstPath = $databasePath
        $databaseText = $database.Text
        $versionMatch = [regex]::Match(
            $database.Options,
            '\bversion\s*=\s*([A-Za-z_][A-Za-z0-9_]*|\d+)'
        )
        if (-not $versionMatch.Success) {
            Add-Fail -Check $check -Message "database version could not be parsed" -RelativePath $databasePath -Line (Get-LineNumber -RelativePath $databasePath -Pattern "@Database")
            return
        }
        $versionExpression = $versionMatch.Groups[1].Value
        if ($versionExpression -match '^\d+$') {
            $version = [int]$versionExpression
        } else {
            $escapedVersionName = [regex]::Escape($versionExpression)
            $constantMatch = [regex]::Match(
                $databaseText,
                "(?m)^\s*const\s+val\s+$escapedVersionName\s*=\s*(\d+)\s*$"
            )
            if (-not $constantMatch.Success) {
                Add-Fail -Check $check -Message "database version constant could not be resolved" -RelativePath $databasePath -Line (Get-LineNumber -RelativePath $databasePath -Pattern $versionExpression)
                return
            }
            $version = [int]$constantMatch.Groups[1].Value
        }

        $problems = @()
        if ($database.Options -notmatch '\bexportSchema\s*=\s*true\b') {
            $problems += "exportSchema=true missing"
            $firstLine = Get-LineNumber -RelativePath $databasePath -Pattern "exportSchema"
        }

        $packageName = ""
        if ($databaseText -match '(?m)^\s*package\s+([A-Za-z0-9_.]+)') {
            $packageName = $Matches[1]
        }
        $className = $database.Class
        if ([string]::IsNullOrWhiteSpace($packageName)) {
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
        foreach ($match in [regex]::Matches($database.Options, 'AutoMigration\s*\(\s*from\s*=\s*(\d+)\s*,\s*to\s*=\s*(\d+)')) {
            $edges += [pscustomobject]@{ From = [int]$match.Groups[1].Value; To = [int]$match.Groups[2].Value }
        }

        $manualMigrations = Get-RegisteredManualMigrationEdges -SourceRoot $sourceRoot -DatabaseText $databaseText
        $edges += @($manualMigrations.Edges)
        $problems += @($manualMigrations.Problems)

        $edges = @($edges | Sort-Object From, To -Unique)
        $startVersion = [int]($schemaVersions | Measure-Object -Minimum).Minimum
        if ($startVersion -gt 1) {
            Add-Warn -Check $check -Message "schema history starts at $startVersion; paths checked from every exported schema" -RelativePath $schemaRelativeDir
        }

        foreach ($sourceVersion in $schemaVersions | Where-Object { $_ -lt $version }) {
            if (-not (Test-MigrationPath -Start $sourceVersion -Target $version -Edges $edges)) {
                $problems += "migration path from $sourceVersion to $version is incomplete"
                if ($null -eq $firstLine) {
                    $firstLine = Get-LineNumber -RelativePath $databasePath -Pattern "MIGRATION_|AutoMigration"
                }
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
    $manifestPath = "app/src/main/AndroidManifest.xml"

    try {
        $mainText = Read-TextFile $mainPath
        $requestText = Read-TextFile $requestPath
        $manifestText = Read-TextFile $manifestPath
        $warnings = @()
        $firstPath = $mainPath
        $firstLine = $null

        $anchors = @(
            [pscustomobject]@{ Path = $mainPath; Text = $mainText; Pattern = "CounterLaunchTokenStore.consumeLaunchId"; Message = "token store consumption symbol not found" },
            [pscustomobject]@{ Path = $mainPath; Text = $mainText; Pattern = "CounterLaunchTokenStore.issueLaunchId"; Message = "token issue symbol not found" },
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

        # Ravelry-OAuth poistettiin Androidista 2026-10-09: paluuosoite ei saa palata, koska
        # se ohittaisi laskurin avauksen token-portin ja toisi kirjautumisen takaisin sovellukseen.
        $oauthSurfaces = @(
            [pscustomobject]@{ Path = $mainPath; Text = $mainText; Pattern = "handleOAuthCallbackIfNeeded" },
            [pscustomobject]@{ Path = $mainPath; Text = $mainText; Pattern = "isOAuthCallback" },
            [pscustomobject]@{ Path = $requestPath; Text = $requestText; Pattern = "isOAuthCallback" },
            [pscustomobject]@{ Path = $manifestPath; Text = $manifestText; Pattern = "ravelry-auth-complete" }
        )
        foreach ($surface in $oauthSurfaces) {
            if ($surface.Text.Contains($surface.Pattern)) {
                $line = Get-LineNumber -RelativePath $surface.Path -Pattern ([regex]::Escape($surface.Pattern))
                Add-Fail -Check $check -Message "removed OAuth callback surface '$($surface.Pattern)' is back in $($surface.Path)" -RelativePath $surface.Path -Line $line
                return
            }
        }

        if ($warnings.Count -gt 0) {
            Add-Warn -Check $check -Message (($warnings -join "; ") + "; static drift detector only") -RelativePath $firstPath -Line $firstLine
        } else {
            Add-Pass -Check $check -Message "token gate anchors present and no OAuth callback surface"
        }
    } catch {
        Add-Fail -Check $check -Message ("unable to inspect widget boundary: " + $_.Exception.Message) -RelativePath $mainPath
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
    if ($qualifier -match '^([a-z]{2,3})(?:-r([A-Z]{2}))?(?:-|$)') {
        if ($Matches[1] -eq "car") { return $null }
        if ($Matches.ContainsKey(2)) {
            return "$($Matches[1])-$($Matches[2])"
        }
        return $Matches[1]
    }

    if ($qualifier -match '^b\+([a-z]{2,8}(?:\+[a-z0-9]{1,8})*)(?:-|$)') {
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
