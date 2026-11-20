# T01-05 — Rota privada sem sessão, com token inválido e depois do logout

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
1. GET /solicitacoes sem Authorization → HTTP 401 (`bruto/01-sem-token.txt`)
2. GET /solicitacoes com token inventado → HTTP 401 (`bruto/02-token-inventado.txt`)
3. login conta A (PB-1) → HTTP 200 (`bruto/03-login.txt`)
4. POST /auth/logout → HTTP 204 (`bruto/04-logout.txt`)
5. GET /solicitacoes com o token encerrado → HTTP 401 (`bruto/05-apos-logout.txt`)

### Esperado × obtido
- ✔ confere: passos 1 e 2: 401 NAO_AUTENTICADO com a mesma resposta
- ✔ confere: logout 204
- ✔ confere: token encerrado → 401 NAO_AUTENTICADO

## Status final do caso
Aprovado — grupo CodeMasters (com apoio do Claude Code) — 2026-11-19
