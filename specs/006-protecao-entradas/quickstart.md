# Quickstart: Proteger entradas e processamento (RC-06)

## Verificar (sem banco, sem Oracle)

```powershell
cd backend
.\mvnw.cmd clean verify
```

Esperado: todos os testes das proteções verdes (conferência de arquivo, armazenamento, download, cabeçalhos, conteúdo não confiável, varredura de segredos, SQL parametrizado, higiene de log) e os testes Oracle ignorados. A suíte das proteções deve rodar em até 60 s.

Só a varredura de segredos:

```powershell
.\mvnw.cmd -Dtest=VarreduraDeSegredosTest test
```

## Usar a área privada na prática (com o jar)

```powershell
$env:DOCUMENT_STORAGE_PATH = "C:\caminho\fora\do\repositorio\regulacheck-arquivos"
.\mvnw.cmd -DskipTests package
java -jar target\regulacheck-backend.jar
```

Sem `DOCUMENT_STORAGE_PATH` a API sobe normalmente; só o armazenamento recusa gravar. Em demonstração remota, defina `API_PUBLIC_URL=https://...` (com proxy/túnel TLS); um `http://` remoto impede a API de iniciar.

## Proteger uma rota nova de arquivo (≤ 20 min, SC-008)

1. **Receber**: leia o arquivo do pedido e chame `ConferenciaDeArquivo.conferir(fluxo, nomeOriginal)`; em `Recusado`, lance `motivo.excecao()` (já mapeia status e código).
2. **Guardar**: com `Aceito`, chame `ArmazenamentoPrivado.guardar(arquivoConferido)` e grave a linha de `RC_DOCUMENTO` com o `idInterno`, o `tipo`, o `tamanho`, as `paginas` e o `NomeExibicao`. Se a gravação da linha falhar, chame `remover(idInterno)`.
3. **Declarar a rota**: `RotasPrivadas.registrar(caminho, servlet, Perfil.PACIENTE)` (RC-05).
4. **Baixar**: busque o documento **já filtrado pela dona** (`Propriedade.exigir`), abra com `ArmazenamentoPrivado.abrir(idInterno)` e responda com `DownloadSeguro.enviar(...)`.
5. **Texto de documento**: use `TextoNaoConfiavel` e `ValidadorDeSaidaEstruturada` (ver contrato).
6. **Prove**: copie a matriz `DownloadAutenticadoTest` para o seu recurso.

## Conferência de código

- `SqlParametrizadoTest` falha o build se surgir consulta montada por concatenação.
- `VarreduraDeSegredosTest` falha o build se surgir segredo no repositório ou em `mobile/`.
