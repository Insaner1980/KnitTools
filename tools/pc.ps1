$ProjectCheckCommand = "pmd-check"
if (-not (Test-Path -LiteralPath Env:PMD_CPD_MINIMUM_TOKENS)) {
    $env:PMD_CPD_MINIMUM_TOKENS = "100"
}
& "C:\Dev\Android-check\tools\InvokeProjectCheck.ps1" -ProjectCheckCommand $ProjectCheckCommand @args
exit $LASTEXITCODE
