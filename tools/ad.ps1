param(
    [switch]$ResolveOnly,
    [switch]$NoBuild,
    [string]$ApkPath = "app\build\outputs\apk\debug\app-debug.apk",
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$AdbArgs
)

$checkerRoot = if (Test-Path -LiteralPath Env:ANDROID_CHECK_ROOT) { $env:ANDROID_CHECK_ROOT } else { "C:\Dev\Android-check" }
if ([string]::IsNullOrWhiteSpace($checkerRoot)) {
    throw "ANDROID_CHECK_ROOT must point to an Android-check directory."
}
$checkerScript = Join-Path $checkerRoot "tools\InstallDebugToDevice.ps1"
if (-not (Test-Path -LiteralPath $checkerScript -PathType Leaf)) {
    throw "Android-check script not found: $checkerScript (ANDROID_CHECK_ROOT)."
}
& $checkerScript `
    -ProjectRoot (Resolve-Path "$PSScriptRoot\..") `
    -ResolveOnly:$ResolveOnly `
    -NoBuild:$NoBuild `
    -ApkPath $ApkPath `
    @AdbArgs
exit $LASTEXITCODE
