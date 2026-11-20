# T02-01 — A não vê pedidos de B na listagem nem no detalhe

Plano: [plano-de-testes.md](../../testes/plano-de-testes.md) · Tipo de execução: API real + Oracle
Leitura e resumo: **simulados** (decisão de 12/11/2026).

## Execução 1
| Item | Valor |
|---|---|
| Data e hora | 2026-11-19 21:53 (America/Bahia) |
| Versão | commit `7532440` (alterações locais: só `docs/`, `specs/`, `README.md` e `entrega/`) · API: `backend/target/regulacheck-backend.jar` contra o Oracle da faculdade · dispositivo: n/a |
| Responsável e execução | grupo CodeMasters · executado com apoio do Claude Code |
| Pedido criado | idB 152 (conta B) |
| Resultado bruto | pasta `bruto/` (token e senha mascarados) |
| Status | Aprovado |

### Passos executados
1. GET /solicitacoes como A → HTTP 200 (`bruto/01-lista-a.txt`)
2. GET /solicitacoes/<idB> como A → HTTP 404 (`bruto/02-detalhe-b.txt`)
3. GET /solicitacoes/999999999 como A → HTTP 404 (`bruto/03-detalhe-inexistente.txt`)

### Esperado × obtido
- ✔ confere: listagem de A sem o pedido de B
- ✔ confere: pedido alheio: 404 NAO_ENCONTRADO
- ✔ confere: corpo idêntico ao do pedido inexistente

## Status final do caso
Aprovado — grupo CodeMasters (com apoio do Claude Code) — 2026-11-19
