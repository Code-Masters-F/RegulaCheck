# Data Model: Reenviar e conferir a versão corrigida (RC-16)

**Sem mudança de esquema.** Nenhuma tabela, coluna, constraint ou script SQL novo. Tudo deriva das tabelas das RC-08 a RC-14 (`RC_SOLICITACAO`, `RC_REVISAO`, `RC_DOCUMENTO`, `RC_EXTRACAO*`, `RC_DECISAO_CONFERENCIA`, `RC_ANALISE`, `RC_RESULTADO_REGRA`, `RC_PENDENCIA`, `RC_EVIDENCIA`).

## Comparação de análises (calculada)

`ComparacaoDeAnalises` — relação entre uma análise registrada (`atual`) e a registrada imediatamente antes dela no mesmo pedido (`anterior`).

| Campo | Tipo | Regra |
|---|---|---|
| `analiseAnteriorId` | long | id da análise anterior |
| `revisaoAnterior` | int | revisão avaliada pela anterior |
| `executadaEmAnterior` | Instant (UTC) | conclusão da anterior |
| `itens` | lista de `ItemComparado` | na ordem dos resultados da análise atual; só regras com pendência em pelo menos uma das duas |

`ItemComparado`:

| Campo | Tipo | Regra |
|---|---|---|
| `codigoRegra` | String | código na versão do protocolo |
| `descricaoRegra` | String | descrição na análise atual |
| `situacao` | enum `RESOLVIDA` \| `CONTINUA` \| `NOVA` | ver tabela abaixo |
| `resultadoAnterior` | `Resultado` | resultado da regra na anterior |
| `resultadoAtual` | `Resultado` | resultado da regra na atual |

| Anterior | Atual | Situação |
|---|---|---|
| `PENDENTE` ou `NAO_VERIFICAVEL` | `ATENDE` ou `NAO_APLICAVEL` | `RESOLVIDA` |
| `PENDENTE` ou `NAO_VERIFICAVEL` | `PENDENTE` ou `NAO_VERIFICAVEL` | `CONTINUA` |
| `ATENDE` ou `NAO_APLICAVEL` | `PENDENTE` ou `NAO_VERIFICAVEL` | `NOVA` |
| `ATENDE` ou `NAO_APLICAVEL` | `ATENDE` ou `NAO_APLICAVEL` | (não aparece) |

Invariantes:
- Sem análise anterior → sem comparação.
- Protocolo (identificador ou versão) diferente → sem comparação.
- Regra presente em só uma das análises não é comparada (não ocorre na mesma versão do protocolo: cada análise tem exatamente um resultado por regra, RC-13). Assim nenhuma regra aparece como "resolvida" sem ter sido avaliada no novo resultado.
- `RESOLVIDA` só existe quando `atual` é uma análise concluída e registrada; envio, substituição, remoção, leitura ou conferência não criam análise, então nunca criam `RESOLVIDA`.
- Calcular não altera nenhuma das duas análises.

## Verificação da versão corrigida (não gravada)

`ReenvioService.Resultado`:

| Campo | Tipo | Regra |
|---|---|---|
| `etapa` | enum `CONFERENCIA` \| `RESUMO` \| `CONCLUIDA` | `CONFERENCIA`: parou porque há campo identificado sem decisão (sem análise); `RESUMO`: análise registrada/vigente devolvida, mas o resumo não pôde ser gravado agora; `CONCLUIDA`: análise e resumo (gerado ou padrão identificado) disponíveis |
| `revisao` | int | revisão da leitura usada |
| `extracaoId` | long | leitura da revisão atual |
| `leituraCriada` | boolean | esta chamada gravou a leitura |
| `camposSemDecisao` | int | campos identificados ainda sem decisão (0 fora de `CONFERENCIA`) |
| `analise` | `Analise` ou nulo | nula só em `CONFERENCIA` |
| `analiseCriada` | boolean | esta chamada registrou a análise |
| `comparacao` | `ComparacaoDeAnalises` ou nulo | da `analise` com a anterior |

Transições (estado do pedido) — nenhuma nova; a verificação só usa as da RC-13:

| Etapa | Efeito no pedido |
|---|---|
| leitura | nenhum (RC-10) |
| conferência (`PENDENTE`) | nenhum; para |
| análise | `PROCESSANDO` → `COM_PENDENCIAS` \| `REQUER_CONFERENCIA` \| `SEM_PENDENCIAS_IDENTIFICADAS`; falha → `FALHA_ANALISE`; descarte → estado da última análise concluída (RC-13) |
| resumo | nenhum (RC-14) |

## Documento (RC-08/RC-09) — regra de estado alterada

| Operação | Antes da RC-16 | Depois da RC-16 |
|---|---|---|
| `POST …/documentos` (incluir) | só `RASCUNHO`; outros → 409 "Este pedido não pode mais receber documentos." | qualquer estado exceto `SUBMETIDA_SIMULACAO` (→ 409 "Este pedido já foi submetido e só pode ser consultado."); revisão + 1 (`DOCUMENTO_ENVIADO`), análise vigente deixa de valer na mesma transação, estado mantido |
| `PUT`/`DELETE …/documentos/{docId}` | qualquer estado exceto submetido (RC-09) | sem mudança |

Limite de 3 documentos disponíveis, conferência de arquivo (RC-06) e chave de tentativa (`X-Chave-Envio`) continuam iguais.
