#Requires -Version 5.1

$ErrorActionPreference = "Stop"

$tempRoot = Join-Path ([IO.Path]::GetTempPath()) ("sonar-timeout-test-" + [Guid]::NewGuid().ToString("N"))
$originalSonarToken = [Environment]::GetEnvironmentVariable("SONAR_TOKEN", "Process")
$originalChildPidPath = [Environment]::GetEnvironmentVariable("SONAR_TEST_CHILD_PID", "Process")
$originalPath = [Environment]::GetEnvironmentVariable("PATH", "Process")
$childPid = $null

function Invoke-SonarFixture {
    param(
        [string]$PowerShellPath,
        [string]$FixtureRoot,
        [string[]]$Arguments
    )

    $previousErrorActionPreference = $ErrorActionPreference
    Push-Location -LiteralPath $FixtureRoot
    try {
        $ErrorActionPreference = "Continue"
        $output = & $PowerShellPath -NoProfile -File (Join-Path $FixtureRoot "tools\sonar.ps1") @Arguments *>&1
        $exitCode = [int]$global:LASTEXITCODE
    }
    finally {
        $ErrorActionPreference = $previousErrorActionPreference
        Pop-Location
    }
    return [pscustomobject]@{
        Output = @($output)
        ExitCode = $exitCode
    }
}

try {
    New-Item -ItemType Directory -Force -Path (Join-Path $tempRoot ".git") | Out-Null
    New-Item -ItemType Directory -Force -Path (Join-Path $tempRoot "tools") | Out-Null
    Copy-Item -LiteralPath (Join-Path $PSScriptRoot "sonar.ps1") -Destination (Join-Path $tempRoot "tools\sonar.ps1")
    Set-Content -LiteralPath (Join-Path $tempRoot "sonar-project.properties") -Encoding utf8 -Value @(
        "sonar.projectKey=fixture"
        "sonar.host.url=https://example.invalid"
    )

    $childPidPath = Join-Path $tempRoot "child.pid"
    Set-Content -LiteralPath (Join-Path $tempRoot "gradlew.bat") -Encoding ascii -Value @(
        "@echo off"
        "echo fake-gradle-stdout"
        "echo fake-gradle-stderr 1>&2"
        "`"%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe`" -NoProfile -Command `"`$PID | Set-Content -LiteralPath `$env:SONAR_TEST_CHILD_PID -Encoding ascii; Start-Sleep -Seconds 30`""
    )

    $pwsh = (Get-Process -Id $PID -ErrorAction Stop).Path
    [Environment]::SetEnvironmentVariable("SONAR_TOKEN", "fixture-token", "Process")
    [Environment]::SetEnvironmentVariable("SONAR_TEST_CHILD_PID", $childPidPath, "Process")
    [Environment]::SetEnvironmentVariable("PATH", (Join-Path $env:SystemRoot "System32"), "Process")

    $startedAt = [DateTimeOffset]::Now
    $timeoutResult =
        Invoke-SonarFixture `
            -PowerShellPath $pwsh `
            -FixtureRoot $tempRoot `
            -Arguments @("-AllowExternalUpload", "-GradleTimeoutSeconds", "2")
    $elapsedSeconds = ([DateTimeOffset]::Now - $startedAt).TotalSeconds

    if ($timeoutResult.ExitCode -ne 2) {
        throw "Expected exit 2 for a Gradle timeout, got $($timeoutResult.ExitCode). Output: $($timeoutResult.Output -join [Environment]::NewLine)"
    }
    if ($elapsedSeconds -ge 15) {
        throw "Timeout was not bounded: elapsed $([Math]::Round($elapsedSeconds, 2)) seconds."
    }

    $reportPath = Join-Path $tempRoot "reports\sonar.txt"
    if (-not (Test-Path -LiteralPath $reportPath -PathType Leaf)) {
        throw "Sonar report was not created."
    }
    $report = Get-Content -LiteralPath $reportPath -Raw
    foreach ($expected in @("fake-gradle-stdout", "fake-gradle-stderr", "ERROR: SONAR_ANALYSIS_TIMEOUT (2 s)")) {
        if (-not $report.Contains($expected)) {
            throw "Sonar report is missing '$expected'."
        }
    }

    if (-not (Test-Path -LiteralPath $childPidPath -PathType Leaf)) {
        throw "The fake Gradle child process did not publish its PID."
    }
    $childPid = [int](Get-Content -LiteralPath $childPidPath -Raw)
    Start-Sleep -Milliseconds 250
    if ($null -ne (Get-Process -Id $childPid -ErrorAction SilentlyContinue)) {
        throw "Timed-out Gradle child process $childPid is still running."
    }

    # Ilman ulkoisen latauksen lupaa Gradlea ei saa käynnistää.
    Remove-Item -LiteralPath $childPidPath -Force
    Remove-Item -LiteralPath (Join-Path $tempRoot "reports") -Recurse -Force
    $gateResult = Invoke-SonarFixture -PowerShellPath $pwsh -FixtureRoot $tempRoot -Arguments @()
    if ($gateResult.ExitCode -ne 2) {
        throw "Expected exit 2 without -AllowExternalUpload, got $($gateResult.ExitCode)."
    }
    $gateReport = Get-Content -LiteralPath $reportPath -Raw
    if (-not $gateReport.Contains("ERROR: EXTERNAL_UPLOAD_APPROVAL_REQUIRED")) {
        throw "Approval-gate report is missing the EXTERNAL_UPLOAD_APPROVAL_REQUIRED marker."
    }
    if (Test-Path -LiteralPath $childPidPath) {
        throw "Gradle ran without external-upload approval."
    }

    # PlanOnly tulostaa suunnitelman mutta ei käynnistä Gradlea.
    $planResult = Invoke-SonarFixture -PowerShellPath $pwsh -FixtureRoot $tempRoot -Arguments @("-PlanOnly")
    if ($planResult.ExitCode -ne 0) {
        throw "Expected exit 0 for -PlanOnly, got $($planResult.ExitCode)."
    }
    if (-not (($planResult.Output -join "`n").Contains("- Gradle sonar"))) {
        throw "PlanOnly output is missing the Gradle Sonar plan."
    }
    if (Test-Path -LiteralPath $childPidPath) {
        throw "Gradle ran during -PlanOnly."
    }

    # Myös suorien sonar.exe-argumenttien lupaportti käyttää exit-koodia 2.
    $cliGateResult =
        Invoke-SonarFixture `
            -PowerShellPath $pwsh `
            -FixtureRoot $tempRoot `
            -Arguments @("-SonarArgs", "status")
    if ($cliGateResult.ExitCode -ne 2) {
        throw "Expected exit 2 for sonar.exe arguments without approval, got $($cliGateResult.ExitCode)."
    }

    Write-Output "sonar-timeout-test: PASS"
}
finally {
    if ($null -ne $childPid) {
        Stop-Process -Id $childPid -Force -ErrorAction SilentlyContinue
    }
    [Environment]::SetEnvironmentVariable("SONAR_TOKEN", $originalSonarToken, "Process")
    [Environment]::SetEnvironmentVariable("SONAR_TEST_CHILD_PID", $originalChildPidPath, "Process")
    [Environment]::SetEnvironmentVariable("PATH", $originalPath, "Process")
    if (Test-Path -LiteralPath $tempRoot) {
        Remove-Item -LiteralPath $tempRoot -Recurse -Force
    }
}
