# Quickstart: Conferir a transcrição (RC-11)

Pré-requisitos: os da RC-10 (`docs/ambiente.md`), mais o script **`09_conferencia.sql`** aplicado (com autorização). API no ar, sessão de paciente sintética, um pedido com as amostras de `samples/` e uma leitura (RC-10). A leitura é **simulada**.

Variáveis: `$TOKEN`, `$PEDIDO`, `$EXTRACAO` (o `id` devolvido por `POST /solicitacoes/$PEDIDO/extracoes`), `$DOC` (um `documentoId` da leitura).

## 1. Consultar

```bash
curl -i "$API/solicitacoes/$PEDIDO/extracoes/$EXTRACAO/conferencia" -H "Authorization: Bearer $TOKEN"
# 200: situacao PENDENTE (ou REQUER_CONFERENCIA se houver campo ilegivel/documento nao lido), liberadoParaSubmissao false
```

## 2. Confirmar e divergir

```bash
curl -i -X PUT "$API/solicitacoes/$PEDIDO/extracoes/$EXTRACAO/conferencia" -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"decisoes\":[{\"documentoId\":\"$DOC\",\"codigo\":\"NOME\",\"decisao\":\"CONFIRMADA\"},{\"documentoId\":\"$DOC\",\"codigo\":\"DATA_EMISSAO\",\"decisao\":\"DIVERGENTE\",\"motivo\":\"VALOR_DIFERENTE_DO_DOCUMENTO\"}]}"
# 200: DATA_EMISSAO passa a divergente; situacao REQUER_CONFERENCIA
```

## 3. Recusas

- Corpo com `"valor":"..."` em uma decisão → 400 (nada gravado).
- Decisão em campo ilegível, ou confirmar campo não localizado → 422 `DECISAO_NAO_APLICAVEL`.
- Depois de substituir ou remover um documento (RC-09): `GET` mostra `DESATUALIZADA` e `PUT` → 409 até uma nova leitura (`POST …/extracoes`).

## 4. Casos que só os testes reproduzem

Pedido `SUBMETIDA_SIMULACAO` (409 no `PUT`, `GET` 200) e concorrência são cobertos por testes automatizados com pedidos montados em memória.

## Testes automatizados

```bash
cd backend && ./mvnw clean verify                              # sem banco
RC_TESTE_ORACLE=SIM ./mvnw -Dtest=ConferenciaOracleTest test   # opt-in, exige 08_ e 09_ e as variáveis DB_*
```
