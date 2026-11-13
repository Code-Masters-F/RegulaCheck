## Contrato mínimo de API proposto

Situação em 12/11/2026: `GET /health` e as três rotas `/auth/*` (RC-04, ver [contrato de autenticação](specs/004-login-sessao/contracts/api-auth.md)) estão implementadas, assim como o **mecanismo de autorização por perfil e por dona** (RC-05, ver [docs/api/autorizacao.md](docs/api/autorizacao.md)); as **proteções de entrada e de arquivos** (RC-06, ver [docs/api/protecoes.md](docs/api/protecoes.md): conferência por tipo real, extensão, tamanho e páginas, área privada, download seguro, texto puro, segredos e HTTPS) também estão implementadas como mecanismo; a **RC-07** implementou `GET /protocolos` e o rascunho da solicitação (`GET/POST /solicitacoes`, `GET/PUT /solicitacoes/{id}`; contrato em [specs/008-rascunho-selecionar-servico/contracts/api-solicitacoes.md](specs/008-rascunho-selecionar-servico/contracts/api-solicitacoes.md)); a **RC-08** implementou o envio, a lista e o download dos documentos do pedido (`POST/GET /solicitacoes/{id}/documentos`, `GET …/documentos/{docId}`; contrato em [specs/009-enviar-consultar-documentos/contracts/api-documentos.md](specs/009-enviar-consultar-documentos/contracts/api-documentos.md)); a **RC-09** implementou remover documento, substituir documento e excluir rascunho com confirmação (`DELETE …/documentos/{docId}`, `PUT …/documentos/{docId}`, `DELETE /solicitacoes/{id}`; contrato em [specs/010-substituir-excluir-documentos/contracts/api-alteracao-documentos.md](specs/010-substituir-excluir-documentos/contracts/api-alteracao-documentos.md)); a **RC-09** implementou remover documento, substituir documento e excluir rascunho com confirmação (`DELETE …/documentos/{docId}`, `PUT …/documentos/{docId}`, `DELETE /solicitacoes/{id}`; contrato em [specs/010-substituir-excluir-documentos/contracts/api-alteracao-documentos.md](specs/010-substituir-excluir-documentos/contracts/api-alteracao-documentos.md)); a **RC-09** implementou remover documento, substituir documento e excluir rascunho com confirmação (`DELETE …/documentos/{docId}`, `PUT …/documentos/{docId}`, `DELETE /solicitacoes/{id}`; contrato em [specs/010-substituir-excluir-documentos/contracts/api-alteracao-documentos.md](specs/010-substituir-excluir-documentos/contracts/api-alteracao-documentos.md)); a **RC-09** implementou remover documento, substituir documento e excluir rascunho com confirmação (`DELETE …/documentos/{docId}`, `PUT …/documentos/{docId}`, `DELETE /solicitacoes/{id}`; contrato em [specs/010-substituir-excluir-documentos/contracts/api-alteracao-documentos.md](specs/010-substituir-excluir-documentos/contracts/api-alteracao-documentos.md)); as demais rotas abaixo estão **planejadas** e, quando forem implementadas, devem cumprir a tabela de perfis ao final deste arquivo. Formatos de dados, erros e estados: [docs/api](docs/api/README.md).

As rotas são uma proposta para manter um domínio só e orientar a implementação; ajustar nomes sem perder comportamentos.

| Método | Recurso                                               | Acesso e finalidade                        |
| ------ | ----------------------------------------------------- | ------------------------------------------ |
| POST   | /auth/login                                           | Autenticar conta demonstrativa (**implementada**, RC-04) |
| GET    | /auth/sessao                                          | Identidade e expiração da sessão atual (**implementada**, RC-04) |
| POST   | /auth/logout                                          | Encerrar a sessão do token enviado (**implementada**, RC-04) |
| GET    | /protocolos                                           | Paciente autenticada consulta os serviços (protocolos) ativos (**implementada**, RC-07) |
| GET    | /solicitacoes                                         | Lista somente pedidos próprios, até 50, do mais recente ao mais antigo (**implementada**, RC-07) |
| POST   | /solicitacoes                                         | Cria rascunho ligado ao serviço escolhido (**implementada**, RC-07) |
| GET    | /solicitacoes/{id}                                    | Detalhes próprios (**implementada**, RC-07) |
| PUT    | /solicitacoes/{id}                                    | Troca o serviço de um rascunho; revisão +1 se mudou (**implementada**, RC-07) |
| DELETE | /solicitacoes/{id}                                    | Exclui o **rascunho** (só `RASCUNHO`) com o cabeçalho obrigatório `X-Confirmar-Exclusao: EXCLUIR`: apaga pedido, revisões, documentos e arquivos; 204; sem confirmação 400; submetido ou fora de rascunho 409 (**implementada**, RC-09) |
| POST   | /solicitacoes/{id}/documentos                         | Envia **um** arquivo (multipart: `arquivo` e `tipoDocumental`; cabeçalho opcional `X-Chave-Envio`) ao rascunho próprio: PDF até 5 páginas, JPEG/PNG, 5 MB, até 3 por pedido, conferido pelo conteúdo; revisão +1; não inicia análise (**implementada**, RC-08) |
| GET    | /solicitacoes/{id}/documentos                         | Lista os documentos disponíveis do pedido próprio, com estado do envio (**implementada**, RC-08) |
| GET    | /solicitacoes/{id}/documentos/{docId}                 | Download protegido (autenticado, só a dona e só pelo pedido ao qual o documento pertence, como anexo) (**implementada**, RC-08) |
| PUT    | /solicitacoes/{id}/documentos/{docId}                 | Substitui o documento por outro arquivo numa só operação (multipart como o envio; `X-Chave-Envio` opcional): 201 com o documento novo, 200 se repetição; recusa deixa o antigo intacto; uma revisão (`DOCUMENTO_SUBSTITUIDO`); não inicia análise (**implementada**, RC-09) |
| DELETE | /solicitacoes/{id}/documentos/{docId}                 | Remove o documento (libera a vaga e apaga o arquivo): 200 com `versaoConjuntoDocumental` e `documentosDisponiveis`; revisão `DOCUMENTO_REMOVIDO`; repetir é 404; pedido submetido em simulação é 409 (**implementada**, RC-09) |
| POST   | /solicitacoes/{id}/extracoes                          | Extrai arquivos da revisão atual (**prevista, não implementada**) |
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
| `GET/POST /solicitacoes`, `GET/PUT /solicitacoes/{id}` (RC-07) e `DELETE /solicitacoes/{id}` (RC-09), implementadas | PACIENTE | sim (a lista traz só as próprias) |
| `POST/GET /solicitacoes/{id}/documentos[/{docId}]` (RC-08) e `PUT/DELETE …/documentos/{docId}` (RC-09), implementadas | PACIENTE | sim (documento **e** solicitação) |
| `POST /solicitacoes/{id}/extracoes`, `PUT …/extracoes/{extracaoId}/conferencia` | PACIENTE | sim |
| `POST/GET /solicitacoes/{id}/analises`, `POST …/analises/{analiseId}/resumo` | PACIENTE | sim |
| `POST /solicitacoes/{id}/submissoes-simuladas` | PACIENTE | sim |
| `GET /solicitacoes/{id}/leituras` | PACIENTE | sim |
| `GET /protocolos` | PACIENTE | não se aplica (protocolo acadêmico, sem dado de paciente). Decisão da RC-07: o catálogo serve ao formulário da paciente; o administrador não o usa nesta fase e recebe 403 |
| `GET /admin/indicadores` | ADMINISTRADOR | não se aplica (somente agregados, sem dado identificável) |
| `POST /sessoes-captura/{id}/leituras` | simulador (cabeçalho `X-Chave-Simulador`) | sessão de captura existente e aberta |
| `GET /auth/sessao`, `POST /auth/logout` | PACIENTE e ADMINISTRADOR | a própria sessão |
| `GET /health`, `POST /auth/login` | públicas | não se aplica |

Regras comuns: 401 sem identidade válida; 403 perfil sem permissão; 404 recurso inexistente **ou** de outra paciente (resposta idêntica); 400 só depois dessas conferências. A paciente é sempre a da sessão, nunca um identificador enviado no corpo. O administrador **não** acessa documentos, análises, resumos nem submissões individuais por padrão. Detalhes e ordem de verificação: [docs/api/erros.md](docs/api/erros.md).
