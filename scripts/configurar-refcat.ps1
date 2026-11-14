<#
Configura a chave do Refcat somente no ambiente local do usuário Windows.
O conteúdo da chave não é exibido, gravado neste arquivo nem adicionado ao Git.
Execute este arquivo no seu próprio PowerShell e digite a chave no campo oculto.
#>
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
Write-Host 'Refcat — chave API local para o Codex'
Write-Host 'Cole a chave abaixo. Os caracteres ficarão ocultos.'
$refcatSecret = Read-Host 'Chave API do Refcat' -AsSecureString
if ($refcatSecret.Length -eq 0) {
    throw 'Nenhuma chave foi informada. A configuração anterior foi preservada.'
}
$refcatSecretPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($refcatSecret)
try {
    $refcatPlainValue = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($refcatSecretPointer)
    [Environment]::SetEnvironmentVariable('REFCAT_API_KEY', $refcatPlainValue, 'User')
    $env:REFCAT_API_KEY = $refcatPlainValue
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($refcatSecretPointer)
    $refcatPlainValue = $null
    $refcatSecret.Dispose()
}
Write-Host 'Chave configurada localmente como REFCAT_API_KEY.'
Write-Host 'Volte ao chat e avise que concluiu. Não envie a chave pelo chat.'
