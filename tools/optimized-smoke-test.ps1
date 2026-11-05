#Requires -Version 5.1
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$tokens = $null
$parseErrors = $null
$ast = [Management.Automation.Language.Parser]::ParseFile(
    (Join-Path $PSScriptRoot 'optimized-smoke.ps1'), [ref]$tokens, [ref]$parseErrors)
if ($parseErrors.Count) { throw 'Source parse failed' }
$native = $ast.Find({ param($node)
    $node -is [Management.Automation.Language.FunctionDefinitionAst] -and $node.Name -eq 'Invoke-NativeCommand'
}, $true)
if ($null -eq $native) { throw 'Missing native-command helper' }
. ([scriptblock]::Create($native.Extent.Text))

$shell = (Get-Process -Id $PID).Path
foreach ($code in @(0, 7)) {
    $result = Invoke-NativeCommand $shell @('-NoProfile', '-Command',
        "[Console]::Out.WriteLine('synthetic stdout'); [Console]::Error.WriteLine('synthetic stderr'); exit $code")
    if ($result.ExitCode -ne $code -or ($result.Output -join "`n") -notmatch 'synthetic stderr' -or
        ($result.Output -join "`n") -notmatch 'synthetic stdout') {
        throw 'Native output or exit code was lost'
    }
    if ($ErrorActionPreference -ne 'Stop') { throw 'Error preference was not restored' }
}
Write-Output 'PASS: native stderr preserves successful and failing exit codes'

$cleanup = @($ast.EndBlock.Statements | Where-Object {
    $_ -is [Management.Automation.Language.TryStatementAst] -and $null -ne $_.Finally
})
if ($cleanup.Count -ne 1) { throw 'Unexpected smoke cleanup structure' }
$body = $cleanup[0].Finally.Extent.Text
$cleanupBlock = [scriptblock]::Create($body.Substring(1, $body.Length - 2))
$calls = [Collections.Generic.List[string]]::new()
$failSize = $false
$Serial = 'emulator-5580'

# Exercise cleanup without invoking adb or changing any device.
function Invoke-NativeCommand([string]$FilePath, [string[]]$Arguments) {
    $calls.Add($Arguments -join ' ')
    return [pscustomobject]@{ Output = @('synthetic evidence'); ExitCode = $(if ($Arguments -contains 'pull') { 7 } else { 0 }) }
}
function Invoke-Adb([string[]]$Arguments) {
    $calls.Add($Arguments -join ' ')
    if ($failSize -and $Arguments -contains 'size') { throw 'Synthetic size reset failure' }
}

$output = Join-Path (Split-Path $PSScriptRoot -Parent) 'reports/optimized-smoke-selftest'
New-Item -ItemType Directory -Path $output -ErrorAction Stop | Out-Null
try {
    . $cleanupBlock
    if (-not $calls.Contains('shell wm size reset') -or -not $calls.Contains('shell wm density reset')) {
        throw 'Failed evidence pull prevented display restoration'
    }
    $calls.Clear()
    $failSize = $true
    $failed = $false
    try { . $cleanupBlock } catch { $failed = $true }
    if (-not $failed -or -not $calls.Contains('shell wm density reset')) {
        throw 'Failed size reset prevented density restoration'
    }
    Write-Output 'PASS: failed pull and size reset cannot skip subsequent restoration'
} finally {
    $log = Join-Path $output 'logcat.txt'
    if (Test-Path -LiteralPath $log) { Remove-Item -LiteralPath $log -Force }
    Remove-Item -LiteralPath $output -Force
}
