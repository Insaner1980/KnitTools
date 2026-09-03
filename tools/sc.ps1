$ProjectCheckCommand = "security-check"
$ProjectRoot = Split-Path -Parent $PSScriptRoot
& "C:\Dev\Android-check\tools\InvokeProjectCheck.ps1" -ProjectCheckCommand $ProjectCheckCommand -Root $ProjectRoot @args
exit $LASTEXITCODE
