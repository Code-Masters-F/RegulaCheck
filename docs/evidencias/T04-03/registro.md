# T04-03 — Serviço inexistente

Plano: [plano-de-testes.md](../../testes/plano-de-testes.md) · Tipo de execução: API real + Oracle
Leitura e resumo: **simulados** (decisão de 12/11/2026).

## Execução 1
| Item | Valor |
|---|---|
| Data e hora | 2026-11-19 21:53 (America/Bahia) |
| Versão | commit `7532440` (alterações locais: só `docs/`, `specs/`, `README.md` e `entrega/`) · API: `backend/target/regulacheck-backend.jar` contra o Oracle da faculdade · dispositivo: n/a |
| Responsável e execução | grupo CodeMasters · executado com apoio do Claude Code |
| Pedido criado | id 151 |
| Resultado bruto | pasta `bruto/` (token e senha mascarados) |
| Status | Aprovado |

### Passos executados
1. POST com serviço inexistente → HTTP 422 (`bruto/01-post.txt`)
2. PUT com serviço inexistente → HTTP 422 (`bruto/02-put.txt`)

### Esperado × obtido
- ✔ confere: POST: 422 REGRA_DE_NEGOCIO, campo protocoloId
- ✔ confere: PUT: 422 REGRA_DE_NEGOCIO, campo protocoloId
- ✔ confere: rascunho mantém serviço e revisao

## Status final do caso
Aprovado — grupo CodeMasters (com apoio do Claude Code) — 2026-11-19
