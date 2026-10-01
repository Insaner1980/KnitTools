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
foreach ($name in @('Join-RepoPath', 'Test-BinaryFileContainsSecret', 'Test-KnownRavelrySecrets')) {
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
            Test-KnownRavelrySecrets
            if ($result -ne 'FAIL' -or $message.Contains($secret)) { throw "Secret detection/redaction failed: $name" }
            Write-Output "PASS: detects synthetic secret in $name, value redacted"
            # A rename immediately after scanning verifies disposal on an early match.
            Move-Item -LiteralPath (Join-Path $fixture $name) -Destination (Join-Path $fixture "$name.checked")
        }
    }
    New-Archive 'clean.apk' 'clean synthetic content' 'Optimal'
    New-Archive 'unicode.aab' 'synthetic-ää-秘密-value' 'Optimal'
    if (-not (Test-BinaryFileContainsSecret 'unicode.aab' @('synthetic-ää-秘密-value'))) { throw 'UTF-8 matching failed' }
    if (Test-BinaryFileContainsSecret 'clean.apk' @($secret)) { throw 'False match: clean.apk' }
    Write-Output 'PASS: no match in clean.apk'
    $script:checkedFile = 'missing.aab'
    Test-KnownRavelrySecrets
    if ($result -ne 'FAIL' -or $message -ne 'Checked binary file is missing: missing.aab') { throw 'Disappeared checked archive must fail closed' }
    Write-Output 'PASS: disappeared checked archive fails closed, value redacted'
    Set-Content -LiteralPath (Join-Path $fixture 'output.bin') -Value $content -Encoding utf8
    if (-not (Test-BinaryFileContainsSecret 'output.bin' @($secret))) { throw 'Raw file regression' }
    Set-Content -LiteralPath (Join-Path $fixture 'invalid.apk') -Value 'invalid synthetic archive'
    $script:checkedFile = 'invalid.apk'
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
    if (Test-Path -LiteralPath $resolved) { Remove-Item -LiteralPath $resolved -Recurse -Force }
}
