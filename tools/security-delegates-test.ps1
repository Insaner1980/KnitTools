param(
    [string]$Bash = 'C:\Program Files\Git\bin\bash.exe'
)

$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$tempRoot = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
$fixture = Join-Path $tempRoot ('knittools-delegates-' + [guid]::NewGuid())
$utf8 = New-Object Text.UTF8Encoding($false)
$savedOutput = $env:DELEGATE_TEST_OUTPUT
$savedExit = $env:DELEGATE_TEST_EXIT
try {
    $caller = Join-Path $fixture 'caller'
    $plain = Join-Path $fixture 'project'
    $spaced = Join-Path $fixture 'project with spaces'
    foreach ($project in @($caller, $plain, $spaced)) {
        New-Item -ItemType Directory -Path (Join-Path $project 'scripts') -Force | Out-Null
        foreach ($name in @('security-check', 'security-check-full')) {
            $stub = @'
#!/usr/bin/env bash
set -euo pipefail
printf '%s\n' "${0##*/}" "$(cd -- "${0%/*}/.." && pwd -W)" "$(pwd -W)" "$#" "$@" > "$DELEGATE_TEST_OUTPUT"
exit "$DELEGATE_TEST_EXIT"
'@
            [IO.File]::WriteAllText((Join-Path $project "scripts/$name.sh"), $stub.Replace("`r`n", "`n"), $utf8)
        }
    }
    $cases = @(
        @{ Name = 'no arguments'; Project = $caller; Args = @(); Forwarded = @() },
        @{ Name = 'explicit project'; Project = $plain; Args = @($plain); Forwarded = @() },
        @{ Name = 'project with spaces'; Project = $spaced; Args = @($spaced); Forwarded = @() },
        @{ Name = 'options first'; Project = $caller; Args = @('--without-deps', '--plan-only'); Forwarded = @('--without-deps', '--plan-only') },
        @{ Name = 'project and options'; Project = $spaced; Args = @($spaced, '--plan-only', 'argument with spaces'); Forwarded = @('--plan-only', 'argument with spaces') }
    )
    $env:DELEGATE_TEST_OUTPUT = (Join-Path $fixture 'result.txt').Replace('\', '/')
    Push-Location $caller
    try {
        foreach ($name in @('security-check', 'security-check-full')) {
            foreach ($case in $cases) {
                foreach ($exitCode in @(0, 37)) {
                    $env:DELEGATE_TEST_EXIT = "$exitCode"
                    $arguments = @($case.Args | ForEach-Object { $_.Replace('\', '/') })
                    & $Bash -c 'export PATH=/usr/bin:/bin:$PATH; exec bash "$@"' delegate-test (Join-Path $repo "scripts/$name-global.sh").Replace('\', '/') @arguments
                    if ($LASTEXITCODE -ne $exitCode) { throw "$name / $($case.Name): exit $LASTEXITCODE, expected $exitCode" }
                    $actual = [IO.File]::ReadAllLines($env:DELEGATE_TEST_OUTPUT)
                    $expected = @("$name.sh", $case.Project.Replace('\', '/'), $caller.Replace('\', '/'), "$($case.Forwarded.Count)") + $case.Forwarded
                    if (($actual -join "`n") -cne ($expected -join "`n")) { throw "$name / $($case.Name): actual=[$($actual -join '|')] expected=[$($expected -join '|')]" }
                    Write-Output "PASS $name / $($case.Name) / exit $exitCode"
                }
            }
        }
    } finally { Pop-Location }
} finally {
    $env:DELEGATE_TEST_OUTPUT = $savedOutput
    $env:DELEGATE_TEST_EXIT = $savedExit
    $resolved = [IO.Path]::GetFullPath($fixture)
    if (!$resolved.StartsWith($tempRoot, [StringComparison]::OrdinalIgnoreCase) -or
        (Split-Path $resolved -Leaf) -notlike 'knittools-delegates-*') { throw 'Unsafe fixture cleanup path' }
    if (Test-Path -LiteralPath $resolved) { Remove-Item -LiteralPath $resolved -Recurse -Force }
}
