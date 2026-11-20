# T01-04 — Login com campos ausentes ou vazios

Plano: [plano-de-testes.md](../../testes/plano-de-testes.md) · Tipo de execução: API real + Oracle
Leitura e resumo: **simulados** (decisão de 12/11/2026).

## Execução 1
| Item | Valor |
|---|---|
| Data e hora | 2026-11-19 21:53 (America/Bahia) |
| Versão | commit `7532440` (alterações locais: só `docs/`, `specs/`, `README.md` e `entrega/`) · API: `backend/target/regulacheck-backend.jar` contra o Oracle da faculdade · dispositivo: n/a |
| Responsável e execução | grupo CodeMasters · executado com apoio do Claude Code |
| Pedido criado | n/a |
| Resultado bruto | pasta `bruto/` (token e senha mascarados) |
| Status | Aprovado |

### Passos executados
1. (a) {} → HTTP 400 (`bruto/01-a-vazio-objeto.txt`)
2. (b) campos vazios → HTTP 400 (`bruto/02-b-campos-vazios.txt`)
3. (c) corpo não JSON → HTTP 400 (`bruto/03-c-nao-json.txt`)
4. (d) text/plain → HTTP 415 (`bruto/04-d-text-plain.txt`)

### Esperado × obtido
- ✔ confere: (a) 400 REQUISICAO_INVALIDA com detalhes
- ✔ confere: (b) 400 REQUISICAO_INVALIDA com detalhes
- ✔ confere: (c) 400
- ✔ confere: (d) 415 TIPO_NAO_SUPORTADO

## Status final do caso
Aprovado — grupo CodeMasters (com apoio do Claude Code) — 2026-11-19
