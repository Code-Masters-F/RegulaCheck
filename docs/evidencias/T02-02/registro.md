# T02-02 — A não altera nem exclui pedido de B

Plano: [plano-de-testes.md](../../testes/plano-de-testes.md) · Tipo de execução: API real + Oracle
Leitura e resumo: **simulados** (decisão de 12/11/2026).

## Execução 1
| Item | Valor |
|---|---|
| Data e hora | 2026-11-19 21:53 (America/Bahia) |
| Versão | commit `7532440` (alterações locais: só `docs/`, `specs/`, `README.md` e `entrega/`) · API: `backend/target/regulacheck-backend.jar` contra o Oracle da faculdade · dispositivo: n/a |
| Responsável e execução | grupo CodeMasters · executado com apoio do Claude Code |
| Pedido criado | rascB 152 (conta B) |
| Resultado bruto | pasta `bruto/` (token e senha mascarados) |
| Status | Aprovado |

### Passos executados
1. PUT como A → HTTP 404 (`bruto/01-put-a.txt`)
2. DELETE como A → HTTP 404 (`bruto/02-delete-a.txt`)
3. GET como B → HTTP 200 (`bruto/03-get-b.txt`)

### Esperado × obtido
- ✔ confere: PUT alheio: 404 idêntico ao de inexistente
- ✔ confere: DELETE alheio: 404 NAO_ENCONTRADO
- ✔ confere: rascunho de B existe, revisao e atualizadaEm iguais

## Status final do caso
Aprovado — grupo CodeMasters (com apoio do Claude Code) — 2026-11-19
