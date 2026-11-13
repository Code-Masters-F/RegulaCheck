# Contratos de dados: solicitação, upload, análise e pendência

Formato JSON, campos em camelCase, instantes em ISO-8601 UTC. Todos os exemplos são fictícios. A paciente é identificada pela sessão autenticada, nunca por campo no corpo. Rotas completas em [`CONTRATO_API.md`](../../CONTRATO_API.md); além de `GET /health`, estão implementadas as rotas `/auth/*` (RC-04), `GET /protocolos` e o rascunho da solicitação (`GET/POST /solicitacoes`, `GET/PUT /solicitacoes/{id}`, RC-07); as demais são planejadas. Fonte detalhada da RC-07: [specs/008-rascunho-selecionar-servico/contracts/api-solicitacoes.md](../../specs/008-rascunho-selecionar-servico/contracts/api-solicitacoes.md).

## Serviços disponíveis (RC-07)
`GET /protocolos` (200), só `PACIENTE`. Lista os serviços ativos; no MVP existe um, o protocolo acadêmico fictício.

```json
{
  "itens": [
    {
      "protocoloId": "PROTOCOLO-ACADEMICO-FICTICIO-v0.1",
      "identificador": "PROTOCOLO-ACADEMICO-FICTICIO",
      "versao": "v0.1",
      "localidade": "Município Exemplo",
      "especialidade": "Consulta ambulatorial em cardiologia",
      "avisoFicticio": "Documento acadêmico fictício. Não é exigência oficial de nenhum serviço de saúde ou do SUS.",
      "simulacao": true
    }
  ]
}
```
Sem nenhum serviço ativo a resposta é `{ "itens": [] }` (200). `protocoloId` é `identificador` + `-` + `versao`; o identificador interno do banco não sai pela API.

## Solicitação
`POST /solicitacoes` (201, com `Location: /solicitacoes/{id}`) · `GET /solicitacoes` (200) · `GET /solicitacoes/{id}` (200) · `PUT /solicitacoes/{id}` (200). Todas só `PACIENTE`; a paciente é sempre a da sessão.

Entrada (criação e troca de serviço): `{ "protocoloId": "PROTOCOLO-ACADEMICO-FICTICIO-v0.1" }`. Outros campos enviados (dono, estado, revisão) são ignorados e nunca lidos. `protocoloId` deve ser texto JSON, não vazio e com até 100 caracteres.

Saída:
```json
{
  "id": "12",
  "protocoloId": "PROTOCOLO-ACADEMICO-FICTICIO-v0.1",
  "servico": {
    "localidade": "Município Exemplo",
    "especialidade": "Consulta ambulatorial em cardiologia",
    "avisoFicticio": "Documento acadêmico fictício. Não é exigência oficial de nenhum serviço de saúde ou do SUS."
  },
  "estado": "RASCUNHO",
  "revisao": 1,
  "criadaEm": "2026-11-13T12:00:00Z",
  "atualizadaEm": "2026-11-13T12:00:00Z",
  "simulacao": true
}
```
| Campo | Tipo | Obrigatório | Observação |
|---|---|---|---|
| `id` | string | sim (saída) | número decimal opaco gerado pelo servidor (ex.: `"12"`); id fora de `^[1-9][0-9]{0,17}$` responde 404 |
| `protocoloId` | string | sim | serviço ativo (fictício); inexistente ou inativo → 422 |
| `servico` | objeto | sim (saída) | localidade, especialidade e aviso de protocolo fictício |
| `estado` | enum | sim (saída) | ver [estados-e-revisoes.md](estados-e-revisoes.md); na RC-07 só `RASCUNHO` é produzido |
| `revisao` | inteiro ≥ 1 | sim (saída) | incrementa a cada alteração relevante (na RC-07: trocar para outro serviço) |
| `criadaEm`, `atualizadaEm` | string | sim (saída) | ISO-8601 UTC, em milissegundos |
| `simulacao` | booleano | sim (saída) | sempre `true`; indica envio apenas simulado |

- `GET /solicitacoes`: `{ "itens": [ … ], "limite": 50 }`, só os pedidos da paciente, do mais recente ao mais antigo (`criadaEm` e depois `id`, decrescente), no máximo 50; vazio é 200 com `itens` vazio.
- `PUT /solicitacoes/{id}`: só em `RASCUNHO`. Serviço diferente → `revisao` +1, `atualizadaEm` = agora e revisão registrada com o motivo `PROTOCOLO_ALTERADO`; mesmo serviço → 200 sem mudar `revisao` nem `atualizadaEm`. Fora de rascunho → 409 `CONFLITO_ESTADO`. A dona é conferida antes do corpo (404 antes de 400). Alterações simultâneas do mesmo rascunho são serializadas no banco (`SELECT … FOR UPDATE`).
- Cada `POST` bem-sucedido cria um pedido novo: o servidor **não** deduplica toques duplos; a proteção é do cliente.
- Erros: 400 `REQUISICAO_INVALIDA` (corpo ilegível/grande, `protocoloId` ausente, nulo, em branco, não textual ou longo, com `detalhes[].campo = "protocoloId"`), 401, 403, 404, 405, 409, 415, 422 `REGRA_DE_NEGOCIO` (serviço inexistente/inativo, mesma resposta), 503. Sem dado clínico: o rascunho guarda só o serviço escolhido.

## Upload de documento
`POST /solicitacoes/{id}/documentos` (201), multipart com um arquivo por requisição.

Limites: até 3 arquivos por solicitação, 5 MB cada, PDF até 5 páginas, JPEG ou PNG; tipo real verificado pelo conteúdo (não só a extensão). Erros: 400 (campo ausente, PDF com mais de 5 páginas, PDF protegido por senha, arquivo ilegível ou mais de 3 arquivos), 413 (tamanho), 415 (tipo ou extensão incompatível), 409 (solicitação em estado que não aceita alteração). O arquivo fica em área privada com nome interno gerado e o download exige autenticação (RC-06; ver [protecoes.md](protecoes.md)).

Saída:
```json
{ "id": "doc-0001", "nomeOriginal": "encaminhamento-sintetico.pdf", "tipo": "PDF", "tamanhoBytes": 120400, "paginas": 1, "revisao": 2 }
```
Enviar, substituir ou remover arquivo incrementa `revisao` da solicitação e invalida a análise vigente.

## Análise
`POST /solicitacoes/{id}/analises` (202 se assíncrona; 200 se concluída na chamada) · `GET /solicitacoes/{id}/analises` (200)

```json
{
  "id": "ana-0001",
  "revisao": 2,
  "versaoProtocolo": "v0.1",
  "resultadoGeral": "COM_PENDENCIAS",
  "vigente": true,
  "simulado": true,
  "resultados": [
    { "codigoRegra": "RC-R01", "resultado": "ATENDE" },
    { "codigoRegra": "RC-R04", "resultado": "PENDENTE" },
    { "codigoRegra": "RC-R06", "resultado": "NAO_APLICAVEL" }
  ],
  "pendencias": [ /* ver Pendência */ ],
  "resumo": { "texto": "…", "simulado": true, "origem": "ADAPTADOR_MOCKADO" }
}
```
| Campo | Valores |
|---|---|
| `resultadoGeral` | `COM_PENDENCIAS`, `REQUER_CONFERENCIA`, `SEM_PENDENCIAS_IDENTIFICADAS` (regras em [protocolo-academico.md](../protocolo-academico.md)) |
| `resultados[].resultado` | `ATENDE`, `PENDENTE`, `NAO_VERIFICAVEL`, `NAO_APLICAVEL` |
| `simulado` / `resumo.simulado` | sempre `true` enquanto IA e extração forem mockadas; a interface deve mostrar a identificação |

O resultado só é `vigente` se `revisao` da análise for igual à revisão atual da solicitação. `SEM_PENDENCIAS_IDENTIFICADAS` não é aprovação do SUS.

## Pendência
```json
{
  "codigoRegra": "RC-R04",
  "resultado": "PENDENTE",
  "mensagem": "O encaminhamento não traz o motivo. Peça à unidade emissora que complete o documento.",
  "responsavelCorrecao": "UNIDADE_EMISSORA"
}
```
`codigoRegra` deve existir no protocolo vigente; `responsavelCorrecao` ∈ {`PACIENTE`, `UNIDADE_EMISSORA`}. Mensagens não diagnosticam, não definem urgência, não autorizam atendimento nem prometem vaga.

## Submissão simulada
`POST /solicitacoes/{id}/submissoes-simuladas` (201): só a partir de `SEM_PENDENCIAS_IDENTIFICADAS` vigente; repetir devolve a mesma submissão (idempotente). Resposta traz referência interna, data e o aviso `"SIMULAÇÃO ACADÊMICA — NÃO ENVIADO AO SUS"`.
