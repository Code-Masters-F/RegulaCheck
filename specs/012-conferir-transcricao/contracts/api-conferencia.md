# Contrato da API: conferência da transcrição (RC-11)

Rotas privadas, **somente perfil `PACIENTE`** (401 sem sessão, 403 para administrador e simulador). A paciente é sempre a da sessão. Dados sintéticos; **a leitura é simulada** e toda resposta traz `simulada: true` e um aviso fixo. A paciente **só confirma ou marca divergência**: não digita valor nem corrige a transcrição. A conferência **não muda o estado do pedido, não cria revisão, não gera resultado de regra e nunca libera submissão**.

## Ordem de verificação

401 → 403 → 404 (pedido ou leitura do pedido inexistente, alheio ou mal formado: resposta idêntica) → 415/400 (corpo, só no `PUT`) → 409 (pedido submetido; leitura desatualizada) → 404 (documento fora da leitura: mesma resposta) → 422 (decisão não aplicável ao campo) → 503. A existência do pedido e da leitura é conferida **antes** de ler o corpo.

## `GET /solicitacoes/{id}/extracoes/{extracaoId}/conferencia` — consultar

| Código | Quando | Corpo |
|---|---|---|
| 200 | existe a leitura do pedido | `ConferenciaResponse` (`situacao` `DESATUALIZADA` se a leitura envelheceu; somente leitura) |
| 401 / 403 | sem sessão / perfil indevido | erro padrão |
| 404 `NAO_ENCONTRADO` | pedido ou leitura inexistente, alheio, de outro pedido ou id mal formado | idêntico |
| 503 `DEPENDENCIA_INDISPONIVEL` | banco indisponível | sem detalhe técnico |

## `PUT /solicitacoes/{id}/extracoes/{extracaoId}/conferencia` — registrar decisões

`Content-Type: application/json`, até 4 KB, de 1 a 18 decisões; cada uma **só** com as chaves abaixo (qualquer outra, inclusive `valor`, é recusada):

```json
{ "decisoes": [
  { "documentoId": "41", "codigo": "NOME", "decisao": "CONFIRMADA" },
  { "documentoId": "41", "codigo": "DATA_EMISSAO", "decisao": "DIVERGENTE", "motivo": "VALOR_DIFERENTE_DO_DOCUMENTO" },
  { "documentoId": "41", "codigo": "AUTORIZACAO_ASSINADA", "decisao": "DIVERGENTE", "motivo": "CAMPO_EXISTE_E_NAO_FOI_LIDO" }
] }
```

| Campo | Regra |
|---|---|
| `documentoId` | texto decimal; deve ser de um documento da leitura |
| `codigo` | `TIPO_DOCUMENTO`, `NOME`, `DATA_NASCIMENTO`, `MOTIVO_ENCAMINHAMENTO`, `DATA_EMISSAO` ou `AUTORIZACAO_ASSINADA` |
| `decisao` | `CONFIRMADA` ou `DIVERGENTE` |
| `motivo` | obrigatório em `DIVERGENTE` e proibido em `CONFIRMADA`: `VALOR_DIFERENTE_DO_DOCUMENTO` (campo identificado) ou `CAMPO_EXISTE_E_NAO_FOI_LIDO` (campo não localizado) |

| Código | Quando | Corpo |
|---|---|---|
| 200 | todas as decisões registradas (decidir de novo substitui; repetir é no-op) | `ConferenciaResponse` já com a situação nova |
| 400 `REQUISICAO_INVALIDA` | corpo ausente/ilegível, `decisoes` vazia ou > 18, chave desconhecida (como `valor`), `decisao`/`motivo`/`codigo` fora da lista, motivo ausente em `DIVERGENTE` ou presente em `CONFIRMADA`, par repetido, `documentoId` mal formado | erro padrão com `detalhes`; **nada** é gravado |
| 401 / 403 | sem sessão / perfil indevido | erro padrão |
| 404 `NAO_ENCONTRADO` | pedido ou leitura inexistente/alheia/de outro pedido; documento fora da leitura | idêntico |
| 409 `CONFLITO_ESTADO` | pedido `SUBMETIDA_SIMULACAO` ("Este pedido já foi submetido e só pode ser consultado."); leitura desatualizada ("Os documentos mudaram; leia os documentos de novo.") | nada é gravado |
| 415 `TIPO_NAO_SUPORTADO` | corpo que não é `application/json` | |
| 422 `DECISAO_NAO_APLICAVEL` | decisão que não cabe à situação do campo (qualquer decisão em campo ilegível; confirmar campo não localizado; motivo errado para a situação) | detalhe com o campo; **nada** é gravado |
| 503 `DEPENDENCIA_INDISPONIVEL` | banco indisponível | sem detalhe técnico |

Garantias: o lote é **tudo ou nada**; uma só decisão vigente por campo; a leitura original (valores, situações, origens) e os documentos ficam intactos; sem revisão nova, sem mudança de estado do pedido.

## `ConferenciaResponse`

```json
{
  "extracaoId": "12",
  "situacao": "REQUER_CONFERENCIA",
  "liberadoParaSubmissao": false,
  "simulada": true,
  "aviso": "Leitura simulada com dados sintéticos; não é leitura real dos documentos.",
  "atualizadaEm": "2026-11-13T16:00:00.000Z",
  "resumo": { "camposNaoVerificados": 3, "camposSemDecisao": 2, "divergencias": 1 },
  "documentos": [
    { "documentoId": "41", "tipoDocumental": "ENCAMINHAMENTO", "situacao": "LIDO",
      "campos": [
        { "codigo": "NOME", "situacao": "IDENTIFICADO", "valor": "Maria Exemplo da Silva",
          "origem": { "pagina": 1, "trecho": "Paciente: Maria Exemplo da Silva" }, "naoVerificado": false,
          "decisao": { "tipo": "CONFIRMADA", "decididaEm": "2026-11-13T16:00:00.000Z" } },
        { "codigo": "AUTORIZACAO_ASSINADA", "situacao": "NAO_LOCALIZADO", "naoVerificado": true,
          "orientacao": "Esta informação não consta no documento lido. Se ela for necessária, peça um documento corrigido à unidade emissora. Não é possível preencher aqui." } ] },
    { "documentoId": "42", "tipoDocumental": "OUTRO", "situacao": "LEITURA_VAZIA", "reenviar": true,
      "motivo": "Não foi possível extrair informações deste arquivo.", "orientacao": "Envie um arquivo mais nítido.", "campos": [] } ]
}
```

- `situacao`: `PENDENTE`, `REQUER_CONFERENCIA`, `CONFERIDA` ou `DESATUALIZADA` (ver `data-model.md`).
- `naoVerificado` é `true` para campo ilegível, não localizado ou com divergência apontada (e todo documento não lido é destacado por `reenviar: true`). Campo ilegível e campo não localizado nunca trazem `valor` nem `origem`; um campo identificado com divergência apontada continua mostrando o que foi lido (a paciente não substitui o valor), marcado como não verificado.
- `atualizadaEm`: instante da decisão mais recente (ou da leitura, se não há decisão); serve à RC-12/RC-13 para saber se uma análise ficou velha frente a uma conferência nova.
- Nenhuma resposta traz caminho, hash, nome interno, resultado de regra, pendência nem estado novo do pedido; orientações não diagnosticam, não definem urgência nem prometem atendimento.

## Métodos

`/solicitacoes/{id}/extracoes/{extracaoId}/conferencia`: `GET, PUT` (`Allow`); demais → 405.
