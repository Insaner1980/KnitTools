$ProjectCheckCommand = "osv-scan"
$ProjectRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")).Path
$checkerRoot = if (Test-Path -LiteralPath Env:ANDROID_CHECK_ROOT) { $env:ANDROID_CHECK_ROOT } else { "C:\Dev\Android-check" }
if ([string]::IsNullOrWhiteSpace($checkerRoot)) {
    throw "ANDROID_CHECK_ROOT must point to an Android-check directory."
}
$checkerScript = Join-Path $checkerRoot "tools\InvokeProjectCheck.ps1"
if (-not (Test-Path -LiteralPath $checkerScript -PathType Leaf)) {
    throw "Android-check script not found: $checkerScript (ANDROID_CHECK_ROOT)."
}
& $checkerScript -ProjectCheckCommand $ProjectCheckCommand -Root $ProjectRoot @args
exit $LASTEXITCODE
