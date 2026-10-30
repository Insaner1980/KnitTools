[CmdletBinding()]
param(
    [Parameter(Mandatory)][ValidatePattern('^emulator-\d+$')][string]$Serial,
    [Parameter(Mandatory)][switch]$DisposableEmulator,
    [string]$OutputDirectory = 'reports/optimized-smoke',
    [ValidatePattern('^com\.finnvek\.knittools\.baselineprofile\.[A-Za-z0-9_.,#]+$')]
    [string]$Test = 'com.finnvek.knittools.baselineprofile.OptimizedSmokeTest',
    [ValidateRange(1, 100)][int]$ExpectedTests = 7
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
if (-not $DisposableEmulator) { throw 'A disposable emulator must be explicitly selected.' }
$root = Split-Path $PSScriptRoot -Parent
$output = [IO.Path]::GetFullPath((Join-Path $root $OutputDirectory))
New-Item -ItemType Directory -Force -Path $output | Out-Null
if (Test-Path (Join-Path $output 'instrumentation.txt')) { throw 'Choose a fresh evidence directory.' }

function Invoke-Adb([string[]]$Arguments) {
    $result = & adb -s $Serial @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) { throw "adb failed: $($result -join "`n")" }
    return ($result -join "`n")
}

if ((Invoke-Adb @('shell', 'getprop', 'ro.kernel.qemu')).Trim() -ne '1') {
    throw 'The selected device is not an emulator.'
}
$appDir = Join-Path $root 'app/build/outputs/apk/benchmarkRelease'
$driverDir = Join-Path $root 'baselineprofile/build/outputs/apk/benchmarkRelease'
$appMetadata = Get-Content (Join-Path $appDir 'output-metadata.json') -Raw | ConvertFrom-Json
$driverMetadata = Get-Content (Join-Path $driverDir 'output-metadata.json') -Raw | ConvertFrom-Json
if ($appMetadata.applicationId -ne 'com.finnvek.knittools.optimizedtest' -or $appMetadata.variantName -ne 'benchmarkRelease') {
    throw 'Build with -Pknittools.optimizedSmoke=true; refusing any other target.'
}
$app = Join-Path $appDir $appMetadata.elements[0].outputFile
$driver = Join-Path $driverDir $driverMetadata.elements[0].outputFile
$mapping = Join-Path $root 'app/build/outputs/mapping/benchmarkRelease/mapping.txt'
if (-not (Test-Path $mapping) -or (Get-Item $mapping).Length -eq 0) { throw 'R8 mapping is required.' }

$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } elseif ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } else {
    Split-Path (Split-Path (Get-Command adb).Source -Parent) -Parent
}
$suffix = if ([Environment]::OSVersion.Platform -eq [PlatformID]::Win32NT) { '.exe' } else { '' }
$aapt = Join-Path $sdk "build-tools/37.0.0/aapt2$suffix"
$manifest = & $aapt dump xmltree $app --file AndroidManifest.xml 2>&1
if ($LASTEXITCODE -ne 0) { throw 'Cannot inspect the packaged manifest.' }
$manifestText = $manifest -join "`n"
$manifest | Set-Content (Join-Path $output 'packaged-manifest.txt')
if ($manifestText -notmatch 'android:testOnly[^\r\n]*=true' -or $manifestText -match 'android:debuggable[^\r\n]*=true') {
    throw 'The target must be test-only and non-debuggable.'
}
if ($manifestText -match '"(android\.permission\.INTERNET|com\.android\.vending\.BILLING|android\.permission\.CAMERA)"') {
    throw 'Unexpected target permission.'
}
$configurations = & $aapt dump configurations $app 2>&1
if ($LASTEXITCODE -ne 0 -or ($configurations -join "`n") -match 'en-rXA|ar-rXB') {
    throw 'Unexpected pseudolocale or resource inspection failure.'
}
$configurations | Set-Content (Join-Path $output 'packaged-configurations.txt')
$identity = [ordered]@{
    package = $appMetadata.applicationId
    variant = $appMetadata.variantName
    version = $appMetadata.elements[0].versionName
    appSha256 = (Get-FileHash $app).Hash
    driverSha256 = (Get-FileHash $driver).Hash
    mappingSha256 = (Get-FileHash $mapping).Hash
    serial = $Serial
    api = (Invoke-Adb @('shell', 'getprop', 'ro.build.version.sdk')).Trim()
    test = $Test
    expectedTests = $ExpectedTests
}
$identity | ConvertTo-Json | Set-Content (Join-Path $output 'identity.json')
Copy-Item -LiteralPath $mapping -Destination (Join-Path $output 'mapping.txt')
Invoke-Adb @('install', '-r', '-t', $app) | Out-Null
Invoke-Adb @('install', '-r', '-t', $driver) | Out-Null
Invoke-Adb @('shell', 'pm', 'clear', 'com.finnvek.knittools.baselineprofile') | Out-Null
Invoke-Adb @('shell', 'settings', 'put', 'global', 'window_animation_scale', '0') | Out-Null
Invoke-Adb @('shell', 'settings', 'put', 'global', 'transition_animation_scale', '0') | Out-Null
Invoke-Adb @('shell', 'settings', 'put', 'global', 'animator_duration_scale', '0') | Out-Null
Invoke-Adb @('logcat', '-c') | Out-Null
try {
    $result = Invoke-Adb @('shell', 'am', 'instrument', '-w', '-r', '-e', 'class', $Test,
        '-e', 'optimizedSmokeAllowed', 'true',
        'com.finnvek.knittools.baselineprofile/androidx.test.runner.AndroidJUnitRunner')
    $result | Set-Content (Join-Path $output 'instrumentation.txt')
    Write-Output $result
    if ($result -notmatch "OK \($ExpectedTests tests?\)" -or
        $result -match 'FAILURES|INSTRUMENTATION_FAILED|Process crashed|INSTRUMENTATION_STATUS_CODE: -[234]|INSTRUMENTATION_RESULT: shortMsg') {
        throw 'Optimized smoke did not execute every expected test successfully. See instrumentation.txt.'
    }
} finally {
    & adb -s $Serial logcat -d -t 5000 2>&1 | Set-Content (Join-Path $output 'logcat.txt')
    if ($LASTEXITCODE -ne 0) { Write-Warning 'Logcat capture was incomplete; preserving its partial output.' }
    & adb -s $Serial pull '/sdcard/Android/data/com.finnvek.knittools.baselineprofile/files/optimized-smoke' $output 2>&1 | Out-Null
    Invoke-Adb @('shell', 'wm', 'size', 'reset') | Out-Null
    Invoke-Adb @('shell', 'wm', 'density', 'reset') | Out-Null
}
