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

function Invoke-NativeCommand([string]$FilePath, [string[]]$Arguments) {
    $command = Get-Command $FilePath -CommandType Application -ErrorAction Stop
    $previousPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        $nativeOutput = @(& $command.Source @Arguments 2>&1)
        $exitCode = $LASTEXITCODE
        return [pscustomobject]@{ Output = $nativeOutput; ExitCode = $exitCode }
    } finally {
        $ErrorActionPreference = $previousPreference
    }
}

function Invoke-Adb([string[]]$Arguments) {
    $result = Invoke-NativeCommand 'adb' (@('-s', $Serial) + $Arguments)
    if ($result.ExitCode -ne 0) { throw "adb failed: $($result.Output -join "`n")" }
    return ($result.Output -join "`n")
}

function Assert-OptimizedArtifactIdentity(
    [string]$App,
    [string]$Mapping,
    [string]$AppManifest,
    [string]$DriverManifest
) {
    if ($AppManifest -cnotmatch '(?m)^\s*A: package="com\.finnvek\.knittools\.optimizedtest"') {
        throw 'Unexpected packaged target application ID.'
    }
    # The Baseline Profile producer is self-instrumenting.
    if ($DriverManifest -cnotmatch '(?m)^\s*A: package="com\.finnvek\.knittools\.baselineprofile"' -or
        $DriverManifest -cnotmatch 'android:targetPackage\([^\r\n)]*\)="com\.finnvek\.knittools\.baselineprofile"' -or
        $DriverManifest -cnotmatch 'android:name\([^\r\n)]*\)="androidx\.test\.runner\.AndroidJUnitRunner"') {
        throw 'Unexpected packaged driver application ID or instrumentation.'
    }

    $header = (Get-Content -LiteralPath $Mapping -TotalCount 12) -join "`n"
    $mappingId = [regex]::Match($header, '(?m)^# pg_map_id: (\S+)$').Groups[1].Value
    Add-Type -AssemblyName System.IO.Compression, System.IO.Compression.FileSystem
    $archive = [IO.Compression.ZipFile]::OpenRead($App)
    try {
        $dex = $archive.GetEntry('classes.dex')
        if ($null -eq $dex) { throw 'Target APK does not contain classes.dex.' }
        $reader = [IO.StreamReader]::new($dex.Open(), [Text.Encoding]::GetEncoding(28591))
        try {
            $marker = [regex]::Match($reader.ReadToEnd(), '~~R8\{[^}\x00]*"pg-map-id":"([^"]+)"[^}\x00]*\}')
        } finally {
            $reader.Dispose()
        }
    } finally {
        $archive.Dispose()
    }
    if (-not $mappingId -or $marker.Groups[1].Value -cne $mappingId) {
        throw 'R8 mapping does not match the target APK.'
    }
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
if ($driverMetadata.applicationId -ne 'com.finnvek.knittools.baselineprofile' -or $driverMetadata.variantName -ne 'benchmarkRelease') {
    throw 'Expected benchmarkRelease metadata for the Baseline Profile driver.'
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
$manifestResult = Invoke-NativeCommand $aapt @('dump', 'xmltree', $app, '--file', 'AndroidManifest.xml')
if ($manifestResult.ExitCode -ne 0) { throw 'Cannot inspect the packaged manifest.' }
$manifest = $manifestResult.Output
$manifestText = $manifest -join "`n"
$manifest | Set-Content (Join-Path $output 'packaged-manifest.txt')
if ($manifestText -notmatch 'android:testOnly[^\r\n]*=true' -or $manifestText -match 'android:debuggable[^\r\n]*=true') {
    throw 'The target must be test-only and non-debuggable.'
}
if ($manifestText -match '"(android\.permission\.INTERNET|com\.android\.vending\.BILLING|android\.permission\.CAMERA)"') {
    throw 'Unexpected target permission.'
}
$driverManifest = Invoke-NativeCommand $aapt @('dump', 'xmltree', $driver, '--file', 'AndroidManifest.xml')
if ($driverManifest.ExitCode -ne 0) { throw 'Cannot inspect the packaged driver manifest.' }
Assert-OptimizedArtifactIdentity $app $mapping $manifestText ($driverManifest.Output -join "`n")

$configurationResult = Invoke-NativeCommand $aapt @('dump', 'configurations', $app)
$configurations = $configurationResult.Output
if ($configurationResult.ExitCode -ne 0 -or ($configurations -join "`n") -match 'en-rXA|ar-rXB') {
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
    try {
        $logcat = Invoke-NativeCommand 'adb' @('-s', $Serial, 'logcat', '-d', '-t', '5000')
        $logcat.Output | Set-Content (Join-Path $output 'logcat.txt')
        if ($logcat.ExitCode -ne 0) { Write-Warning 'Logcat capture was incomplete; preserving its partial output.' }
        $pull = Invoke-NativeCommand 'adb' @('-s', $Serial, 'pull', '/sdcard/Android/data/com.finnvek.knittools.baselineprofile/files/optimized-smoke', $output)
        if ($pull.ExitCode -ne 0) { Write-Warning 'Optimized smoke evidence could not be fully copied.' }
    } finally {
        try {
            Invoke-Adb @('shell', 'wm', 'size', 'reset') | Out-Null
        } finally {
            Invoke-Adb @('shell', 'wm', 'density', 'reset') | Out-Null
        }
    }
}
