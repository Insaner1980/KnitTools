param(
    [switch]$ResolveOnly,
    [switch]$NoBuild,
    [string]$ApkPath = "app\build\outputs\apk\debug\app-debug.apk",
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$AdbArgs
)

& "C:\Dev\Android-check\tools\InstallDebugToDevice.ps1" `
    -ProjectRoot (Resolve-Path "$PSScriptRoot\..") `
    -ResolveOnly:$ResolveOnly `
    -NoBuild:$NoBuild `
    -ApkPath $ApkPath `
    @AdbArgs
exit $LASTEXITCODE
