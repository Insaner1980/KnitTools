[CmdletBinding()]
param(
    [string]$Root = (Get-Location).Path,
    [switch]$ResolveOnly,
    [switch]$WithDeps
)

$ErrorActionPreference = "Continue"
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new()
$OutputEncoding = [Console]::OutputEncoding
$env:PYTHONUTF8 = "1"
$env:PYTHONIOENCODING = "utf-8"
if ([string]::IsNullOrWhiteSpace($env:NVD_API_KEY)) {
    $userNvdApiKey = [Environment]::GetEnvironmentVariable("NVD_API_KEY", "User")
    if (-not [string]::IsNullOrWhiteSpace($userNvdApiKey)) {
        $env:NVD_API_KEY = $userNvdApiKey
    }
}

$ProjectRootFromScript = Split-Path -Parent $PSScriptRoot
$DependencyCheckTask = if ($env:DEPENDENCY_CHECK_TASK) { $env:DEPENDENCY_CHECK_TASK } else { ":app:dependencyCheckAnalyze" }
$SecurityCheckGradleHome = Join-Path $ProjectRootFromScript ".gradle\security-check-home"
$DependencyCheckDataDir = Join-Path $ProjectRootFromScript ".gradle\dependency-check-data"
$DependencyCheckDbFile = Join-Path $DependencyCheckDataDir "odc.mv.db"
$LegacyReportsGradleHome = Join-Path $ProjectRootFromScript "reports\.gradle-home"

function Get-RepositoryRoot {
    param([string]$Start)

    $dir = (Resolve-Path -LiteralPath $Start).Path
    while (-not [string]::IsNullOrWhiteSpace($dir)) {
        if (Test-Path -LiteralPath (Join-Path $dir ".git")) {
            return $dir
        }

        $parent = Split-Path -Parent $dir
        if ([string]::IsNullOrWhiteSpace($parent) -or $parent -eq $dir) {
            return (Resolve-Path -LiteralPath $Start).Path
        }
        $dir = $parent
    }
}

function Write-ReportHeader {
    param([string]$Path, [string]$Title, [string]$Command)

    Set-Content -LiteralPath $Path -Encoding utf8 -Value @(
        $Title
        "Root: $script:RepoRoot"
        "Command: $Command"
        "Started: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
        ""
    )
}

function Add-CheckResult {
    param(
        [string]$Name,
        [string]$ReportName,
        [int]$ExitCode,
        [string]$SkipReason = ""
    )

    $script:CheckResults.Add([pscustomobject]@{
        Name = $Name
        Report = Join-Path $script:ReportsDir $ReportName
        ExitCode = $ExitCode
        Skipped = -not [string]::IsNullOrWhiteSpace($SkipReason)
    }) | Out-Null
}

function Write-CheckSummary {
    Write-Host ""
    Write-Host "security-check summary"
    Write-Host "Root: $script:RepoRoot"
    foreach ($result in $script:CheckResults) {
        $status = if ($result.Skipped) { "SKIPPED" } elseif ($result.ExitCode -eq 0) { "OK" } else { "FAILED ($($result.ExitCode))" }
        Write-Host ("{0}: {1} -> {2}" -f $result.Name, $status, $result.Report)
    }
}

function Test-EnvFlag {
    param(
        [string]$Value,
        [bool]$Default = $false
    )

    if ([string]::IsNullOrWhiteSpace($Value)) {
        return $Default
    }

    return $Value -in @("1", "true", "yes", "on")
}

function Remove-LegacyReportsGradleHome {
    if (-not (Test-Path -LiteralPath $LegacyReportsGradleHome)) {
        return
    }

    $reportsPath = (Resolve-Path -LiteralPath $script:ReportsDir).Path
    $legacyPath = (Resolve-Path -LiteralPath $LegacyReportsGradleHome).Path
    if (-not $legacyPath.StartsWith($reportsPath, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to remove unexpected path: $legacyPath"
    }

    Remove-Item -LiteralPath $legacyPath -Recurse -Force
}

function Invoke-Semgrep {
    $reportPath = Join-Path $script:ReportsDir "security-code.txt"
    $jsonPath = Join-Path $script:ReportsDir "security-code.json"
    $configPath = Join-Path $script:RepoRoot "config\semgrep\knittools-security.yml"
    $commandText = "reports/security-code.txt :: semgrep scan --config config/semgrep/knittools-security.yml --metrics=off --json"
    Write-ReportHeader -Path $reportPath -Title "semgrep" -Command $commandText

    $semgrep = Get-Command semgrep -ErrorAction SilentlyContinue
    if ($null -eq $semgrep) {
        Add-Content -LiteralPath $reportPath -Encoding utf8 -Value "SKIPPED: semgrep was not found on PATH."
        Add-CheckResult -Name "semgrep" -ReportName "security-code.txt" -ExitCode 0 -SkipReason "semgrep was not found on PATH."
        return 0
    }

    if (-not (Test-Path -LiteralPath $configPath)) {
        Add-Content -LiteralPath $reportPath -Encoding utf8 -Value "Semgrep-konfigia ei löytynyt: $configPath"
        Add-CheckResult -Name "semgrep" -ReportName "security-code.txt" -ExitCode 1
        return 1
    }

    Push-Location -LiteralPath $script:RepoRoot
    try {
        $semgrepArgs = @(
            "scan",
            "--metrics=off",
            "--quiet",
            "--json",
            "--config", $configPath,
            "--exclude", "app/build",
            "--exclude", "baselineprofile/build",
            "--exclude", "build",
            "--exclude", "reports",
            "--disable-version-check",
            "--output", $jsonPath,
            "."
        )
        & $semgrep.Source @semgrepArgs 2>&1 | Tee-Object -FilePath $reportPath -Append | Out-Host
        $code = if ($null -ne $global:LASTEXITCODE) { [int]$global:LASTEXITCODE } else { 0 }
    }
    catch {
        Add-Content -LiteralPath $reportPath -Encoding utf8 -Value $_.Exception.Message
        $code = 1
    }
    finally {
        Pop-Location
    }

    if ($code -eq 1 -and (Test-Path -LiteralPath $jsonPath)) {
        $code = 0
    }

    if ($code -ne 0 -or -not (Test-Path -LiteralPath $jsonPath)) {
        Add-Content -LiteralPath $reportPath -Encoding utf8 -Value "Semgrep-skannaus epäonnistui."
        Add-CheckResult -Name "semgrep" -ReportName "security-code.txt" -ExitCode $code
        return $code
    }

    try {
        $data = Get-Content -Raw -Encoding utf8 -LiteralPath $jsonPath | ConvertFrom-Json
        $results = @($data.results)
        $lines = @("== Semgrep security scan ==")
        if ($results.Count -eq 0) {
            $lines += "Ei löydöksiä."
        }
        else {
            $lines += "Löydöksiä: $($results.Count)"
            foreach ($item in $results) {
                $path = if ($item.path) { $item.path } else { "?" }
                $line = if ($item.start.line) { $item.start.line } else { "?" }
                $checkId = if ($item.check_id) { $item.check_id } else { "?" }
                $message = if ($item.extra.message) { $item.extra.message.Trim() } else { "" }
                $lines += "- ${path}:${line} [$checkId] $message"
            }
        }

        Set-Content -LiteralPath $reportPath -Encoding utf8 -Value $lines
        $lines | ForEach-Object { Write-Host $_ }
        Add-CheckResult -Name "semgrep" -ReportName "security-code.txt" -ExitCode 0
        return 0
    }
    catch {
        Add-Content -LiteralPath $reportPath -Encoding utf8 -Value $_.Exception.Message
        Add-CheckResult -Name "semgrep" -ReportName "security-code.txt" -ExitCode 1
        return 1
    }
}

function Invoke-DependencyAudit {
    $reportPath = Join-Path $script:ReportsDir "security-deps.txt"
    $rawReportPath = Join-Path $script:ReportsDir "security-deps-raw.txt"
    $jsonReportPath = Join-Path $script:ReportsDir "dependency-check-report.json"
    $gradle = Join-Path $script:RepoRoot "gradlew.bat"
    $enabled = $WithDeps -or (Test-EnvFlag -Value $env:DEPENDENCY_CHECK_ENABLED -Default $true)
    $dependencyCheckDbExists = Test-Path -LiteralPath $DependencyCheckDbFile
    $autoUpdate = Test-EnvFlag -Value $env:DEPENDENCY_CHECK_AUTO_UPDATE -Default (-not $dependencyCheckDbExists)
    $requireNvdApiKey = Test-EnvFlag -Value $env:DEPENDENCY_CHECK_REQUIRE_NVD_API_KEY
    Write-ReportHeader -Path $reportPath -Title "dependency audit" -Command "reports/security-deps.txt :: OWASP dependency-check"
    Set-Content -LiteralPath $rawReportPath -Encoding utf8 -Value @()

    if (-not $enabled) {
        Add-Content -LiteralPath $reportPath -Encoding utf8 -Value "SKIPPED: dependency-check ohitetaan, koska DEPENDENCY_CHECK_ENABLED ei ole true."
        Add-CheckResult -Name "dependency audit" -ReportName "security-deps.txt" -ExitCode 0 -SkipReason "dependency-check ohitettu ympäristömuuttujalla."
        return 0
    }

    if (-not (Test-Path -LiteralPath $gradle)) {
        Add-Content -LiteralPath $reportPath -Encoding utf8 -Value "Gradle wrapperia ei löytynyt: $gradle"
        Add-CheckResult -Name "dependency audit" -ReportName "security-deps.txt" -ExitCode 1
        return 1
    }

    if ($requireNvdApiKey -and [string]::IsNullOrWhiteSpace($env:NVD_API_KEY)) {
        Add-Content -LiteralPath $reportPath -Encoding utf8 -Value "SKIPPED: NVD_API_KEY puuttuu ja hidas täyspäivitys on estetty oletuksena."
        Add-Content -LiteralPath $reportPath -Encoding utf8 -Value "Aja tarvittaessa ensin: `$env:NVD_API_KEY=`"...`"; `$env:DEPENDENCY_CHECK_AUTO_UPDATE=`"true`"; `$env:DEPENDENCY_CHECK_ENABLED=`"true`"; sc"
        Add-CheckResult -Name "dependency audit" -ReportName "security-deps.txt" -ExitCode 0 -SkipReason "NVD_API_KEY puuttuu."
        return 0
    }

    if (-not $autoUpdate -and -not (Test-Path -LiteralPath $DependencyCheckDbFile)) {
        Add-Content -LiteralPath $reportPath -Encoding utf8 -Value "FAILED: dependency-checkin paikallinen CVE-tietokanta puuttuu."
        Add-Content -LiteralPath $reportPath -Encoding utf8 -Value "Alusta se tarvittaessa: `$env:DEPENDENCY_CHECK_AUTO_UPDATE=`"true`"; `$env:DEPENDENCY_CHECK_ENABLED=`"true`"; sc"
        Add-CheckResult -Name "dependency audit" -ReportName "security-deps.txt" -ExitCode 1
        return 1
    }

    Push-Location -LiteralPath $script:RepoRoot
    try {
        Remove-LegacyReportsGradleHome
        $env:GRADLE_USER_HOME = $SecurityCheckGradleHome
        $env:DEPENDENCY_CHECK_DATA_DIRECTORY = $DependencyCheckDataDir
        $env:DEPENDENCY_CHECK_AUTO_UPDATE = if ($autoUpdate) { "true" } else { "false" }
        & $gradle "--no-daemon" $DependencyCheckTask "--no-configuration-cache" "--console=plain" *>&1 |
            Tee-Object -FilePath $rawReportPath |
            Out-Host
        $code = if ($null -ne $global:LASTEXITCODE) { [int]$global:LASTEXITCODE } else { 0 }
    }
    catch {
        Add-Content -LiteralPath $rawReportPath -Encoding utf8 -Value $_.Exception.Message
        $code = 1
    }
    finally {
        Pop-Location
    }

    if ($code -ne 0) {
        Add-Content -LiteralPath $reportPath -Encoding utf8 -Value "Dependency-check palautti virhekoodin $code. Raakaloki: $rawReportPath"
    }

    if (-not (Test-Path -LiteralPath $jsonReportPath)) {
        Add-Content -LiteralPath $reportPath -Encoding utf8 -Value "Yhteenvetoa ei voitu muodostaa: dependency-check-report.json puuttuu."
        Add-Content -LiteralPath $reportPath -Encoding utf8 -Value "Raakaloki: $rawReportPath"
        Add-CheckResult -Name "dependency audit" -ReportName "security-deps.txt" -ExitCode $code
        return $code
    }

    try {
        $data = Get-Content -Raw -Encoding utf8 -LiteralPath $jsonReportPath | ConvertFrom-Json
        $packageVulns = [ordered]@{}
        foreach ($dependency in @($data.dependencies)) {
            $vulns = @($dependency.vulnerabilities)
            if ($vulns.Count -eq 0) {
                continue
            }

            $vulnIds = @($vulns | ForEach-Object { if ($_.name) { $_.name } else { "?" } } | Sort-Object -Unique)
            $packages = @($dependency.packages | ForEach-Object { $_.id } | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
            if ($packages.Count -eq 0) {
                $packages = @($(if ($dependency.fileName) { $dependency.fileName } else { "?" }))
            }

            foreach ($packageId in $packages) {
                if (-not $packageVulns.Contains($packageId)) {
                    $packageVulns[$packageId] = [System.Collections.Generic.HashSet[string]]::new()
                }
                foreach ($vulnId in $vulnIds) {
                    [void]$packageVulns[$packageId].Add($vulnId)
                }
            }
        }

        $lines = @()
        if ($packageVulns.Count -eq 0) {
            $lines += "Ei löydöksiä."
        }
        else {
            $totalVulns = 0
            foreach ($set in $packageVulns.Values) {
                $totalVulns += $set.Count
            }
            $lines += "Löydöksiä: $($packageVulns.Count) pakettia, $totalVulns yksilöllistä CVE-viittausta"
            foreach ($packageId in ($packageVulns.Keys | Sort-Object)) {
                $cves = @($packageVulns[$packageId] | Sort-Object) -join ", "
                $lines += "- ${packageId}: $cves"
            }
        }
        $lines += "Raakaloki: $rawReportPath"

        Add-Content -LiteralPath $reportPath -Encoding utf8 -Value $lines
        $lines | ForEach-Object { Write-Host $_ }
        Add-CheckResult -Name "dependency audit" -ReportName "security-deps.txt" -ExitCode $code
        return $code
    }
    catch {
        Add-Content -LiteralPath $reportPath -Encoding utf8 -Value "Raportin parsinta epäonnistui: $($_.Exception.Message)"
        Add-CheckResult -Name "dependency audit" -ReportName "security-deps.txt" -ExitCode 1
        return 1
    }
}

$script:RepoRoot = Get-RepositoryRoot -Start $Root
if ($ResolveOnly) {
    Write-Output $script:RepoRoot
    exit 0
}

$repoLocalBashScript = Join-Path $script:RepoRoot "scripts\security-check.sh"
$bash = Get-Command bash -ErrorAction SilentlyContinue
$canUseRepoLocalBash =
    $null -ne $bash -and
    (Test-Path -LiteralPath $repoLocalBashScript) -and
    $bash.Source -notlike "*\WindowsApps\bash.exe" -and
    -not (Test-EnvFlag -Value $env:KNITTOOLS_SECURITY_CHECK_POWERSHELL_FALLBACK)
if ($canUseRepoLocalBash) {
    $bashArgs = @($repoLocalBashScript)
    if ($WithDeps) {
        $bashArgs += "--with-deps"
    }

    & $bash.Source @bashArgs
    exit $(if ($null -ne $global:LASTEXITCODE) { [int]$global:LASTEXITCODE } else { 0 })
}

$script:ReportsDir = Join-Path $script:RepoRoot "reports"
New-Item -ItemType Directory -Force -Path $script:ReportsDir | Out-Null

$script:CheckResults = [System.Collections.Generic.List[object]]::new()
$exitCode = 0

$code = Invoke-Semgrep
if ($code -ne 0) {
    $exitCode = $code
}

$code = Invoke-DependencyAudit
if ($code -ne 0) {
    $exitCode = $code
}

Write-CheckSummary
exit $exitCode
