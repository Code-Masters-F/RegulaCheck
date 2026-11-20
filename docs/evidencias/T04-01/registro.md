# T04-01 — Criar rascunho válido

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
1. PB-2 POST /solicitacoes → HTTP 201 (`bruto/01-criar.txt`)
2. GET /solicitacoes/<id> → HTTP 200 (`bruto/02-detalhe.txt`)
3. GET /solicitacoes (conta A) → HTTP 200 (`bruto/03-lista-a.txt`)
4. GET /solicitacoes (conta B) → HTTP 200 (`bruto/04-lista-b.txt`)

### Esperado × obtido
- ✔ confere: 201 com Location /solicitacoes/<id>
- ✔ confere: estado RASCUNHO, revisao 1, simulacao true
- ✔ confere: servico presente (aviso fictício conferido no registro bruto)
- ✔ confere: GET devolve os mesmos dados (persistido)
- ✔ confere: pedido aparece na listagem de A
- ✔ confere: pedido NÃO aparece na listagem de B

## Status final do caso
Aprovado — grupo CodeMasters (com apoio do Claude Code) — 2026-11-19
