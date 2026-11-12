# Contratos de dados: solicitação, upload, análise e pendência

Formato JSON, campos em camelCase, instantes em ISO-8601 UTC. Todos os exemplos são fictícios. A paciente é identificada pela sessão autenticada, nunca por campo no corpo. Rotas completas em [`CONTRATO_API.md`](../../../CONTRATO_API.md); apenas `GET /health` existe nesta história, as demais são planejadas.

## Solicitação
`POST /solicitacoes` (201) · `GET /solicitacoes/{id}` (200)

Entrada (criação): `{ "protocoloId": "PROTOCOLO-ACADEMICO-FICTICIO-v0.1" }`

Saída:
```json
{
  "id": "sol-0001",
  "protocoloId": "PROTOCOLO-ACADEMICO-FICTICIO-v0.1",
  "estado": "RASCUNHO",
  "revisao": 1,
  "criadaEm": "2026-11-12T12:00:00Z",
  "atualizadaEm": "2026-11-12T12:00:00Z",
  "simulacao": true
}
```
| Campo | Tipo | Obrigatório | Observação |
|---|---|---|---|
| `id` | string | sim (saída) | gerado pelo servidor |
| `protocoloId` | string | sim | protocolo ativo (fictício) |
| `estado` | enum | sim (saída) | ver [estados-e-revisoes.md](estados-e-revisoes.md) |
| `revisao` | inteiro ≥ 1 | sim (saída) | incrementa a cada alteração relevante |
| `simulacao` | booleano | sim (saída) | sempre `true`; indica envio apenas simulado |

## Upload de documento
`POST /solicitacoes/{id}/documentos` (201), multipart com um arquivo por requisição.

Limites: até 3 arquivos por solicitação, 5 MB cada, PDF até 5 páginas, JPEG ou PNG; tipo real verificado (não só a extensão). Erros: 400 (campo ausente), 413 (tamanho), 415 (tipo), 409 (limite de arquivos ou solicitação em estado que não aceita alteração).

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
| `resultadoGeral` | `COM_PENDENCIAS`, `REQUER_CONFERENCIA`, `SEM_PENDENCIAS_IDENTIFICADAS` (regras em [protocolo-academico.md](../../../docs/protocolo-academico.md)) |
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
