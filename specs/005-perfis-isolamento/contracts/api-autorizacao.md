# Contrato: autorização por perfil e por dona (RC-05)

Complementa [`docs/api/erros.md`](../../../docs/api/erros.md). Valem para **toda** rota privada, atual ou futura. Formato de erro: o padrão do projeto (`codigo`, `mensagem`, `detalhes`, `idRastreamento`).

## Identidades aceitas

| Identidade | Como é enviada | Pode usar |
|---|---|---|
| Conta (paciente ou administrador) | `Authorization: Bearer <token>` da RC-04 | rotas privadas cujo perfil esteja declarado |
| Simulador | `X-Chave-Simulador: <IOT_API_KEY>` | somente a rota de telemetria |

Rotas **públicas**: `GET /health`, `POST /auth/login`. Todo o resto é privado.

## Ordem de verificação (sempre esta)

1. **Autenticação** → sem identidade válida: `401 NAO_AUTENTICADO`.
2. **Perfil permitido na rota** → identidade válida mas sem permissão: `403 ACESSO_NEGADO`.
3. **Dona do recurso** → recurso inexistente **ou** de outra paciente: `404 NAO_ENCONTRADO` (corpo idêntico).
4. **Validação do pedido** → `400 REQUISICAO_INVALIDA` (e 413/415 quando aplicável).
5. Regra de negócio e demais erros (409, 503…).

## Respostas

| Situação | Status | Código | Mensagem fixa |
|---|---|---|---|
| Sem token, token inválido/expirado, chave do simulador ausente ou errada na telemetria | 401 | `NAO_AUTENTICADO` | a da RC-04 |
| Perfil sem permissão na rota (inclui administrador em recurso individual e paciente em rota administrativa) | 403 | `ACESSO_NEGADO` | "Voce nao tem permissao para esta operacao." |
| Chave do simulador em rota de conta; token de conta na telemetria | 403 | `ACESSO_NEGADO` | idem |
| Recurso inexistente, de outra paciente, sessão de captura inexistente ou encerrada | 404 | `NAO_ENCONTRADO` | "Recurso nao encontrado." |
| Método HTTP não previsto numa rota privada | 405 | (padrão) | só depois de 401/403 |

Regras: a mensagem não muda conforme o motivo; nunca cita dona, perfil exigido, identificador interno, SQL ou nome de arquivo.

## Perfil permitido por rota (contrato `CONTRATO_API.md`)

| Rota | Perfil | Dona verificada |
|---|---|---|
| `GET/POST /solicitacoes`, `GET/PUT/DELETE /solicitacoes/{id}` | PACIENTE | sim (lista só as próprias) |
| `POST/GET/DELETE /solicitacoes/{id}/documentos[/{docId}]` | PACIENTE | sim (documento **e** solicitação) |
| `POST /solicitacoes/{id}/extracoes`, `PUT …/extracoes/{extracaoId}/conferencia` | PACIENTE | sim |
| `POST/GET /solicitacoes/{id}/analises`, `POST …/analises/{analiseId}/resumo` | PACIENTE | sim |
| `POST /solicitacoes/{id}/submissoes-simuladas` | PACIENTE | sim |
| `GET /solicitacoes/{id}/leituras` | PACIENTE | sim |
| `POST /sessoes-captura/{id}/leituras` | simulador (chave) | sessão de captura existente e aberta |
| Consulta de protocolo e indicadores agregados (RC-20) | ADMINISTRADOR | não se aplica; sem dado identificável |
| `GET /auth/sessao`, `POST /auth/logout` | PACIENTE, ADMINISTRADOR | a própria sessão |

Esta tabela é a fonte para `CONTRATO_API.md` e `openapi.yaml`; cada história de negócio a cumpre ao registrar sua rota.

## Garantias para o cliente

- Uma paciente nunca recebe dado de outra; o campo de dona nunca vem do corpo (se enviado, é ignorado ou recusado com 400 `REQUISICAO_INVALIDA`, conforme a rota).
- O aplicativo trata 401 como "voltar ao login", 403 como "sem permissão para esta tela" e 404 como "item não existe".
- Administrador não obtém documento, análise, resumo nem submissão individual por ter esse perfil.
