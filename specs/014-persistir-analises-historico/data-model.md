# Data Model: Persistir análises e histórico por revisão (RC-13)

Alterações **aditivas** do esquema Oracle (script novo `database/scripts/10_analise_historico.sql` para bancos já criados; `01_criar_esquema.sql` atualizado para bancos novos). Nenhuma linha existente é alterada ou apagada; a única remoção é a restrição `RC_ANALISE_UK_REVISAO`, substituída por outra mais permissiva na mesma execução.

## Tabelas

### `RC_SOLICITACAO` (alterada)

| Coluna | Tipo | Regra |
|---|---|---|
| `ANALISE_INICIADA_EM` (nova) | `TIMESTAMP WITH TIME ZONE`, nula | Instante (UTC) em que a análise em andamento começou. Preenchida **se e somente se** `ESTADO = 'PROCESSANDO'` (`RC_SOLICITACAO_CK_ANALISE`). É o início, a base do limite de 1 minuto e o token de quem processa (research D1). |

### `RC_ANALISE` (alterada)

| Coluna | Tipo | Regra |
|---|---|---|
| `CONFERENCIA_EM` (nova) | `TIMESTAMP(6)` em UTC, nula | Instante da conferência considerada (`conferenciaAtualizadaEm` da RC-12). Sem fuso por causa da restrição única (research D6). Nula só nas linhas da carga sintética (`02_`). |
| `DATA_REFERENCIA` (nova) | `DATE`, nula | Data (Salvador) do envio mais recente entre os documentos avaliados, usada por R05/R06. Nula só na carga sintética. |
| `CRIADA_EM` (existente) | | Passa a ser lida como **data de execução** (instante da conclusão). |
| `VIGENTE` (existente) | | No máximo 1 por pedido (`RC_ANALISE_VIGENTE_UK`, existente). Só muda de 1 para 0. |

Restrições: **remove** `RC_ANALISE_UK_REVISAO (SOLICITACAO_ID, REVISAO)`; **acrescenta** `RC_ANALISE_UK_CONFERENCIA UNIQUE (SOLICITACAO_ID, REVISAO, CONFERENCIA_EM)`. A FK `RC_ANALISE_FK_REVISAO` continua.

### `RC_PENDENCIA` (alterada)

| Coluna | Tipo | Regra |
|---|---|---|
| `MOTIVO` (nova) | `VARCHAR2(40 CHAR)`, nula | Motivo do não verificável: `CAMPO_ILEGIVEL`, `DOCUMENTO_NAO_LIDO`, `TRANSCRICAO_NAO_CONFERIDA`, `DIVERGENCIA_APONTADA`, `VALOR_NAO_INTERPRETAVEL`, `CONDICAO_INDETERMINADA`, `DOCUMENTOS_INSUFICIENTES`, `DEPENDE_DE_DOCUMENTO_AUSENTE` (`RC_PENDENCIA_CK_MOTIVO`). `PENDENTE` nunca tem motivo (`RC_PENDENCIA_CK_MOTIVO_TIPO`). `NAO_VERIFICAVEL` gravado pela API sempre tem; a carga sintética não tem. |

### `RC_PENDENCIA_EVIDENCIA` (nova)

| Coluna | Tipo | Regra |
|---|---|---|
| `ANALISE_ID` | `NUMBER` | PK; FK composta `(ANALISE_ID, REGRA_ID)` → `RC_PENDENCIA (ANALISE_ID, REGRA_ID)` (`RC_PENDENCIA_UK_REGRA`) |
| `REGRA_ID` | `NUMBER` | PK |
| `ORDEM` | `NUMBER(3)` | PK; ordem da evidência na pendência (a do motor), `>= 1` |
| `DOCUMENTO_ID` | `NUMBER` | FK → `RC_DOCUMENTO` |
| `CODIGO` | `VARCHAR2(40 CHAR)` | Campo: `TIPO_DOCUMENTO`, `NOME`, `DATA_NASCIMENTO`, `MOTIVO_ENCAMINHAMENTO`, `DATA_EMISSAO`, `AUTORIZACAO_ASSINADA` |
| `SITUACAO` | `VARCHAR2(30 CHAR)` | `CONFIRMADO`, `NAO_LOCALIZADO`, `ILEGIVEL`, `NAO_CONFERIDO`, `DIVERGENCIA_APONTADA`, `DOCUMENTO_NAO_LIDO` |

Sem valor nem trecho: a origem (página, trecho) vem de `RC_EXTRACAO_CAMPO` da leitura da mesma revisão (`RC_EXTRACAO (SOLICITACAO_ID, REVISAO)` é única). Índice `RC_PEND_EVID_IX_DOCUMENTO (DOCUMENTO_ID)` para a FK.

## Modelo Java

### Gravação (`analise.repository`)

- **`AnaliseParaGravar`** `(int revisao, long protocoloId, long extracaoId, Instant conferenciaEm, LocalDate dataReferencia, AvaliacaoDeRegras avaliacao)`: montada pelo `AnaliseService` a partir do `AvaliacaoDoPedido` da RC-12.
- **`InicioAnalise`** (desfecho de `iniciar`): `NAO_ENCONTRADO`, `SUBMETIDO`, `EM_ANDAMENTO(desde)`, `VIGENTE(analise)`, `SEM_LEITURA_ATUAL`, `INICIADA(iniciadaEm, interrompeuAnterior)`.
- **`PublicacaoAnalise`** (desfecho de `publicar`): `PUBLICADA(analise)`, `ENTRADA_MUDOU`, `SUBSTITUIDA`, `PRAZO_ESGOTADO` (passou do limite sem ninguém assumir: não publica, pedido em `FALHA_ANALISE`), `NAO_ENCONTRADO`.
- **`Encerramento`**: `FALHA` (→ `FALHA_ANALISE`) ou `DESCARTE` (→ estado de D4).

### Leitura (`analise.entity`)

- **`Analise`** `(long id, int revisao, boolean vigente, Instant executadaEm, String protocoloIdentificador, String protocoloVersao, Instant conferenciaEm /*nulo na carga*/, LocalDate dataReferencia /*nulo na carga*/, ResultadoGeral resultadoGeral, List<DocumentoDaAnalise> documentos, List<ResultadoGravado> resultados, List<PendenciaGravada> pendencias)`.
- **`DocumentoDaAnalise`** `(long documentoId, String nomeArquivo, TipoDocumental tipoDeclarado, boolean disponivel)`: documentos da revisão avaliada (`REVISAO_ENVIO <= revisao` e não removidos até ela); `disponivel` = ainda não removido/substituído.
- **`ResultadoGravado`** `(String codigoRegra, String descricaoRegra, Resultado resultado, MotivoNaoVerificavel motivo /*nulo salvo NAO_VERIFICAVEL com pendência gravada pela API*/)`, na ordem da versão.
- **`PendenciaGravada`** `(String codigoRegra, String descricaoRegra, Resultado tipo, String explicacao, MotivoNaoVerificavel motivo, ResponsavelCorrecao responsavel, List<EvidenciaGravada> evidencias)`.
- **`EvidenciaGravada`** `(long documentoId, CodigoCampo codigo, SituacaoEvidencia situacao, Integer pagina, String trecho)`; `toString` sem trecho.
- **`HistoricoDeAnalises`** `(EstadoSolicitacao estado, int revisaoAtual, Instant analiseIniciadaEm /*nulo fora de PROCESSANDO*/, List<Analise> analises)`: da mais recente para a mais antiga, até o limite.

### Pedido (`solicitacao.entity.Solicitacao`, alterada)

- Novo componente `ResultadoGeral resultadoVigente`: resultado geral da análise vigente **da revisão atual**; `null` quando não há. Usado só para `analise.situacao` (research D8).

## Estados

```text
RASCUNHO | REQUER_CONFERENCIA | COM_PENDENCIAS | SEM_PENDENCIAS_IDENTIFICADAS | FALHA_ANALISE
   │  pedir análise, sem vigente da revisão atual, com leitura atual, sem outra em andamento no prazo
   ▼
PROCESSANDO (ANALISE_INICIADA_EM = início)
   ├── publicar ok ──────────────────────▶ COM_PENDENCIAS | REQUER_CONFERENCIA | SEM_PENDENCIAS_IDENTIFICADAS (nova vigente)
   ├── entrada mudou (revisão/conferência) ▶ resultado da análise concluída mais recente, ou RASCUNHO
   ├── falha da avaliação ────────────────▶ FALHA_ANALISE
   ├── resultado depois do limite ────────▶ FALHA_ANALISE (resultado tardio não publicado)
   └── passou de 1 minuto ────────────────▶ (continua PROCESSANDO; o próximo pedido assume com novo início)
SUBMETIDA_SIMULACAO: pedir análise → 409; consultar → 200
```

Invariantes: `PROCESSANDO ⇔ ANALISE_INICIADA_EM não nula`; no máximo uma análise vigente por pedido; a vigente, se existe, é da revisão atual e da conferência atual (documentos e conferência tiram a vigência na mesma transação em que mudam); análise gravada nunca é alterada além de `VIGENTE` 1 → 0.

## Validações

- `AnaliseParaGravar`: um resultado por regra da versão (o motor já garante); a gravação resolve `REGRA_ID` por `(PROTOCOLO_ID, CODIGO)` e falha por inteiro (rollback) se algum código não existir na versão.
- Leitura: valores de enum desconhecidos no banco são erro de programação (não são escondidos).
