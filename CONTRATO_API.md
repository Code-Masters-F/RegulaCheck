# Contrato de API: mapa de rotas e perfis

Mapa resumido das rotas do RegulaCheck e do perfil que pode acessar cada uma. **Documentação interativa (Swagger UI, RC-28):** `GET /docs` com a API em execução; percurso do login ao comprovante em [docs/api/roteiro-avaliacao.md](docs/api/roteiro-avaliacao.md). **Fonte de verdade das respostas, dos códigos HTTP e dos formatos:** [docs/api/openapi.yaml](docs/api/openapi.yaml), [docs/api/contratos-dados.md](docs/api/contratos-dados.md) e [docs/api/erros.md](docs/api/erros.md). **Situação de cada rota e verificação por história (RC-XX):** [docs/api/README.md](docs/api/README.md#situação-das-rotas). Contratos por feature: `specs/*/contracts/`.

Dados exclusivamente sintéticos. Leitura de documentos, resumo de pendências e sensores são **simulados**.

## Rotas

Todas as rotas abaixo estão **implementadas**. A tabela é conferida automaticamente contra o `openapi.yaml` (`ContratoDaApiTest`, RC-28): os pares método + caminho precisam ser os mesmos.

| Método | Recurso | Finalidade | História |
| ------ | ------- | ---------- | -------- |
| GET | /health | Estado técnico (não consulta o banco) | Fundação |
| POST | /auth/login | Autenticar conta demonstrativa | RC-04 |
| GET | /auth/sessao | Identidade e expiração da sessão atual | RC-04 |
| POST | /auth/logout | Encerrar a sessão do token enviado | RC-04 |
| GET | /protocolos | Serviços (protocolos) ativos | RC-07 |
| GET | /solicitacoes | Pedidos próprios, até 50, do mais recente ao mais antigo | RC-07 |
| POST | /solicitacoes | Cria rascunho ligado ao serviço escolhido | RC-07 |
| GET | /solicitacoes/{id} | Detalhes próprios | RC-07 |
| PUT | /solicitacoes/{id} | Troca o serviço de um rascunho; revisão +1 se mudou | RC-07 |
| DELETE | /solicitacoes/{id} | Exclui o **rascunho** com `X-Confirmar-Exclusao: EXCLUIR`; 204; sem confirmação 400; fora de rascunho 409 | RC-09 |
| POST | /solicitacoes/{id}/documentos | Envia **um** arquivo ao rascunho (multipart `arquivo` e `tipoDocumental`; `X-Chave-Envio` opcional); limites em [protecoes.md](docs/api/protecoes.md); revisão +1; não inicia análise | RC-08 |
| GET | /solicitacoes/{id}/documentos | Lista os documentos do pedido, com estado do envio | RC-08 |
| GET | /solicitacoes/{id}/documentos/{docId} | Download protegido como anexo, só da dona e só pelo pedido do documento | RC-08 |
| PUT | /solicitacoes/{id}/documentos/{docId} | Substitui o documento numa só operação; 201 novo, 200 se repetição; recusa deixa o antigo intacto | RC-09 |
| DELETE | /solicitacoes/{id}/documentos/{docId} | Remove o documento (200 com `versaoConjuntoDocumental`); repetir é 404; pedido submetido é 409 | RC-09 |
| POST | /solicitacoes/{id}/extracoes | Lê (**simulada**) os documentos da versão atual e guarda o resultado; 201 nova, 200 se já existe; 422 `SEM_DOCUMENTOS`; 503 `LEITURA_INDISPONIVEL` | RC-10 |
| GET | /solicitacoes/{id}/extracoes | Leitura mais recente por documento (`IDENTIFICADO`, `NAO_LOCALIZADO`, `ILEGIVEL`), `ATUAL` ou `DESATUALIZADA` | RC-10 |
| GET | /solicitacoes/{id}/extracoes/{extracaoId}/conferencia | Conferência da leitura: campos com origem, destaque dos **não verificados**, decisões, orientações e situação; `liberadoParaSubmissao` sempre `false` | RC-11 |
| PUT | /solicitacoes/{id}/extracoes/{extracaoId}/conferencia | A paciente **só confirma ou marca divergência**; nenhum valor digitado é aceito (400); lote tudo ou nada; 409 se desatualizada | RC-11 |
| POST | /solicitacoes/{id}/analises | Executa e grava as regras da versão atual; 201 nova, 200 se vigente, 409 `ANALISE_EM_ANDAMENTO`, 503 `ANALISE_INDISPONIVEL` | RC-13 |
| GET | /solicitacoes/{id}/analises | Resultados e histórico, com a vigente marcada | RC-13 |
| POST | /solicitacoes/{id}/resumo | Resumo **simulado** da análise vigente; repete em caso de falha | RC-14 |
| POST | /solicitacoes/{id}/reenvio | Verifica a versão corrigida (leitura → conferência → análise → resumo) e compara com a anterior | RC-16 |
| POST | /solicitacoes/{id}/submissoes-simuladas | Conclui **só no simulador** o pedido com análise vigente sem pendências; 201 com comprovante `SIM-…`; 200 ao repetir; 409 se não estiver pronto | RC-17 |
| GET | /solicitacoes/{id}/submissoes-simuladas | Consulta o comprovante; 404 idêntico se não submetido ou de outra paciente | RC-17 |
| POST | /solicitacoes/{id}/sessoes-captura | Abre a sessão de captura (estação **simulada**); no máximo uma aberta por pedido; 409 se submetido | RC-21 |
| POST | /sessoes-captura/{id}/leituras | O **simulador** (`X-Chave-Simulador`) envia leitura de luminosidade (lux) ou distância (cm); 201; 404 sessão inexistente ou encerrada; 400 leitura inválida | RC-21 |
| GET | /solicitacoes/{id}/leituras | Telemetria própria: dois sensores com situação e orientação; não comprova legibilidade nem bloqueia envio | RC-22 |
| GET | /admin/indicadores | Administrador consulta protocolo ativo e indicadores agregados, sem dado identificável | RC-20 |
| GET | /docs | Documentação interativa (Swagger UI) servida pela API; pública, só GET | RC-28 |
| GET | /docs/openapi.yaml | Este contrato OpenAPI, idêntico a `docs/api/openapi.yaml` | RC-28 |

## Perfil permitido por rota (RC-05)

| Rota | Perfil permitido | Dona verificada |
|---|---|---|
| `GET/POST /solicitacoes`, `GET/PUT/DELETE /solicitacoes/{id}` | PACIENTE | sim (a lista traz só as próprias) |
| `POST/GET /solicitacoes/{id}/documentos[/{docId}]`, `PUT/DELETE …/documentos/{docId}` | PACIENTE | sim (documento **e** solicitação) |
| `POST/GET /solicitacoes/{id}/extracoes` e `GET/PUT …/extracoes/{extracaoId}/conferencia` | PACIENTE | sim (pedido **e** leitura) |
| `POST/GET /solicitacoes/{id}/analises`, `POST …/resumo`, `POST …/reenvio` | PACIENTE | sim |
| `POST/GET /solicitacoes/{id}/submissoes-simuladas` | PACIENTE | sim (administrador e chave do simulador recebem 403) |
| `POST /solicitacoes/{id}/sessoes-captura` e `GET /solicitacoes/{id}/leituras` | PACIENTE | sim (administrador e chave do simulador recebem 403) |
| `GET /protocolos` | PACIENTE | não se aplica (protocolo acadêmico, sem dado de paciente); o administrador recebe 403 (decisão da RC-07) |
| `GET /admin/indicadores` | ADMINISTRADOR | não se aplica (somente agregados, sem dado identificável) |
| `POST /sessoes-captura/{id}/leituras` | simulador (cabeçalho `X-Chave-Simulador`) | sessão de captura existente e aberta (conta de paciente ou administrador recebe 403) |
| `GET /auth/sessao`, `POST /auth/logout` | PACIENTE e ADMINISTRADOR | a própria sessão |
| `GET /health`, `POST /auth/login`, `GET /docs`, `GET /docs/openapi.yaml` | públicas | não se aplica (a documentação só expõe o contrato, sem dado de paciente) |

Regras comuns: 401 sem identidade válida; 403 perfil sem permissão; 404 recurso inexistente **ou** de outra paciente (resposta idêntica); 400 só depois dessas conferências. A paciente é sempre a da sessão, nunca um identificador enviado no corpo. O administrador **não** acessa documentos, análises, resumos nem submissões individuais por padrão. Detalhes e ordem de verificação: [docs/api/erros.md](docs/api/erros.md) e [docs/api/autorizacao.md](docs/api/autorizacao.md).
