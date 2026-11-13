## Contrato mínimo de API proposto

Situação em 12/11/2026: `GET /health` e as três rotas `/auth/*` (RC-04, ver [contrato de autenticação](specs/004-login-sessao/contracts/api-auth.md)) estão implementadas, assim como o **mecanismo de autorização por perfil e por dona** (RC-05, ver [docs/api/autorizacao.md](docs/api/autorizacao.md)); as **proteções de entrada e de arquivos** (RC-06, ver [docs/api/protecoes.md](docs/api/protecoes.md): conferência por tipo real, extensão, tamanho e páginas, área privada, download seguro, texto puro, segredos e HTTPS) também estão implementadas como mecanismo; a **RC-07** implementou `GET /protocolos` e o rascunho da solicitação (`GET/POST /solicitacoes`, `GET/PUT /solicitacoes/{id}`; contrato em [specs/008-rascunho-selecionar-servico/contracts/api-solicitacoes.md](specs/008-rascunho-selecionar-servico/contracts/api-solicitacoes.md)); a **RC-08** implementou o envio, a lista e o download dos documentos do pedido (`POST/GET /solicitacoes/{id}/documentos`, `GET …/documentos/{docId}`; contrato em [specs/009-enviar-consultar-documentos/contracts/api-documentos.md](specs/009-enviar-consultar-documentos/contracts/api-documentos.md)); a **RC-09** implementou remover documento, substituir documento e excluir rascunho com confirmação (`DELETE …/documentos/{docId}`, `PUT …/documentos/{docId}`, `DELETE /solicitacoes/{id}`; contrato em [specs/010-substituir-excluir-documentos/contracts/api-alteracao-documentos.md](specs/010-substituir-excluir-documentos/contracts/api-alteracao-documentos.md)); a **RC-10** implementou a leitura **simulada** dos documentos (`POST/GET /solicitacoes/{id}/extracoes`; contrato em [specs/011-extrair-informacoes-arquivos/contracts/api-extracao.md](specs/011-extrair-informacoes-arquivos/contracts/api-extracao.md)); a **RC-11** implementou a conferência da transcrição (`GET/PUT /solicitacoes/{id}/extracoes/{extracaoId}/conferencia`; contrato em [specs/012-conferir-transcricao/contracts/api-conferencia.md](specs/012-conferir-transcricao/contracts/api-conferencia.md)); as demais rotas abaixo estão **planejadas** e, quando forem implementadas, devem cumprir a tabela de perfis ao final deste arquivo. Formatos de dados, erros e estados: [docs/api](docs/api/README.md).

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
| POST   | /solicitacoes/{id}/extracoes                          | Lê (**leitura SIMULADA**, dados sintéticos) os documentos disponíveis da versão atual do pedido próprio e guarda o resultado: 201 nova, 200 se já existe para a versão; 422 `SEM_DOCUMENTOS`; 409 se submetido em simulação; 503 `LEITURA_INDISPONIVEL` (nada é gravado). Não muda o estado nem gera análise (**implementada**, RC-10) |
| GET    | /solicitacoes/{id}/extracoes                          | Devolve a leitura mais recente, por documento (campos `IDENTIFICADO`, `NAO_LOCALIZADO`, `ILEGIVEL`; motivo e reenvio quando não lido), `ATUAL` ou `DESATUALIZADA`; 404 se nunca houve leitura (**implementada**, RC-10) |
| GET    | /solicitacoes/{id}/extracoes/{extracaoId}/conferencia | Devolve a conferência da leitura: campos com origem, destaque dos **não verificados**, decisões da paciente, orientações fixas (sem diagnóstico, urgência ou promessa) e a situação calculada (`PENDENTE`, `REQUER_CONFERENCIA`, `CONFERIDA`, `DESATUALIZADA`); `liberadoParaSubmissao` é sempre `false`; leitura desatualizada só consulta; 404 idêntico se o pedido ou a leitura não for da paciente (**implementada**, RC-11) |
| PUT    | /solicitacoes/{id}/extracoes/{extracaoId}/conferencia | A paciente **só confirma ou marca divergência**, campo a campo (`{"decisoes":[{documentoId, codigo, decisao, motivo}]}`, 1 a 18; **nenhum valor digitado é aceito**: chave desconhecida é 400). Lote tudo ou nada; decidir de novo substitui; 409 se a leitura estiver desatualizada ou o pedido submetido; 422 `DECISAO_NAO_APLICAVEL`. Não muda o estado nem a revisão do pedido e nunca libera submissão (**implementada**, RC-11) |
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
| `POST/GET /solicitacoes/{id}/extracoes` (RC-10) e `GET/PUT …/extracoes/{extracaoId}/conferencia` (RC-11), implementadas | PACIENTE | sim (pedido **e** leitura) |
| `POST/GET /solicitacoes/{id}/analises`, `POST …/analises/{analiseId}/resumo` | PACIENTE | sim |
| `POST /solicitacoes/{id}/submissoes-simuladas` | PACIENTE | sim |
| `GET /solicitacoes/{id}/leituras` | PACIENTE | sim |
| `GET /protocolos` | PACIENTE | não se aplica (protocolo acadêmico, sem dado de paciente). Decisão da RC-07: o catálogo serve ao formulário da paciente; o administrador não o usa nesta fase e recebe 403 |
| `GET /admin/indicadores` | ADMINISTRADOR | não se aplica (somente agregados, sem dado identificável) |
| `POST /sessoes-captura/{id}/leituras` | simulador (cabeçalho `X-Chave-Simulador`) | sessão de captura existente e aberta |
| `GET /auth/sessao`, `POST /auth/logout` | PACIENTE e ADMINISTRADOR | a própria sessão |
| `GET /health`, `POST /auth/login` | públicas | não se aplica |

Regras comuns: 401 sem identidade válida; 403 perfil sem permissão; 404 recurso inexistente **ou** de outra paciente (resposta idêntica); 400 só depois dessas conferências. A paciente é sempre a da sessão, nunca um identificador enviado no corpo. O administrador **não** acessa documentos, análises, resumos nem submissões individuais por padrão. Detalhes e ordem de verificação: [docs/api/erros.md](docs/api/erros.md).
