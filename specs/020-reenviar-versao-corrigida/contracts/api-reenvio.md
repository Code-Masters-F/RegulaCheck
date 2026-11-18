# API: reenvio e verificação da versão corrigida (RC-16)

Todas as rotas abaixo são privadas de `PACIENTE` (401 sem sessão, 403 para administrador e simulador, 404 idêntico para pedido inexistente ou de outra paciente — ordem de `docs/api/erros.md`). Leitura e resumo são **simulados** e o protocolo é **acadêmico e fictício**; nenhuma resposta traz valor lido de documento.

## 1. `POST /solicitacoes/{id}/reenvio` — verificar a versão corrigida

Sem corpo (corpo enviado é ignorado). Executa, para a revisão atual: leitura (reaproveita a existente) → conferência → análise (RC-13) → resumo (RC-14).

### 200 — parou na conferência (nenhuma análise registrada)

```json
{
  "solicitacaoId": "42",
  "etapa": "CONFERENCIA",
  "revisao": 6,
  "extracaoId": "77",
  "leituraCriada": true,
  "camposSemDecisao": 4,
  "analiseCriada": false,
  "simulada": true,
  "orientacao": "Confira a leitura dos documentos da revisão 6 antes do novo resultado. Nenhuma pendência foi marcada como resolvida."
}
```

`analise` é omitida. Conferir pela RC-11 (`PUT …/extracoes/{extracaoId}/conferencia`) e chamar de novo.

### 201 — nova análise registrada · 200 — análise vigente já existente

```json
{
  "solicitacaoId": "42",
  "etapa": "CONCLUIDA",
  "revisao": 6,
  "extracaoId": "77",
  "leituraCriada": false,
  "camposSemDecisao": 0,
  "analiseCriada": true,
  "simulada": true,
  "orientacao": "Nova verificação concluída. Compare com o resultado anterior. \"Sem pendências identificadas\" não é aprovação do SUS.",
  "analise": { "...": "AnaliseResponse (RC-13/14/15) com 'comparacao' (seção 2)" }
}
```

- `etapa = "RESUMO"`: a análise foi registrada (201) ou já existia (200), mas o resumo não pôde ser gravado agora (análise deixou de ser vigente durante a geração ou banco indisponível). `analise` vem sem `resumo`; `orientacao` pede para gerar o resumo de novo (`POST …/resumo`) ou verificar de novo se os documentos mudaram. A análise **não** é desfeita.
- Falha ou saída inválida do gerador simulado **não** é `RESUMO`: segue a RC-14 (resumo `origem: "PADRAO"`, identificado, com nova tentativa possível) e a etapa é `CONCLUIDA`.
- Repetir sem mudança: 200, mesma análise (`analiseCriada: false`), nenhum resumo novo (resumo `ADAPTADOR_MOCKADO` existente é devolvido como está).

### Erros

Mantêm `status` e `codigo` da etapa que falhou; `mensagem` diz o que foi preservado e o que repetir; `detalhes` identifica a etapa:

```json
{
  "codigo": "LEITURA_INDISPONIVEL",
  "mensagem": "A leitura da versão corrigida falhou. Seus documentos foram mantidos e o último resultado concluído continua no histórico. Peça a verificação de novo; não é preciso reenviar o arquivo.",
  "detalhes": [ { "campo": "etapa", "mensagem": "LEITURA" } ]
}
```

| HTTP | Código | Etapa | Quando / o que repetir |
|---|---|---|---|
| 404 | `NAO_ENCONTRADO` | — | pedido inexistente, de outra paciente ou id ilegível (corpo idêntico, sem detalhe) |
| 409 | `CONFLITO_ESTADO` | — | pedido submetido: "Este pedido já foi submetido e só pode ser consultado." |
| 422 | `SEM_DOCUMENTOS` | `LEITURA` | pedido sem documento disponível: enviar o documento corrigido e verificar de novo |
| 503 | `LEITURA_INDISPONIVEL` | `LEITURA` | leitor simulado falhou: verificar de novo, sem reenviar arquivo |
| 409 | `CONFLITO_ESTADO` | `LEITURA` | documentos mudaram durante a leitura: verificar de novo |
| 409 | `ANALISE_EM_ANDAMENTO` | `ANALISE` | outra análise do pedido em andamento; `Retry-After` com os segundos restantes |
| 409 | `CONFLITO_ESTADO` | `ANALISE` | documentos ou conferência mudaram durante a análise, ou ela passou do limite: nada publicado; verificar de novo |
| 503 | `ANALISE_INDISPONIVEL` | `ANALISE` | avaliação falhou; pedido em `FALHA_ANALISE`; verificar de novo |
| 503 | `DEPENDENCIA_INDISPONIVEL` | `LEITURA`, `CONFERENCIA` ou `ANALISE` | banco ou arquivos indisponíveis na etapa indicada: verificar de novo mais tarde |

Em todos os casos: a última análise concluída fica intacta no histórico, nenhum resultado parcial é gravado e os documentos enviados são mantidos.

## 2. Campo `comparacao` em `AnaliseResponse`

Aditivo; omitido na primeira análise do pedido e entre versões de protocolo diferentes. Presente em `POST …/analises`, `GET …/analises` (cada análise comparada com a imediatamente anterior), `POST …/resumo` e `POST …/reenvio`.

```json
"comparacao": {
  "analiseAnteriorId": "12",
  "revisaoAnterior": 4,
  "executadaEmAnterior": "2026-11-17T13:05:10.123Z",
  "itens": [
    { "codigoRegra": "RC-R04", "descricaoRegra": "Motivo do encaminhamento informado",
      "situacao": "RESOLVIDA", "resultadoAnterior": "PENDENTE", "resultadoAtual": "ATENDE" },
    { "codigoRegra": "RC-R02", "descricaoRegra": "Identificação coerente entre documentos",
      "situacao": "CONTINUA", "resultadoAnterior": "NAO_VERIFICAVEL", "resultadoAtual": "PENDENTE" }
  ]
}
```

`situacao`: `RESOLVIDA` | `CONTINUA` | `NOVA` (regra da [data-model](../data-model.md)). Regras sem pendência nas duas análises não aparecem; `itens` pode ser vazio. "Resolvida" é só a leitura da regra no novo resultado simulado; **não** é aprovação do SUS.

## 3. `POST /solicitacoes/{id}/documentos` — inclusão após a análise

Mesmo contrato da RC-08 (multipart, `tipoDocumental`, `X-Chave-Envio`, 201/200, 400/413/415, 409 `CHAVE_ENVIO_REUTILIZADA`, 422 limite de 3), com a regra de estado da RC-09:

- Aceito em `RASCUNHO`, `PROCESSANDO`, `REQUER_CONFERENCIA`, `COM_PENDENCIAS`, `SEM_PENDENCIAS_IDENTIFICADAS` e `FALHA_ANALISE`: revisão + 1 (`DOCUMENTO_ENVIADO`) e, na mesma transação, a análise vigente deixa de valer (fica no histórico); o estado do pedido **não** muda; nada é lido nem analisado.
- `SUBMETIDA_SIMULACAO` → 409 `CONFLITO_ESTADO` "Este pedido já foi submetido e só pode ser consultado.", sem gravar arquivo.
- Arquivo recusado (RC-06): nada muda — documentos, revisão e análise vigente continuam iguais.
