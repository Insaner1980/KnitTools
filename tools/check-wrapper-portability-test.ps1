#Requires -Version 5.1
$ErrorActionPreference = 'Stop'
$fixture = Join-Path ([IO.Path]::GetTempPath()) ('knittools-wrapper-' + [Guid]::NewGuid().ToString('N'))
$shell = (Get-Process -Id $PID).Path
$wrappers = @('ac', 'ad', 'bc', 'cr', 'cs', 'db', 'dc', 'ds', 'ga', 'lc', 'ms', 'os', 'pc', 'ql', 'sc', 'sentry', 'ss', 'tc')
try {
    $project = Join-Path $fixture 'project with spaces'
    New-Item -ItemType Directory -Path (Join-Path $project 'tools') -Force | Out-Null
    foreach ($name in $wrappers) {
        $source = Get-Content -LiteralPath (Join-Path $PSScriptRoot "$name.ps1") -Raw
        $literal = '"C:\Dev\Android-check"'
        if (($source.Split(@($literal), [StringSplitOptions]::None)).Count -ne 2) { throw "Unexpected default root in $name" }
        # Rebase only the default literal in the isolated copy; never touch the installed toolkit.
        $source.Replace($literal, ('"' + (Join-Path $fixture 'Dev\Android-check') + '"')) |
            Set-Content -LiteralPath (Join-Path $project "tools\$name.ps1")
    }
    $roots = @('Dev\Android-check', 'custom', 'custom with spaces')
    foreach ($root in $roots) {
        $directory = Join-Path $fixture "$root\tools"
        New-Item -ItemType Directory -Path $directory -Force | Out-Null
        foreach ($target in @('InvokeProjectCheck.ps1', 'InstallDebugToDevice.ps1')) {
            Set-Content -LiteralPath (Join-Path $directory $target) -Value @'
$record = @{ Arguments = @($args | ForEach-Object { "$_" }); Tokens = $env:PMD_CPD_MINIMUM_TOKENS } | ConvertTo-Json -Depth 4
[IO.File]::WriteAllText($env:WRAPPER_TEST_RESULT, $record)
$global:LASTEXITCODE = [int]$env:WRAPPER_TEST_EXIT
'@
        }
    }
    Set-Content -LiteralPath (Join-Path $fixture 'run.ps1') -Value @'
param($Fixture, $Name, $Case, $Result, [int]$Code)
$ErrorActionPreference = 'Stop'
$wrapper = Join-Path $Fixture "project with spaces\tools\$Name.ps1"
$env:WRAPPER_TEST_RESULT = $Result
$env:WRAPPER_TEST_EXIT = "$Code"
Remove-Item Env:ANDROID_CHECK_ROOT -ErrorAction SilentlyContinue
Remove-Item Env:PMD_CPD_MINIMUM_TOKENS -ErrorAction SilentlyContinue
switch ($Case) {
    'custom' { $env:ANDROID_CHECK_ROOT = (Join-Path $Fixture 'custom') }
    'spaces' { $env:ANDROID_CHECK_ROOT = (Join-Path $Fixture 'custom with spaces') }
    'invalid' { $env:ANDROID_CHECK_ROOT = (Join-Path $Fixture 'missing') }
    'whitespace' { $env:ANDROID_CHECK_ROOT = '   ' }
    'tokens' { $env:ANDROID_CHECK_ROOT = (Join-Path $Fixture 'custom'); $env:PMD_CPD_MINIMUM_TOKENS = '73' }
}
$arguments = @('-PlanOnly', '-Label', 'value with spaces', '-AllowExternalUpload')
if ($Name -eq 'ad') {
    & $wrapper -ResolveOnly -NoBuild -ApkPath 'app path\sample.apk' -AdbArgs @('-s', 'serial with spaces')
} else {
    & $wrapper @arguments
}
exit $LASTEXITCODE
'@
    $count = 0
    foreach ($name in $wrappers) {
        foreach ($case in @('absent', 'custom', 'spaces', 'invalid', 'whitespace', 'tokens')) {
            foreach ($code in @(0, 7)) {
                $result = Join-Path $fixture "result-$name-$case-$code.json"
                try {
                    $ErrorActionPreference = 'Continue'
                    $output = & $shell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $fixture 'run.ps1') $fixture $name $case $result $code 2>&1
                    $actualExit = $LASTEXITCODE
                } finally { $ErrorActionPreference = 'Stop' }
                if ($case -in @('invalid', 'whitespace')) {
                    if ($actualExit -eq 0 -or (Test-Path -LiteralPath $result) -or "$output" -notmatch 'ANDROID_CHECK_ROOT') {
                        throw "$name/$case must fail clearly without invoking a target: $output"
                    }
                } else {
                    if ($actualExit -ne $code) { throw "$name/$case exit $actualExit instead of $code : $output" }
                    $record = Get-Content -LiteralPath $result -Raw | ConvertFrom-Json
                    $source = Get-Content -LiteralPath (Join-Path $project "tools\$name.ps1") -Raw
                    if ($name -eq 'ad') {
                        $expected = @('-ProjectRoot', $project, '-ResolveOnly:', 'True', '-NoBuild:', 'True', '-ApkPath', 'app path\sample.apk', '-s', 'serial with spaces')
                    } else {
                        $command = [regex]::Match($source, '\$ProjectCheckCommand = "([^"]+)"').Groups[1].Value
                        $expected = @('-ProjectCheckCommand', $command)
                        if ($name -in @('sc', 'os')) { $expected += @('-Root', $project) }
                        $expected += @('-PlanOnly', '-Label', 'value with spaces', '-AllowExternalUpload')
                    }
                    if (($record.Arguments -join '|') -ne ($expected -join '|')) { throw "$name/$case arguments: $($record.Arguments -join '|'); expected: $($expected -join '|')" }
                    $expectedTokens = if ($case -eq 'tokens') { '73' } elseif ($name -eq 'pc') { '100' } else { $null }
                    if ($record.Tokens -ne $expectedTokens) { throw "$name/$case changed token defaults" }
                }
                $count++
                Write-Output "PASS $name/$case target-exit=$code actual-exit=$actualExit"
            }
        }
    }
    Write-Output "check-wrapper-portability-test: $count passed; isolated targets only"
} finally {
    $resolved = [IO.Path]::GetFullPath($fixture)
    $temporary = [IO.Path]::GetFullPath([IO.Path]::GetTempPath()).TrimEnd('\') + '\'
    if (-not $resolved.StartsWith($temporary, [StringComparison]::OrdinalIgnoreCase)) { throw 'Unsafe fixture cleanup path' }
    if (Test-Path -LiteralPath $resolved) { Remove-Item -LiteralPath $resolved -Recurse -Force }
}
