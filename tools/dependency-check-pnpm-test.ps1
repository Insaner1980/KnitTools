#Requires -Version 5.1

[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$gradleWrapper = Join-Path $repoRoot "gradlew.bat"
$tempRoot = Join-Path ([IO.Path]::GetTempPath()) ("knittools-owasp-pnpm-" + [Guid]::NewGuid().ToString("N"))
$initScript = Join-Path $tempRoot "dependency-check-pnpm-probe.init.gradle"

try {
    $pnpmCommands = @(Get-Command "pnpm.cmd" -CommandType Application -ErrorAction Stop)
    New-Item -ItemType Directory -Force -Path $tempRoot | Out-Null
    $initScriptContent = @'
gradle.afterProject { project ->
    if (project.path == ":app") {
        project.tasks.register("printDependencyCheckPnpmPath") {
            doLast {
                def dependencyCheck = project.extensions.getByName("dependencyCheck")
                def pnpmPath = dependencyCheck.analyzers.nodeAudit.pnpmPath.getOrNull()
                println("DEPENDENCY_CHECK_PNPM_PATH=" + (pnpmPath ?: ""))
            }
        }
    }
}
'@
    # Windows PowerShell 5.1:n utf8 tarkoittaa BOMillista UTF-8:aa, jota Gradle
    # ei hyväksy init-skriptin ensimmäisenä merkkinä.
    [IO.File]::WriteAllText($initScript, $initScriptContent, [Text.UTF8Encoding]::new($false))

    $previousErrorActionPreference = $ErrorActionPreference
    try {
        # Windows PowerShell 5.1 muuntaa native stderr -rivit ErrorRecord-olioiksi.
        # Kerää Gradlen molemmat virrat ja ratkaise onnistuminen vasta exit-koodista.
        $ErrorActionPreference = "Continue"
        $output =
            & $gradleWrapper `
                --no-daemon `
                :app:printDependencyCheckPnpmPath `
                --no-configuration-cache `
                --console=plain `
                -I $initScript 2>&1
        $exitCode = $LASTEXITCODE
    }
    finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    if ($exitCode -ne 0) {
        throw "Gradle probe epäonnistui (exit $exitCode):`n$($output -join "`n")"
    }

    $pathLine =
        @($output) |
            Where-Object { $_ -is [string] -and $_.StartsWith("DEPENDENCY_CHECK_PNPM_PATH=") } |
            Select-Object -Last 1
    if ([string]::IsNullOrWhiteSpace([string]$pathLine)) {
        throw "Dependency-Checkin pnpmPath-arvo puuttuu Gradle-proben tulosteesta."
    }

    $configuredPath = ([string]$pathLine).Substring("DEPENDENCY_CHECK_PNPM_PATH=".Length)
    if ([string]::IsNullOrWhiteSpace($configuredPath)) {
        throw "Dependency-Checkin pnpmPath on tyhjä, vaikka pnpm.cmd löytyy PATHista."
    }
    if (-not (Test-Path -LiteralPath $configuredPath -PathType Leaf)) {
        throw "Dependency-Checkin pnpmPath ei osoita tiedostoon: $configuredPath"
    }
    $configuredFullPath = [IO.Path]::GetFullPath($configuredPath)
    $knownPnpmPaths = @($pnpmCommands | ForEach-Object { [IO.Path]::GetFullPath($_.Source) })
    if ($configuredFullPath -notin $knownPnpmPaths) {
        throw "Dependency-Check käyttää PATHin ulkopuolista pnpm-ohjelmaa: '$configuredPath'."
    }

    Write-Output "dependency-check-pnpm-test.ps1 OK"
}
finally {
    if (Test-Path -LiteralPath $tempRoot) {
        Remove-Item -LiteralPath $tempRoot -Recurse -Force
    }
}
