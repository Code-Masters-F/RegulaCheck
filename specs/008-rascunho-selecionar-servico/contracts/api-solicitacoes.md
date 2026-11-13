# Contrato da API: serviços e solicitações em rascunho (RC-07)

Formato JSON, campos em camelCase, instantes ISO-8601 UTC. Todas as rotas exigem sessão (`Authorization: Bearer <token>`, RC-04) e perfil `PACIENTE`. A paciente é sempre a da sessão. Dados fictícios. Este arquivo é a fonte; ao implementar, `CONTRATO_API.md`, `docs/api/contratos-dados.md`, `erros.md`, `estados-e-revisoes.md` e `openapi.yaml` são atualizados a partir dele.

## Ordem de verificação (igual a `docs/api/erros.md`)

401 sem sessão válida → 403 perfil não permitido (administrador, simulador) → 404 pedido inexistente, alheio ou id inválido → 400/415 corpo → 422/409 regra e estado → 503 banco.

## GET /protocolos — serviços disponíveis

200:
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
Só protocolos ativos. Sem nenhum: `{ "itens": [] }` com 200. Erros: 401, 403, 503.

## POST /solicitacoes — criar rascunho

Entrada: `{ "protocoloId": "PROTOCOLO-ACADEMICO-FICTICIO-v0.1" }` (`Content-Type: application/json`).

201 (cabeçalho `Location: /solicitacoes/12`):
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
Campos extras no corpo (`pacienteId`, `estado`, `revisao`) são ignorados. Cada chamada bem-sucedida cria um pedido novo (sem deduplicação).

| HTTP | Código | Quando |
|---|---|---|
| 400 | `REQUISICAO_INVALIDA` | corpo vazio, grande, ilegível, `protocoloId` ausente, em branco, de tipo errado ou com mais de 100 caracteres (`detalhes[0].campo = "protocoloId"`) |
| 401 / 403 | `NAO_AUTENTICADO` / `ACESSO_NEGADO` | sem sessão / administrador ou simulador |
| 415 | `TIPO_NAO_SUPORTADO` | `Content-Type` diferente de `application/json` |
| 422 | `REGRA_DE_NEGOCIO` | protocolo inexistente ou inativo (`detalhes[0].campo = "protocoloId"`, "Escolha um dos serviços da lista.") |
| 503 | `DEPENDENCIA_INDISPONIVEL` | banco fora |

## GET /solicitacoes — meus pedidos

200: `{ "itens": [ <SolicitacaoResponse>, … ], "limite": 50 }`, só da paciente da sessão, do mais recente para o mais antigo (`criadaEm` desc, `id` desc), no máximo 50. Sem pedidos: `itens` vazio, 200. Erros: 401, 403, 503.

## GET /solicitacoes/{id} — reabrir

200: `SolicitacaoResponse` (igual ao da criação). 404 `NAO_ENCONTRADO` (mensagem fixa "Recurso nao encontrado.") para pedido inexistente, de outra paciente, id fora de `^[1-9][0-9]{0,17}$` ou subcaminho desconhecido. Erros: 401, 403, 503.

## PUT /solicitacoes/{id} — trocar o serviço do rascunho

Entrada: `{ "protocoloId": "..." }`. Só enquanto `estado = RASCUNHO`.

200: `SolicitacaoResponse` atualizado.
- Serviço diferente do atual: `revisao` +1, `atualizadaEm` = agora, revisão registrada com motivo `PROTOCOLO_ALTERADO`.
- Mesmo serviço: 200 com os dados atuais; `revisao` e `atualizadaEm` não mudam.

| HTTP | Código | Quando |
|---|---|---|
| 404 | `NAO_ENCONTRADO` | pedido inexistente, alheio ou id inválido (**antes** de validar o corpo) |
| 400 / 415 | `REQUISICAO_INVALIDA` / `TIPO_NAO_SUPORTADO` | mesmas regras do POST |
| 409 | `CONFLITO_ESTADO` | o pedido já saiu de `RASCUNHO` ("Este pedido não pode mais ser alterado.") |
| 422 | `REGRA_DE_NEGOCIO` | protocolo inexistente ou inativo |
| 401 / 403 / 503 | — | como acima |

Concorrência: PUTs simultâneos no mesmo rascunho são serializados; a sequência de revisões não tem salto nem repetição.

## Fora deste contrato

`DELETE /solicitacoes/{id}`, `/solicitacoes/{id}/documentos`, extração, análises e submissão continuam planejados em `CONTRATO_API.md` e respondem 404 padrão até suas histórias. Todos os métodos não listados em cada rota respondem 405 `METODO_NAO_PERMITIDO` (depois de 401/403).
