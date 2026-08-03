#Requires -Version 5.1

[CmdletBinding()]
param(
    [switch]$PlanOnly,

    [switch]$AllowExternalUpload,

    [ValidateRange(1, 86400)]
    [int]$GradleTimeoutSeconds = 3600,

    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$SonarArgs
)

$ErrorActionPreference = "Continue"
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new()
$OutputEncoding = [Console]::OutputEncoding

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

function Invoke-SonarCli {
    param([string[]]$Arguments)

    $cli = Get-Command sonar.exe -CommandType Application -ErrorAction SilentlyContinue
    if ($null -eq $cli) {
        throw "sonar.exe ei löytynyt PATHista."
    }

    & $cli.Source @Arguments
    exit $(if ($null -ne $global:LASTEXITCODE) { [int]$global:LASTEXITCODE } else { 0 })
}

function Get-SonarProjectProperties {
    param([string]$RepoRoot)

    $path = Join-Path $RepoRoot "sonar-project.properties"
    if (-not (Test-Path -LiteralPath $path)) {
        throw "sonar-project.properties ei löytynyt: $path"
    }

    $properties = @{}
    foreach ($line in Get-Content -LiteralPath $path -Encoding utf8) {
        $trimmed = $line.Trim()
        if ([string]::IsNullOrWhiteSpace($trimmed) -or $trimmed.StartsWith("#")) {
            continue
        }

        $separator = $trimmed.IndexOf("=")
        if ($separator -lt 1) {
            continue
        }

        $key = $trimmed.Substring(0, $separator).Trim()
        $value = $trimmed.Substring($separator + 1).Trim()
        $properties[$key] = $value
    }

    return $properties
}

function Import-SharedCheckerModule {
    param([string]$ModuleName)

    $sharedCheckerRoot =
        if ([string]::IsNullOrWhiteSpace($env:ANDROID_CHECK_ROOT)) {
            "C:\Dev\Android-check"
        } else {
            $env:ANDROID_CHECK_ROOT
        }
    $modulePath = Join-Path (Join-Path $sharedCheckerRoot "tools") "$ModuleName.psm1"
    if (-not (Test-Path -LiteralPath $modulePath -PathType Leaf)) {
        throw "SHARED_CHECKER_MODULE_MISSING: $modulePath"
    }
    Import-Module $modulePath -Force -ErrorAction Stop
}

if ($SonarArgs.Count -gt 0) {
    if (-not $AllowExternalUpload) {
        Write-Error "EXTERNAL_UPLOAD_APPROVAL_REQUIRED: sonar.exe-komennot vaativat -AllowExternalUpload-valitsimen."
        exit 2
    }
    Invoke-SonarCli -Arguments $SonarArgs
}

$repoRoot = Get-RepositoryRoot -Start (Get-Location).Path
$sonarProperties = Get-SonarProjectProperties -RepoRoot $repoRoot
$reportsDir = Join-Path $repoRoot "reports"
$scanReport = Join-Path $reportsDir "sonar.txt"
$issuesReport = Join-Path $reportsDir "sonar-issues.json"
$projectKey = $sonarProperties["sonar.projectKey"]

if ([string]::IsNullOrWhiteSpace($projectKey)) {
    throw "sonar.projectKey puuttuu sonar-project.properties-tiedostosta."
}

if ($PlanOnly) {
    Write-Output @(
        "sonar"
        "  - Gradle sonar"
        "  - voi lähettää lähdekoodia ja analyysimetatietoa ulkoiseen palveluun"
        "  - varsinainen ajo vaatii -AllowExternalUpload"
        "  - project: $projectKey"
    )
    exit 0
}

New-Item -ItemType Directory -Force -Path $reportsDir | Out-Null

Set-Content -LiteralPath $scanReport -Encoding utf8 -Value @(
    "sonar"
    "Root: $repoRoot"
    "Project: $projectKey"
    "Command: reports/sonar.txt :: ./gradlew sonar"
    "Started: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
    ""
)

if (-not $AllowExternalUpload) {
    Add-Content -LiteralPath $scanReport -Encoding utf8 -Value @(
        "ERROR: EXTERNAL_UPLOAD_APPROVAL_REQUIRED"
        "Sonar-analyysi voi lähettää lähdekoodia ja analyysimetatietoa ulkoiseen palveluun."
        "Tarkista PlanOnly-tuloste ja käytä -AllowExternalUpload vain nimenomaisella luvalla."
    )
    Get-Content -LiteralPath $scanReport
    exit 2
}

if (Test-Path -LiteralPath $issuesReport) {
    Remove-Item -LiteralPath $issuesReport -Force
}

Push-Location -LiteralPath $repoRoot
try {
    $env:SONAR_HOST_URL = if ($env:SONAR_HOST_URL) { $env:SONAR_HOST_URL } else { "https://sonarcloud.io" }

    if (-not $env:SONAR_TOKEN) {
        Add-Content -LiteralPath $scanReport -Encoding utf8 -Value @(
            "SONAR_TOKEN ei ole asetettu tälle shellille."
            "SonarQube CLI:n `sonar auth login` riittää issueiden lukemiseen, mutta Gradle-skannaus tarvitsee SONAR_TOKEN-arvon."
            "Luo/aseta analyysitoken ja aja esimerkiksi: `$env:SONAR_TOKEN=`"...`"; sonar"
            ""
        )
        Get-Content -LiteralPath $scanReport
        exit 2
    }

    try {
        Import-SharedCheckerModule -ModuleName "CheckRuntime"
        $scanResult = Invoke-ManagedProcess `
            -Executable (Join-Path $repoRoot "gradlew.bat") `
            -Arguments @("sonar", "--console=plain") `
            -WorkingDirectory $repoRoot `
            -TimeoutSeconds $GradleTimeoutSeconds
        foreach ($streamText in @($scanResult.StandardOutput, $scanResult.StandardError)) {
            if (-not [string]::IsNullOrWhiteSpace($streamText)) {
                Add-Content -LiteralPath $scanReport -Encoding utf8 -Value $streamText
                Write-Output $streamText
            }
        }
    }
    catch {
        Add-Content -LiteralPath $scanReport -Encoding utf8 -Value "ERROR: SONAR_ANALYSIS_PROCESS_ERROR: $($_.Exception.Message)"
        exit 2
    }

    if ($scanResult.TimedOut) {
        Add-Content -LiteralPath $scanReport -Encoding utf8 -Value "ERROR: SONAR_ANALYSIS_TIMEOUT ($GradleTimeoutSeconds s)"
        exit 2
    }
    if ($scanResult.ExitCode -ne 0) {
        Add-Content -LiteralPath $scanReport -Encoding utf8 -Value "ERROR: SONAR_ANALYSIS_FAILED (exit $($scanResult.ExitCode))"
        exit 2
    }

    $cli = Get-Command sonar.exe -CommandType Application -ErrorAction SilentlyContinue
    if ($null -eq $cli) {
        Add-Content -LiteralPath $scanReport -Encoding utf8 -Value "NOT_APPLICABLE: sonar.exe issue export is unavailable."
        exit 0
    }

    try {
        Import-SharedCheckerModule -ModuleName "SonarProjectChecks"
        Invoke-SonarIssueExport `
            -Executable $cli.Source `
            -Arguments @("list", "issues", "--project", $projectKey, "--statuses", "OPEN,CONFIRMED", "--format", "json") `
            -WorkingDirectory $repoRoot `
            -ReportPath $issuesReport | Out-Null
    }
    catch {
        Add-Content -LiteralPath $scanReport -Encoding utf8 -Value "ERROR: $($_.Exception.Message)"
        exit 2
    }

    exit 0
}
finally {
    Pop-Location
}
