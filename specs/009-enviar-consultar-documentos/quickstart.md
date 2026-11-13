# Quickstart: verificar a RC-08

Tudo a partir de `backend/`. Dados sintéticos.

## 1. Sem banco (o que o build prova)

```powershell
.\mvnw.cmd clean verify
```

Roda os testes unitários e de servlet com repositórios em memória e a área de arquivos numa pasta temporária. Os testes que usam o Oracle real ficam ignorados e são relatados como **não verificados**.

## 2. Contra o Oracle da faculdade (opt-in)

Pré-requisito: scripts `01_`, `02_`, `05_` e `06_` aplicados e **`07_documento_tipo_e_tentativa.sql`** aplicado (o grupo executa; sem ele o envio falha ao gravar).

```powershell
$env:DB_URL="jdbc:oracle:thin:@oracle.fiap.com.br:1521:orcl"
$env:DB_USERNAME="<conta do grupo>"; $env:DB_PASSWORD="<senha>"
$env:RC_TESTE_ORACLE="SIM"
.\mvnw.cmd -Dtest=DocumentoOracleTest test
```

O teste cria pedidos e documentos próprios e remove somente o que criou (linhas e arquivos).

## 3. Exercitar a API

```powershell
$env:DB_URL=…; $env:DB_USERNAME=…; $env:DB_PASSWORD=…
$env:DOCUMENT_STORAGE_PATH="C:\regulacheck-arquivos"     # pasta FORA do repositório
java -jar target\regulacheck-backend.jar
```

Com a sessão de uma paciente sintética (login em `POST /auth/login`), um rascunho criado pela RC-07 (`POST /solicitacoes`, id `12`) e um PDF sintético seu (a pasta `samples/` ainda não tem arquivos; os testes geram os próprios):

```powershell
# enviar (201); repita com a mesma chave para ver 200 sem duplicar
curl -i -X POST http://localhost:8080/solicitacoes/12/documentos `
  -H "Authorization: Bearer $TOKEN" -H "X-Chave-Envio: 7d1c1b8e-9f2a-4c3e-8a10-aa11bb22cc33" `
  -F "tipoDocumental=ENCAMINHAMENTO" -F "arquivo=@encaminhamento-sintetico.pdf"

curl -s http://localhost:8080/solicitacoes/12/documentos -H "Authorization: Bearer $TOKEN"
curl -s -OJ http://localhost:8080/solicitacoes/12/documentos/41 -H "Authorization: Bearer $TOKEN"
curl -s http://localhost:8080/solicitacoes/12 -H "Authorization: Bearer $TOKEN"   # versaoConjuntoDocumental, analise
```

Sem `DOCUMENT_STORAGE_PATH`, o envio e o download respondem 503; a lista e o pedido continuam funcionando. Os documentos da carga sintética (`02_`) apontam para arquivos fictícios: listar funciona, baixar responde 503.

## 4. Cenários de recusa para conferir

| Entrada | Esperado |
|---|---|
| texto renomeado para `.pdf` | 415 `EXTENSAO_INCOMPATIVEL` ou `TIPO_NAO_SUPORTADO`; lista e revisão inalteradas |
| arquivo > 5 MB | 413 `ARQUIVO_MUITO_GRANDE` |
| PDF com 6 páginas / PDF com senha | 400, detalhe `arquivo` |
| `tipoDocumental` ausente ou inválido | 400, detalhe `tipoDocumental` |
| quarto documento | 422 `REGRA_DE_NEGOCIO` |
| mesma chave, outro arquivo | 409 `CHAVE_ENVIO_REUTILIZADA` |
| pedido de outra paciente | 404 igual ao de pedido inexistente |

## Resultado esperado (preencher na verificação)

O comando, o total de testes e quantos ficaram ignorados vão na seção "Resultado da verificação" do `tasks.md`, com o que **não** foi executado no Oracle real.
