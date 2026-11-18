#Requires -Version 5.1
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$fixture = Join-Path ([IO.Path]::GetTempPath()) ('knittools-secret-archive-' + [Guid]::NewGuid().ToString('N'))
$secret = 'synthetic-only-' + [Guid]::NewGuid().ToString('N')
$RepoRoot = $fixture

# Load only the scanner functions; never execute the release wrapper or read real credentials.
$tokens = $null
$parseErrors = $null
$ast = [Management.Automation.Language.Parser]::ParseFile((Join-Path $PSScriptRoot 'release-surface.ps1'), [ref]$tokens, [ref]$parseErrors)
if ($parseErrors.Count) { throw 'Source parse failed' }
foreach ($name in @('Join-RepoPath', 'Test-BinaryFileContainsSecret', 'Remove-SecretValuesFromDiagnostic', 'Test-KnownRavelrySecrets')) {
    $node = $ast.Find({ param($n) $n -is [Management.Automation.Language.FunctionDefinitionAst] -and $n.Name -eq $name }, $true)
    if ($null -eq $node) { throw "Missing function: $name" }
    . ([scriptblock]::Create($node.Extent.Text))
}
function Get-KnownRavelrySecretValues { return @($secret) }
function Get-RelativeFilesForRoots { param($Roots) if ($Roots -contains 'app/build/outputs/apk') { return @($script:checkedFile) } }
function Add-Pass { param($Check, $Message) $script:result = 'PASS'; $script:message = $Message }
function Add-Fail { param($Check, $Message, $RelativePath) $script:result = 'FAIL'; $script:message = $Message }
function New-Archive {
    param([string]$Name, [string]$Content, [IO.Compression.CompressionLevel]$Level)
    $archive = [IO.Compression.ZipFile]::Open((Join-Path $fixture $Name), [IO.Compression.ZipArchiveMode]::Create)
    try {
        foreach ($entryName in @('empty/', 'assets/clean.txt', 'base/assets/config.bin')) {
            $entry = $archive.CreateEntry($entryName, $Level)
            $stream = $entry.Open()
            try {
                $value = if ($entryName -eq 'base/assets/config.bin') { $Content } else { '' }
                $bytes = [Text.Encoding]::UTF8.GetBytes($value)
                $stream.Write($bytes, 0, $bytes.Length)
            } finally { $stream.Dispose() }
        }
    } finally { $archive.Dispose() }
}
try {
    New-Item -ItemType Directory -Path $fixture | Out-Null
    $content = ('synthetic padding ' * 100) + $secret + (' more padding' * 100)
    foreach ($extension in @('apk', 'aab')) {
        foreach ($level in @('Optimal', 'NoCompression')) {
            $name = "$level.$extension"
            New-Archive $name $content $level
            if ($level -eq 'Optimal' -and [Text.Encoding]::UTF8.GetString([IO.File]::ReadAllBytes((Join-Path $fixture $name))).Contains($secret)) {
                throw 'Fixture does not demonstrate compressed-byte bypass'
            }
            $script:checkedFile = $name
            $script:result = $null
            $script:message = $null
            Test-KnownRavelrySecrets
            if ($result -ne 'FAIL' -or $message.Contains($secret)) { throw "Secret detection/redaction failed: $name" }
            Write-Output "PASS: detects synthetic secret in $name, value redacted"
            # A rename immediately after scanning verifies disposal on an early match.
            Move-Item -LiteralPath (Join-Path $fixture $name) -Destination (Join-Path $fixture "$name.checked")
        }
    }
    New-Archive 'clean.apk' 'clean synthetic content' 'Optimal'
    $unicodeSecret = 'synthetic-' + [char]0x00e4 + [char]0x00e4 + '-' + [char]0x79d8 + [char]0x5bc6 + '-value'
    New-Archive 'unicode.aab' $unicodeSecret 'Optimal'
    if (-not (Test-BinaryFileContainsSecret 'unicode.aab' @($unicodeSecret))) { throw 'UTF-8 matching failed' }
    if (Test-BinaryFileContainsSecret 'clean.apk' @($secret)) { throw 'False match: clean.apk' }
    Write-Output 'PASS: no match in clean.apk'
    $script:checkedFile = 'missing.aab'
    $script:result = $null
    $script:message = $null
    Test-KnownRavelrySecrets
    if ($result -ne 'FAIL' -or $message -ne 'Checked binary file is missing: missing.aab') { throw 'Disappeared checked archive must fail closed' }
    Write-Output 'PASS: disappeared checked archive fails closed, value redacted'
    Set-Content -LiteralPath (Join-Path $fixture 'output.bin') -Value $content -Encoding utf8
    if (-not (Test-BinaryFileContainsSecret 'output.bin' @($secret))) { throw 'Raw file regression' }
    $rawBytes = New-Object byte[] (1024 * 1024)
    $rawBytes[0] = 0xff
    $rawBytes[1] = 0xfe
    $needle = [Text.Encoding]::UTF8.GetBytes($unicodeSecret)
    [Array]::Copy($needle, 0, $rawBytes, $rawBytes.Length - $needle.Length, $needle.Length)
    [IO.File]::WriteAllBytes((Join-Path $fixture 'raw-boundary.bin'), $rawBytes)
    $elapsed = [Diagnostics.Stopwatch]::StartNew()
    if (-not (Test-BinaryFileContainsSecret 'raw-boundary.bin' @('', $unicodeSecret))) { throw 'Raw UTF-8 byte match failed' }
    if (Test-BinaryFileContainsSecret 'raw-boundary.bin' @('', $unicodeSecret.ToUpperInvariant(), ([string][char]0xfffd))) {
        throw 'Raw byte matching must remain ordinal and must not replace invalid UTF-8'
    }
    $elapsed.Stop()
    Write-Output "PASS: raw binary boundaries and ordinal matching ($($elapsed.ElapsedMilliseconds) ms)"
    Set-Content -LiteralPath (Join-Path $fixture 'invalid.apk') -Value 'invalid synthetic archive'
    $script:checkedFile = 'invalid.apk'
    $script:result = $null
    $script:message = $null
    Test-KnownRavelrySecrets
    if ($result -ne 'FAIL' -or $message.Contains($secret)) { throw 'Unreadable archive must fail closed without disclosing values' }
    Write-Output 'PASS: UTF-8, raw file fallback, corrupt archive fail closed'
    Write-Output 'release-secret-archive-test: PASS; synthetic values only'
}
finally {
    $resolved = [IO.Path]::GetFullPath($fixture)
    if (-not $resolved.StartsWith([IO.Path]::GetFullPath([IO.Path]::GetTempPath()), [StringComparison]::OrdinalIgnoreCase)) {
        throw 'Unsafe fixture cleanup path'
    }
    if (Test-Path -LiteralPath $resolved) {
        if ((Get-Item -LiteralPath $resolved).Attributes.HasFlag([IO.FileAttributes]::ReparsePoint) -or
            @(Get-ChildItem -LiteralPath $resolved -Recurse -Force -Attributes ReparsePoint).Count -gt 0) {
            throw "Unsafe fixture cleanup path: $resolved"
        }
        Remove-Item -LiteralPath $resolved -Recurse -Force
        if (Test-Path -LiteralPath $resolved) { throw "Fixture cleanup failed: $resolved" }
    }
}
