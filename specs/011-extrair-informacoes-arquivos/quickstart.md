# Quickstart: Extrair informações dos arquivos (RC-10)

Pré-requisitos: os da RC-08/RC-09 (`docs/ambiente.md`), mais o script **`08_extracao.sql`** aplicado (com autorização) e `DOCUMENT_STORAGE_PATH` configurada. API no ar, sessão de paciente sintética. A leitura é **simulada**.

Variáveis: `$TOKEN`, `$PEDIDO` (rascunho com as amostras de `samples/` anexadas pela RC-08).

## 1. Ler e consultar

```bash
curl -i -X POST "$API/solicitacoes/$PEDIDO/extracoes" -H "Authorization: Bearer $TOKEN"   # 201, simulada: true
curl -i -X POST "$API/solicitacoes/$PEDIDO/extracoes" -H "Authorization: Bearer $TOKEN"   # 200, mesma leitura
curl -i "$API/solicitacoes/$PEDIDO/extracoes" -H "Authorization: Bearer $TOKEN"           # 200, situacao ATUAL
```

Conferir com `samples/gabarito.md`: valores, situações e origens dos campos de cada amostra.

## 2. Leitura que não deu certo

Anexar um arquivo que não é amostra (PDF qualquer) e ler: o documento vem `LEITURA_VAZIA`, `reenviar: true`, sem campos; nenhuma pendência. Pedido sem documentos → 422 `SEM_DOCUMENTOS`.

## 3. Leitura desatualizada

Substituir ou remover um documento (RC-09) e consultar: `situacao: DESATUALIZADA`. Ler de novo: nova leitura, `ATUAL`.

## 4. Casos que só os testes reproduzem

Falha do leitor (503 `LEITURA_INDISPONIVEL`), arquivo `PROTEGIDO`, saída do leitor fora do contrato e pedido `SUBMETIDA_SIMULACAO` são cobertos por testes automatizados com leitor falso e pedidos montados em memória; não há rota que os produza no ambiente.

## Testes automatizados

```bash
cd backend && ./mvnw clean verify                       # sem banco
RC_TESTE_ORACLE=SIM ./mvnw -Dtest=ExtracaoOracleTest test   # opt-in, exige o 08_ e as variáveis DB_*
```
