# API

Documentação à mão (a stack não usa Spring, então não há Swagger gerado). Dados exclusivamente sintéticos; o sistema não diagnostica, não define urgência nem autoriza atendimento.

| Documento | Conteúdo |
|---|---|
| [../../CONTRATO_API.md](../../CONTRATO_API.md) | Mapa de rotas (proposta) |
| [contratos-dados.md](contratos-dados.md) | Solicitação, upload, análise, pendência, submissão simulada |
| [erros.md](erros.md) | Formato padrão, catálogo de erros e ordem de verificação 401 → 403 → 404 → 400 |
| [autorizacao.md](autorizacao.md) | Perfis, dona do recurso, credencial do simulador e guia para proteger uma rota nova (RC-05) |
| [protecoes.md](protecoes.md) | Conferência de arquivos, área privada, download seguro, texto puro, conteúdo não confiável, segredos e HTTPS: guia para as próximas histórias (RC-06) |
| [estados-e-revisoes.md](estados-e-revisoes.md) | 7 estados, transições e regra de revisão |
| [openapi.yaml](openapi.yaml) | OpenAPI escrito à mão (hoje cobre `/health`, `/auth/*` e o formato de erro) |
| [RegulaCheck.postman_collection.json](RegulaCheck.postman_collection.json) | Coleção Postman (health, login, sessão atual e logout) |

## Situação das rotas
- **Implementadas (RC-04):** `POST /auth/login`, `GET /auth/sessao` e `POST /auth/logout`; contrato em [specs/004-login-sessao/contracts/api-auth.md](../../specs/004-login-sessao/contracts/api-auth.md). Dependem do banco: sem `DB_*` configuradas respondem 503 `DEPENDENCIA_INDISPONIVEL`.
- **Implementada e verificada:** `GET /health` (200 `{"status":"UP"}`, só estado técnico; não consulta banco, logo UP não prova que o banco está acessível nem que o MVP está completo). Rota inexistente devolve 404 e método não permitido devolve 405, ambos no formato padrão de erro.
- **Mecanismo de autorização (RC-05):** registro único de rotas privadas com perfis, 403 por perfil, 404 por dona, chave do simulador só para a telemetria; verificado com rotas de exemplo existentes apenas em teste. As rotas de negócio ainda não existem e cada história futura deve cumprir a tabela de perfis de [CONTRATO_API.md](../../CONTRATO_API.md). Guia: [autorizacao.md](autorizacao.md).
- **Proteções de entrada e processamento (RC-06):** conferência de arquivo por tipo real, extensão, tamanho e páginas, área privada com nome interno gerado, download seguro, JSON com texto puro, contrato de conteúdo não confiável, varredura de segredos e recusa de `API_PUBLIC_URL` remota sem https; verificadas com arquivos e textos sintéticos em teste. As rotas de envio e download de negócio ainda não existem. Guia: [protecoes.md](protecoes.md).
- **Planejadas (contrato definido, sem implementação):** todas as demais rotas de [CONTRATO_API.md](../../CONTRATO_API.md).

Regras a respeitar: isolamento por dona da solicitação; upload até 3 arquivos, 5 MB cada, PDF até 5 páginas, JPEG/PNG, só material sintético; resultado por regra e geral conforme [protocolo-academico.md](../protocolo-academico.md); submissão idempotente e simulada; telemetria em JSON por HTTP pelo simulador Java; resumo de IA e extração mockados e identificados como simulados. Arquitetura: [../arquitetura.md](../arquitetura.md).
