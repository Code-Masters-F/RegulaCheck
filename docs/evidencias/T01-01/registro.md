# T01-01 — Login com credenciais válidas

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
1. PB-1 login conta A → HTTP 200 (`bruto/01-login.txt`)
2. GET /auth/sessao → HTTP 200 (`bruto/02-sessao.txt`)

### Esperado × obtido
- ✔ confere: 200 com token, tipo Bearer, expiraEm e perfil PACIENTE
- ✔ confere: resposta sem senha nem hash
- ✔ confere: sessão 200 com a mesma identidade

## Status final do caso
Aprovado — grupo CodeMasters (com apoio do Claude Code) — 2026-11-19
