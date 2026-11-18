#Requires -Version 5.1
$ErrorActionPreference = 'Stop'
$fixture = Join-Path ([IO.Path]::GetTempPath()) ('knittools-exported-' + [Guid]::NewGuid().ToString('N'))
$RepoRoot = $fixture
$AndroidNamespace = 'http://schemas.android.com/apk/res/android'
$tokens = $null
$parseErrors = $null
$ast = [Management.Automation.Language.Parser]::ParseFile((Join-Path $PSScriptRoot 'release-surface.ps1'), [ref]$tokens, [ref]$parseErrors)
if ($parseErrors.Count) { throw 'Source parse failed' }
# Load only production functions; do not execute the release wrapper.
foreach ($name in @('Join-RepoPath', 'Read-TextFile', 'Read-XmlFile', 'Get-AndroidAttribute', 'Get-LineNumber', 'Test-ExportedSurface')) {
    $node = $ast.Find({ param($n) $n -is [Management.Automation.Language.FunctionDefinitionAst] -and $n.Name -eq $name }, $true)
    if ($null -eq $node) { throw "Missing function: $name" }
    . ([scriptblock]::Create($node.Extent.Text))
}
function Add-Pass { param($Check, $Message) $script:result = 'PASS'; $script:message = $Message }
function Add-Fail { param($Check, $Message, $RelativePath, $Line) $script:result = 'FAIL'; $script:message = $Message }
$components = @'
<activity android:name=".MainActivity" android:exported="true" />
<receiver android:name=".widget.CounterWidgetReceiver" android:exported="true" />
<receiver android:name=".widget.CounterWidgetActions" android:exported="false" />
<provider android:name="androidx.core.content.FileProvider" android:exported="false" />
'@
$cases = @(
    @{ Name = 'allowed components'; Extra = ''; Expected = 'PASS' },
    @{ Name = 'private alias'; Extra = '<activity-alias android:name=".PrivateAlias" android:targetActivity=".MainActivity" android:exported="false" />'; Expected = 'PASS' },
    @{ Name = 'public alias'; Extra = '<activity-alias android:name=".PublicAlias" android:targetActivity=".MainActivity" android:exported="true" />'; Expected = 'FAIL'; Match = "unexpected exported true component 'activity-alias|.PublicAlias'" },
    @{ Name = 'alias named like allowed activity'; Extra = '<activity-alias android:name=".MainActivity" android:targetActivity=".MainActivity" android:exported="true" />'; Expected = 'FAIL'; Match = "unexpected exported true component 'activity-alias|.MainActivity'" }
)
try {
    New-Item -ItemType Directory -Path (Join-Path $fixture 'app/src/main') -Force | Out-Null
    foreach ($case in $cases) {
        $manifest = "<manifest xmlns:android=`"$AndroidNamespace`"><application>`n$components`n$($case.Extra)`n</application></manifest>"
        Set-Content -LiteralPath (Join-Path $fixture 'app/src/main/AndroidManifest.xml') -Value $manifest -Encoding utf8
        $script:result = $null
        Test-ExportedSurface
        if ($result -ne $case.Expected -or ($case.Match -and -not $message.Contains($case.Match))) {
            throw "Case '$($case.Name)': expected $($case.Expected), got $result ($message)"
        }
        Write-Output "PASS: $($case.Name)"
    }
    Write-Output 'release-exported-surface-test: 4/4 PASS'
} finally {
    $resolved = [IO.Path]::GetFullPath($fixture)
    $separators = [char[]]@([IO.Path]::DirectorySeparatorChar, [IO.Path]::AltDirectorySeparatorChar)
    $tempRoot = [IO.Path]::GetFullPath([IO.Path]::GetTempPath()).TrimEnd($separators) + [IO.Path]::DirectorySeparatorChar
    if (-not $resolved.StartsWith($tempRoot, [StringComparison]::OrdinalIgnoreCase)) { throw 'Unsafe fixture cleanup path' }
    if (Test-Path -LiteralPath $resolved) { Remove-Item -LiteralPath $resolved -Recurse -Force }
}
