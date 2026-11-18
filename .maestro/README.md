# KnitTools Maestro tests

Maestro drives the installed Android app through its visible UI. These flows check
specific actions and expected results; a passing suite is not exhaustive testing.
Maestro Studio is an optional visual editor. These tests use the installed CLI.

## Dedicated test device

Every flow clears its app's data. Use only a disposable emulator named
`KnitToolsMaestro`, with English Android, normal animation settings, and the
default debug build. The runner refuses other AVD names and physical devices.
The original test environment was Android 12L / API 32, Pixel 4, 393 dp width.
Do not run these files against an installation containing personal projects.

To recreate the disposable device with the already installed API 32 system image:

```powershell
$sdkRoot = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
$env:ANDROID_AVD_HOME = Join-Path (Get-Location) 'reports\maestro\avd-home'
New-Item -ItemType Directory -Force $env:ANDROID_AVD_HOME | Out-Null
'no' | & "$sdkRoot\cmdline-tools\latest\bin\avdmanager.bat" create avd --name KnitToolsMaestro --package 'system-images;android-32;google_apis;x86_64' --device pixel_4
$maestroEmulator = Start-Process "$sdkRoot\emulator\emulator.exe" -ArgumentList @('-avd', 'KnitToolsMaestro', '-port', '5586', '-no-window', '-no-audio', '-no-snapshot', '-gpu', 'swiftshader_indirect', '-memory', '1536', '-cores', '2') -WindowStyle Hidden -PassThru
```

Wait until `adb -s emulator-5586 shell getprop sys.boot_completed` returns `1`.
After testing, stop this emulator with `adb -s emulator-5586 emu kill`.

After starting the dedicated emulator, build and install the current app, then run:

```powershell
.\gradlew.bat :app:assembleDebug --no-daemon --console=plain
adb -s emulator-5586 install -r app/build/outputs/apk/debug/app-debug.apk
.\.maestro\run.ps1 -Device emulator-5586
```

Replace the serial with the dedicated emulator's actual serial from `adb devices`.
The runner copies the small two-page PDF fixture to its Downloads folder and writes
JUnit results and failure diagnostics into a separate directory for each attempt
under ignored `reports/maestro/runs/`.
The default main selection omits the Ravelry sign-in-page flow. With separate
authorization to contact Ravelry, pass `-IncludeLive` to add that flow. It loads
the sign-in page without entering credentials. The default suite can still exercise
the installed app's usual Play billing lookup.
The camera flow uses the API 32 emulator's Camera app and emulated camera feed.
The PDF file is selected by name. The PDF drawing stroke and the backup export
picker's filename gesture use coordinates for this device. Backup filenames
include a per-run timestamp so a restore cannot accidentally validate an older
test backup.
Do not run another UI test on the same device at the same time.

## First launch

The normal debug build seeds demo projects. The existing onboarding preview
profile provides an empty, separate app for first-launch testing:

```powershell
.\gradlew.bat :app:assembleDebug '-Pknittools.isolatedTest=onboarding' --no-daemon --console=plain
adb -s emulator-5586 install -r app/build/onboarding/outputs/apk/debug/app-debug.apk
.\.maestro\run.ps1 -Device emulator-5586 -Onboarding
```

This profile uses preview prices and disables real billing/network services.
Neither suite proves Play purchases, production Pro gates, account login, physical
camera quality, every PDF format, or every locale. Default debug unlocks Pro tools.
The trial flow checks local trial state, including when Play billing is unavailable.
The project's previously completed 320 dp, 200% font, and TalkBack checks are not
repeated by this suite.

See `reports/maestro/report.md` for the actual run results and remaining limits.
