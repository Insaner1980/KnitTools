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
$artifactIdentity = $ast.Find({ param($node)
    $node -is [Management.Automation.Language.FunctionDefinitionAst] -and $node.Name -eq 'Assert-OptimizedArtifactIdentity'
}, $true)
if ($null -eq $artifactIdentity) { throw 'Missing artifact identity check' }
. ([scriptblock]::Create($artifactIdentity.Extent.Text))

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
$apk = Join-Path $output 'fixture.apk'
$mapping = Join-Path $output 'fixture-mapping.txt'
try {
    Add-Type -AssemblyName System.IO.Compression, System.IO.Compression.FileSystem
    $archive = [IO.Compression.ZipFile]::Open($apk, [IO.Compression.ZipArchiveMode]::Create)
    try {
        $writer = [IO.StreamWriter]::new($archive.CreateEntry('classes.dex').Open())
        try {
            $writer.Write('~~R8{"backend":"dex","pg-map-id":"fixture-map-id","version":"9.5.29"}')
        } finally {
            $writer.Dispose()
        }
    } finally {
        $archive.Dispose()
    }
    $appManifest = '    A: package="com.finnvek.knittools.optimizedtest"'
    $driverManifest = @'
    A: package="com.finnvek.knittools.baselineprofile"
      E: instrumentation (line=9)
        A: http://schemas.android.com/apk/res/android:name(0x01010003)="androidx.test.runner.AndroidJUnitRunner"
        A: http://schemas.android.com/apk/res/android:targetPackage(0x01010021)="com.finnvek.knittools.baselineprofile"
'@
    '# pg_map_id: fixture-map-id' | Set-Content -LiteralPath $mapping
    Assert-OptimizedArtifactIdentity $apk $mapping $appManifest $driverManifest

    function Assert-IdentityRejected($appManifest, $driverManifest, $message) {
        try {
            Assert-OptimizedArtifactIdentity $apk $mapping $appManifest $driverManifest
        } catch {
            if ($_.Exception.Message -ceq $message) { return }
            throw
        }
        throw "Expected rejection: $message"
    }
    Assert-IdentityRejected ($appManifest.Replace('.optimizedtest', '')) $driverManifest 'Unexpected packaged target application ID.'
    Assert-IdentityRejected $appManifest ($driverManifest.Replace('.baselineprofile', '.unrelated')) 'Unexpected packaged driver application ID or instrumentation.'
    Assert-IdentityRejected $appManifest ($driverManifest.Replace('AndroidJUnitRunner', 'OtherRunner')) 'Unexpected packaged driver application ID or instrumentation.'
    '# pg_map_id: stale-map-id' | Set-Content -LiteralPath $mapping
    Assert-IdentityRejected $appManifest $driverManifest 'R8 mapping does not match the target APK.'
    Write-Output 'PASS: artifact identity accepts a matching set and rejects the wrong target, driver and mapping'

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
    foreach ($file in @($log, $apk, $mapping)) {
        if (Test-Path -LiteralPath $file) { Remove-Item -LiteralPath $file -Force }
    }
    Remove-Item -LiteralPath $output -Force
}
