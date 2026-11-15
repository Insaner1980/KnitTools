param(
    [Parameter(Mandatory)]
    [ValidatePattern('^emulator-[0-9]+$')]
    [string]$Device,
    [switch]$Onboarding,
    [switch]$IncludeLive
)

$ErrorActionPreference = 'Stop'
if ($Onboarding -and $IncludeLive) { throw '-IncludeLive applies only to the main suite.' }
$avdName = @(& adb -s $Device emu avd name)
if ($LASTEXITCODE -ne 0 -or $avdName[0].Trim() -ne 'KnitToolsMaestro') {
    throw 'These flows clear app data. Use the dedicated KnitToolsMaestro emulator.'
}

$repoRoot = Split-Path -Parent $PSScriptRoot
Push-Location $repoRoot
try {
    if ($Onboarding) {
        $suite = 'onboarding'
        $flowNames = @('onboarding/01-first-launch.yaml')
    } else {
        $suite = if ($IncludeLive) { 'main-live' } else { 'main-local' }
        $flowNames = @(
            '01-project-counter.yaml'
            '02-calculators.yaml'
            '03-web-pattern.yaml'
            '04-yarn.yaml'
            '05-library-reference.yaml'
            '06-settings-guide.yaml'
            '07-notes.yaml'
            '08-folders.yaml'
            '09-pdf.yaml'
            '10-measurements.yaml'
            '11-extra-counter-reminder.yaml'
            '12-language.yaml'
            '13-backup.yaml'
            '14-pro-trial.yaml'
            '15-project-lifecycle.yaml'
            '16-photo.yaml'
            '17-project-yarn.yaml'
            '19-photo-pdf.yaml'
        )
        if ($IncludeLive) { $flowNames += '18-ravelry-browser.yaml' }
    }
    $flows = @(($flowNames | Sort-Object) | ForEach-Object {
        Get-Item -LiteralPath (Join-Path $PSScriptRoot $_) | Select-Object -ExpandProperty FullName
    })
    if (!$Onboarding) {
        & adb -s $Device push .maestro/fixtures/maestro-pattern.pdf /sdcard/Download/maestro-pattern.pdf
        if ($LASTEXITCODE -ne 0) { throw 'Could not copy the PDF fixture.' }
        & adb -s $Device shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Download/maestro-pattern.pdf
        if ($LASTEXITCODE -ne 0) { throw 'Could not register the PDF fixture.' }
    }
    $reportDir = "reports/maestro/runs/$suite-$(Get-Date -Format 'yyyyMMdd-HHmmss-fffffff')"
    New-Item -ItemType Directory -Path $reportDir | Out-Null
    & maestro --device $Device test --no-reinstall-driver --test-output-dir $reportDir --format junit --output "$reportDir/result.xml" @flows
    if ($LASTEXITCODE -ne 0) { throw "Maestro $suite suite failed. See $reportDir/." }
    Write-Output "Maestro $suite suite passed. See $reportDir/."
}
finally {
    Pop-Location
}
