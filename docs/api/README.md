# API

Documentação à mão (a stack não usa Spring, então não há Swagger gerado). Dados exclusivamente sintéticos; o sistema não diagnostica, não define urgência nem autoriza atendimento.

| Documento | Conteúdo |
|---|---|
| [../../CONTRATO_API.md](../../CONTRATO_API.md) | Mapa de rotas (proposta) |
| [contratos-dados.md](contratos-dados.md) | Solicitação, upload, análise, pendência, submissão simulada |
| [erros.md](erros.md) | Formato padrão e catálogo de erros |
| [estados-e-revisoes.md](estados-e-revisoes.md) | 7 estados, transições e regra de revisão |
| [openapi.yaml](openapi.yaml) | OpenAPI escrito à mão (hoje cobre `/health`, `/auth/*` e o formato de erro) |
| [RegulaCheck.postman_collection.json](RegulaCheck.postman_collection.json) | Coleção Postman (health, login, sessão atual e logout) |

## Situação das rotas
- **Implementadas (RC-04):** `POST /auth/login`, `GET /auth/sessao` e `POST /auth/logout`; contrato em [specs/004-login-sessao/contracts/api-auth.md](../../specs/004-login-sessao/contracts/api-auth.md). Dependem do banco: sem `DB_*` configuradas respondem 503 `DEPENDENCIA_INDISPONIVEL`.
- **Implementada e verificada:** `GET /health` (200 `{"status":"UP"}`, só estado técnico; não consulta banco, logo UP não prova que o banco está acessível nem que o MVP está completo). Rota inexistente devolve 404 e método não permitido devolve 405, ambos no formato padrão de erro.
- **Planejadas (contrato definido, sem implementação):** todas as demais rotas de [CONTRATO_API.md](../../CONTRATO_API.md).

Regras a respeitar: isolamento por dona da solicitação; upload até 3 arquivos, 5 MB cada, PDF até 5 páginas, JPEG/PNG, só material sintético; resultado por regra e geral conforme [protocolo-academico.md](../protocolo-academico.md); submissão idempotente e simulada; telemetria em JSON por HTTP pelo simulador Java; resumo de IA e extração mockados e identificados como simulados. Arquitetura: [../arquitetura.md](../arquitetura.md).
