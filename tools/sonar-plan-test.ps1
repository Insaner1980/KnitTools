#Requires -Version 5.1
$ErrorActionPreference = 'Stop'
$fixture = Join-Path ([IO.Path]::GetTempPath()) ('knittools-sonar-plan-' + [Guid]::NewGuid().ToString('N'))
$shell = (Get-Process -Id $PID).Path
try {
    New-Item -ItemType Directory -Path $fixture | Out-Null
    New-Item -ItemType Directory -Path (Join-Path $fixture '.git') | Out-Null
    Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'sonar.ps1') -Destination $fixture
    Set-Content (Join-Path $fixture 'sonar-project.properties') 'sonar.projectKey=fixture'
    Set-Content (Join-Path $fixture 'cli-stub.ps1') @'
Set-Content -LiteralPath (Join-Path $PSScriptRoot 'cli-called') -Value ($args -join ' ')
$global:LASTEXITCODE = 7
'@
    Set-Content (Join-Path $fixture 'gradlew.bat') '@echo called>gradle-called'
    Set-Content (Join-Path $fixture 'run.ps1') @'
param([switch]$Plan, [switch]$Allow, [switch]$Cli)
$ErrorActionPreference = 'Stop'
function Get-Command {
    param($Name, $CommandType, $ErrorAction)
    if ($Name -ne 'sonar.exe') { throw "Unexpected executable lookup: $Name" }
    [pscustomobject]@{ Source = (Join-Path $PSScriptRoot 'cli-stub.ps1') }
}
Set-Location -LiteralPath $PSScriptRoot
$parameters = @{ PlanOnly = $Plan; AllowExternalUpload = $Allow }
if ($Cli) { $parameters.SonarArgs = @('status') }
& (Join-Path $PSScriptRoot 'sonar.ps1') @parameters
exit $LASTEXITCODE
'@
    $cases = @(
        @{ Args = @('-Plan'); Exit = 0; Cli = $false; Plan = 'Gradle sonar' },
        @{ Args = @('-Plan', '-Allow'); Exit = 0; Cli = $false; Plan = 'Gradle sonar' },
        @{ Args = @('-Plan', '-Allow', '-Cli'); Exit = 0; Cli = $false; Plan = 'sonar.exe' },
        @{ Args = @('-Plan', '-Cli'); Exit = 0; Cli = $false; Plan = 'sonar.exe' },
        @{ Args = @('-Cli'); Exit = 2; Cli = $false; Plan = $null },
        @{ Args = @('-Allow', '-Cli'); Exit = 7; Cli = $true; Plan = $null }
    )
    foreach ($case in $cases) {
        $arguments = $case.Args
        try {
            $ErrorActionPreference = 'Continue'
            $output = & $shell -NoProfile -File (Join-Path $fixture 'run.ps1') @arguments 2>&1
            $exitCode = $LASTEXITCODE
        } finally { $ErrorActionPreference = 'Stop' }
        if ($exitCode -ne $case.Exit) { throw "Unexpected exit for $arguments : $exitCode; $output" }
        if ((Test-Path (Join-Path $fixture 'cli-called')) -ne $case.Cli) { throw "Unexpected CLI execution for $arguments" }
        if (Test-Path (Join-Path $fixture 'gradle-called')) { throw 'Gradle was invoked' }
        if (Test-Path (Join-Path $fixture 'reports')) { throw 'Plan/CLI dispatch wrote reports' }
        if ($case.Plan -and ($output -join "`n") -notmatch [regex]::Escape($case.Plan)) { throw "Missing plan for $arguments" }
        Write-Output "PASS: $arguments (exit $($case.Exit))"
    }
    if ((Get-Content (Join-Path $fixture 'cli-called') -Raw).Trim() -ne 'status') { throw 'CLI arguments changed' }
    Write-Output 'sonar-plan-test: 6 passed; CLI stub only; no external upload'
}
finally {
    $resolved = [IO.Path]::GetFullPath($fixture)
    if (-not $resolved.StartsWith([IO.Path]::GetFullPath([IO.Path]::GetTempPath()), [StringComparison]::OrdinalIgnoreCase)) {
        throw 'Unsafe fixture cleanup path'
    }
    if (Test-Path -LiteralPath $resolved) { Remove-Item -LiteralPath $resolved -Recurse -Force }
}
