# T01-02 — Senha incorreta

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
1. login com senha errada → HTTP 401 (`bruto/01-login-errada.txt`)
2. GET /auth/sessao sem token → HTTP 401 (`bruto/02-sessao-sem-token.txt`)

### Esperado × obtido
- ✔ confere: 401 CREDENCIAIS_INVALIDAS
- ✔ confere: mensagem do plano (obtida: E-mail ou senha inválidos.)
- ✔ confere: nenhum token emitido
- ✔ confere: sessão sem token → 401 NAO_AUTENTICADO

## Status final do caso
Aprovado — grupo CodeMasters (com apoio do Claude Code) — 2026-11-19
