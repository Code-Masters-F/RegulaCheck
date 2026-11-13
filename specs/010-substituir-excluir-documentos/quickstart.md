# Quickstart: Substituir documento e excluir rascunho (RC-09)

Pré-requisitos: os da RC-08 (`docs/ambiente.md`): JDK 21+, banco com os scripts `01_`…`07_`, `DB_*` e `DOCUMENT_STORAGE_PATH` exportadas, API no ar, sessão de paciente sintética. Não há script novo nesta história.

Variáveis: `$TOKEN` (login), `$PEDIDO` (rascunho com documentos da RC-08), `$DOC` (um de seus documentos).

## 1. Remover um documento

```bash
curl -i -X DELETE "$API/solicitacoes/$PEDIDO/documentos/$DOC" -H "Authorization: Bearer $TOKEN"
# 200 {"versaoConjuntoDocumental":N+1,"documentosDisponiveis":M-1}
curl -i "$API/solicitacoes/$PEDIDO/documentos/$DOC" -H "Authorization: Bearer $TOKEN"   # 404
curl -i -X DELETE "$API/solicitacoes/$PEDIDO/documentos/$DOC" -H "Authorization: Bearer $TOKEN"   # 404, versão não muda
```

## 2. Substituir um documento

```bash
curl -i -X PUT "$API/solicitacoes/$PEDIDO/documentos/$DOC" -H "Authorization: Bearer $TOKEN" \
  -H "X-Chave-Envio: troca-0001-abc" -F "tipoDocumental=ENCAMINHAMENTO" -F "arquivo=@corrigido.pdf;type=application/pdf"
# 201 + Location do documento novo; repetir o mesmo comando => 200 sem duplicar
```

Arquivo inválido (por exemplo, um `.txt` renomeado para `.pdf`) → 415, e o documento antigo continua abrindo.

## 3. Excluir o rascunho

```bash
curl -i -X DELETE "$API/solicitacoes/$PEDIDO" -H "Authorization: Bearer $TOKEN"                                   # 400 (sem confirmação)
curl -i -X DELETE "$API/solicitacoes/$PEDIDO" -H "Authorization: Bearer $TOKEN" -H "X-Confirmar-Exclusao: EXCLUIR"  # 204
curl -i "$API/solicitacoes/$PEDIDO" -H "Authorization: Bearer $TOKEN"                                              # 404
```

Conferir a pasta de `DOCUMENT_STORAGE_PATH`: nenhum arquivo dos documentos do pedido excluído deve restar.

## 4. Pedido submetido

Só é possível reproduzir com pedido montado direto em `SUBMETIDA_SIMULACAO` (nenhuma rota o produz ainda): remover, substituir e excluir respondem 409 `CONFLITO_ESTADO`; `GET` e download continuam 200. Cobertura automatizada em memória; **não** verificável de ponta a ponta no Oracle até existir a história de submissão.

## Testes automatizados

```powershell
cd backend
.\mvnw.cmd clean verify          # tudo sem banco
$env:RC_TESTE_ORACLE="SIM"; .\mvnw.cmd test "-Dtest=DocumentoOracleTest,AlteracaoDocumentoOracleTest"   # opt-in, exige DB_* e o 07_ aplicado
```
