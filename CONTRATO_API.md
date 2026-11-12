## Contrato mínimo de API proposto

Situação em 12/11/2026: `GET /health` e as três rotas `/auth/*` (RC-04, ver [contrato de autenticação](specs/004-login-sessao/contracts/api-auth.md)) estão implementadas, assim como o **mecanismo de autorização por perfil e por dona** (RC-05, ver [docs/api/autorizacao.md](docs/api/autorizacao.md)); as demais rotas abaixo estão **planejadas** e, quando forem implementadas, devem cumprir a tabela de perfis ao final deste arquivo. Formatos de dados, erros e estados: [docs/api](docs/api/README.md).

As rotas são uma proposta para manter um domínio só e orientar a implementação; ajustar nomes sem perder comportamentos.

| Método | Recurso                                               | Acesso e finalidade                        |
| ------ | ----------------------------------------------------- | ------------------------------------------ |
| POST   | /auth/login                                           | Autenticar conta demonstrativa (**implementada**, RC-04) |
| GET    | /auth/sessao                                          | Identidade e expiração da sessão atual (**implementada**, RC-04) |
| POST   | /auth/logout                                          | Encerrar a sessão do token enviado (**implementada**, RC-04) |
| GET    | /protocolos                                           | Paciente autenticada consulta opção ativa  |
| GET    | /solicitacoes                                         | Lista somente pedidos próprios             |
| POST   | /solicitacoes                                         | Cria rascunho                              |
| GET    | /solicitacoes/{id}                                    | Detalhes próprios                          |
| PUT    | /solicitacoes/{id}                                    | Atualiza rascunho permitido                |
| DELETE | /solicitacoes/{id}                                    | Exclui rascunho permitido                  |
| POST   | /solicitacoes/{id}/documentos                         | Upload multipart autenticado               |
| GET    | /solicitacoes/{id}/documentos/{docId}                 | Download protegido                         |
| DELETE | /solicitacoes/{id}/documentos/{docId}                 | Remove documento antes da submissão        |
| POST   | /solicitacoes/{id}/extracoes                          | Extrai arquivos da revisão atual           |
| PUT    | /solicitacoes/{id}/extracoes/{extracaoId}/conferencia | Registra conferência da transcrição        |
| POST   | /solicitacoes/{id}/analises                           | Executa regras e resumo de IA              |
| GET    | /solicitacoes/{id}/analises                           | Consulta resultados e histórico            |
| POST   | /solicitacoes/{id}/analises/{analiseId}/resumo        | Repete resumo em caso de falha             |
| POST   | /solicitacoes/{id}/submissoes-simuladas               | Conclui apenas no simulador                |
| POST   | /sessoes-captura/{id}/leituras                        | Credencial de simulador vinculada à sessão |
| GET    | /solicitacoes/{id}/leituras                           | Paciente consulta telemetria própria       |
| GET    | /admin/indicadores                                    | Administrador consulta agregados           |

## Perfil permitido por rota (RC-05)

| Rota | Perfil permitido | Dona verificada |
|---|---|---|
| `GET/POST /solicitacoes`, `GET/PUT/DELETE /solicitacoes/{id}` | PACIENTE | sim (a lista traz só as próprias) |
| `POST/GET/DELETE /solicitacoes/{id}/documentos[/{docId}]` | PACIENTE | sim (documento **e** solicitação) |
| `POST /solicitacoes/{id}/extracoes`, `PUT …/extracoes/{extracaoId}/conferencia` | PACIENTE | sim |
| `POST/GET /solicitacoes/{id}/analises`, `POST …/analises/{analiseId}/resumo` | PACIENTE | sim |
| `POST /solicitacoes/{id}/submissoes-simuladas` | PACIENTE | sim |
| `GET /solicitacoes/{id}/leituras` | PACIENTE | sim |
| `GET /protocolos` | PACIENTE e ADMINISTRADOR | não se aplica (protocolo acadêmico, sem dado de paciente) |
| `GET /admin/indicadores` | ADMINISTRADOR | não se aplica (somente agregados, sem dado identificável) |
| `POST /sessoes-captura/{id}/leituras` | simulador (cabeçalho `X-Chave-Simulador`) | sessão de captura existente e aberta |
| `GET /auth/sessao`, `POST /auth/logout` | PACIENTE e ADMINISTRADOR | a própria sessão |
| `GET /health`, `POST /auth/login` | públicas | não se aplica |

Regras comuns: 401 sem identidade válida; 403 perfil sem permissão; 404 recurso inexistente **ou** de outra paciente (resposta idêntica); 400 só depois dessas conferências. A paciente é sempre a da sessão, nunca um identificador enviado no corpo. O administrador **não** acessa documentos, análises, resumos nem submissões individuais por padrão. Detalhes e ordem de verificação: [docs/api/erros.md](docs/api/erros.md).
