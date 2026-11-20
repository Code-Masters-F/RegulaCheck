# T04-02 — Campo obrigatório ausente, vazio ou inválido

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
1. POST corpo 1 → HTTP 400 (`bruto/01-post-1.txt`)
2. POST corpo 2 → HTTP 400 (`bruto/02-post-2.txt`)
3. POST corpo 3 → HTTP 400 (`bruto/03-post-3.txt`)
4. POST corpo 4 → HTTP 400 (`bruto/04-post-4.txt`)
5. POST corpo 5 → HTTP 400 (`bruto/05-post-5.txt`)
6. PUT corpo 1 → HTTP 400 (`bruto/06-put-1.txt`)
7. PUT corpo 2 → HTTP 400 (`bruto/07-put-2.txt`)
8. PUT corpo 3 → HTTP 400 (`bruto/08-put-3.txt`)
9. PUT corpo 4 → HTTP 400 (`bruto/09-put-4.txt`)
10. PUT corpo 5 → HTTP 400 (`bruto/10-put-5.txt`)

### Esperado × obtido
- ✔ confere: POST corpo 1: 400 REQUISICAO_INVALIDA, campo protocoloId
- ✔ confere: POST corpo 2: 400 REQUISICAO_INVALIDA, campo protocoloId
- ✔ confere: POST corpo 3: 400 REQUISICAO_INVALIDA, campo protocoloId
- ✔ confere: POST corpo 4: 400 REQUISICAO_INVALIDA, campo protocoloId
- ✔ confere: POST corpo 5: 400 REQUISICAO_INVALIDA, campo protocoloId
- ✔ confere: nenhum pedido criado (contagem igual)
- ✔ confere: PUT corpo 1: 400 REQUISICAO_INVALIDA, campo protocoloId
- ✔ confere: PUT corpo 2: 400 REQUISICAO_INVALIDA, campo protocoloId
- ✔ confere: PUT corpo 3: 400 REQUISICAO_INVALIDA, campo protocoloId
- ✔ confere: PUT corpo 4: 400 REQUISICAO_INVALIDA, campo protocoloId
- ✔ confere: PUT corpo 5: 400 REQUISICAO_INVALIDA, campo protocoloId
- ✔ confere: PUT inválido não persistiu (serviço, revisao e atualizadaEm iguais)

## Status final do caso
Aprovado — grupo CodeMasters (com apoio do Claude Code) — 2026-11-19
