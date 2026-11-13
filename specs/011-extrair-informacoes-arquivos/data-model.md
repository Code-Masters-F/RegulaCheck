# Data Model: Extrair informações dos arquivos (RC-10)

## Mudança de esquema

**Script aditivo `database/scripts/08_extracao.sql`** (só `CREATE TABLE`/`CREATE INDEX`, prefixo `RC_`); o `01_criar_esquema.sql` também passa a criá-las (banco novo). Aplicar o `08_` no Oracle compartilhado exige autorização específica. Sem o `08_`, só a rota de extração falha (503); o resto da API segue.

```sql
RC_EXTRACAO (
  ID              NUMBER GENERATED ALWAYS AS IDENTITY  -- PK
  SOLICITACAO_ID  NUMBER NOT NULL                      -- FK composta (SOLICITACAO_ID, REVISAO) -> RC_REVISAO
  REVISAO         NUMBER(6) NOT NULL                   -- versão do conjunto documental lida
  SIMULADA        NUMBER(1) DEFAULT 1 NOT NULL         -- CHECK (SIMULADA = 1)
  CRIADA_EM       TIMESTAMP WITH TIME ZONE ... NOT NULL
  UK (SOLICITACAO_ID, REVISAO)                         -- uma leitura por versão
)

RC_EXTRACAO_DOCUMENTO (
  ID              NUMBER GENERATED ALWAYS AS IDENTITY  -- PK
  EXTRACAO_ID     NUMBER NOT NULL                      -- FK -> RC_EXTRACAO
  DOCUMENTO_ID    NUMBER NOT NULL                      -- FK -> RC_DOCUMENTO
  SITUACAO        VARCHAR2(20 CHAR) NOT NULL           -- CHECK IN ('LIDO','PROTEGIDO','LEITURA_VAZIA')
  UK (EXTRACAO_ID, DOCUMENTO_ID)
)

RC_EXTRACAO_CAMPO (
  EXTRACAO_DOCUMENTO_ID  NUMBER NOT NULL               -- FK -> RC_EXTRACAO_DOCUMENTO
  CODIGO          VARCHAR2(40 CHAR) NOT NULL           -- CHECK IN (6 códigos)
  SITUACAO        VARCHAR2(20 CHAR) NOT NULL           -- CHECK IN ('IDENTIFICADO','NAO_LOCALIZADO','ILEGIVEL')
  VALOR           VARCHAR2(200 CHAR)                   -- só IDENTIFICADO
  PAGINA          NUMBER(3)                            -- origem (PDF); CHECK >= 1
  TRECHO          VARCHAR2(200 CHAR)                   -- origem
  PK (EXTRACAO_DOCUMENTO_ID, CODIGO)
  CHECK (VALOR IS NULL OR SITUACAO = 'IDENTIFICADO')
  CHECK (SITUACAO = 'IDENTIFICADO' OR (PAGINA IS NULL AND TRECHO IS NULL))
  CHECK (SITUACAO <> 'IDENTIFICADO' OR VALOR IS NOT NULL)
)
```

O banco impõe os invariantes de campo: valor só quando identificado; origem só quando identificado; identificado sempre com valor. Índices nas FKs (`EXTRACAO_ID`, `EXTRACAO_DOCUMENTO_ID`, `DOCUMENTO_ID`).

## Campos extraídos (os usados pelas regras RC-R01 a RC-R06)

| Código | Regra(s) que usam | Valor quando identificado |
|---|---|---|
| `TIPO_DOCUMENTO` | R01, R03 | `ENCAMINHAMENTO`, `IDENTIFICACAO`, `AUTORIZACAO_RESPONSAVEL` ou `OUTRO` |
| `NOME` | R02 | nome sintético da paciente |
| `DATA_NASCIMENTO` | R02, R06 | `AAAA-MM-DD` |
| `MOTIVO_ENCAMINHAMENTO` | R04 | texto do motivo (≤ 200) |
| `DATA_EMISSAO` | R05 | `AAAA-MM-DD` |
| `AUTORIZACAO_ASSINADA` | R06 | `SIM` ou `NAO` |

Documento `LIDO` traz **os 6 campos**, cada um numa das três situações (um campo irrelevante ao tipo do documento é `NAO_LOCALIZADO`, sem valor). Documento não lido não traz campo algum.

## Regras de estado

| Situação | Condição | Observação |
|---|---|---|
| Leitura `ATUAL` | `RC_EXTRACAO.REVISAO = RC_SOLICITACAO.REVISAO_ATUAL` | derivada, não gravada |
| Leitura `DESATUALIZADA` | `REVISAO < REVISAO_ATUAL` | depois de remover/substituir/enviar documento |
| Estado do pedido | nunca muda | RC-11/RC-12 tratam os estados que dependem da leitura |

`POST` aceita em qualquer estado, **exceto** `SUBMETIDA_SIMULACAO` (409). `GET` aceita em qualquer estado.

## Invariantes (verificadas por teste)

1. No máximo uma leitura por `(pedido, revisão)`, mesmo com solicitações repetidas e simultâneas.
2. Campo `IDENTIFICADO` tem valor; `NAO_LOCALIZADO` e `ILEGIVEL` nunca têm valor nem origem.
3. Documento `LIDO` tem os 6 campos; `PROTEGIDO` e `LEITURA_VAZIA` não têm campo e nunca têm `NAO_LOCALIZADO`.
4. Falha do leitor ou saída inválida não deixa nenhuma linha gravada.
5. A leitura cobre exatamente os documentos disponíveis da revisão capturada; documento removido não entra.
6. Toda leitura tem `simulada = true`; nenhuma resposta traz caminho, hash ou nome interno.
7. A leitura não altera `ESTADO`, `REVISAO_ATUAL` nem `RC_ANALISE`.
8. Excluir rascunho apaga também as leituras do pedido (sem resíduo).

## Entidades e DTOs (Java)

| Tipo | Papel |
|---|---|
| `LeitorDeDocumento` (interface) | `LeituraDeDocumento ler(ArquivoParaLeitura)`; lança `LeituraIndisponivelException` |
| `ArquivoParaLeitura` (record) | tipo (PDF/JPEG/PNG), páginas, bytes; só em memória |
| `LeituraDeDocumento` (record) | `SituacaoLeitura` (`LIDO`, `PROTEGIDO`, `LEITURA_VAZIA`) + lista de `CampoLido` |
| `CampoLido` (record) | `CodigoCampo`, `SituacaoCampo`, valor, página, trecho |
| `Extracao`, `ExtracaoDocumento`, `CampoExtraido` | modelo persistido (records) |
| `ExtracaoRepository` | `buscarAtual…`, `gravar(...)` → `ResultadoGravacao` (`GRAVADA`, `JA_EXISTIA`, `REVISAO_MUDOU`, `SUBMETIDO`, `NAO_ENCONTRADO`) |
| DTOs de resposta | `ExtracaoResponse` (`situacao`, `revisao`, `simulada`, `aviso`, `documentos[]`), `DocumentoLidoResponse` (`documentoId`, `tipoDocumental`, `situacao`, `motivo`, `reenviar`, `campos[]`), `CampoExtraidoResponse` (`codigo`, `situacao`, `valor`, `origem`) |
