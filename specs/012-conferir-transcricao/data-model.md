# Data Model: Conferir a transcrição antes da validação (RC-11)

## Mudança de esquema

**Script aditivo `database/scripts/09_conferencia.sql`** (só `CREATE TABLE`, prefixo `RC_`); o `01_criar_esquema.sql` também passa a criá-la (banco novo). Aplicar o `09_` no Oracle compartilhado exige autorização específica. Sem o `09_`, só a conferência falha (503); o resto da API, inclusive a leitura (RC-10) e excluir rascunho, segue funcionando.

```sql
RC_CONFERENCIA_CAMPO (
  EXTRACAO_DOCUMENTO_ID  NUMBER NOT NULL            -- FK composta -> RC_EXTRACAO_CAMPO
  CODIGO                 VARCHAR2(40 CHAR) NOT NULL -- FK composta -> RC_EXTRACAO_CAMPO
  DECISAO                VARCHAR2(20 CHAR) NOT NULL -- CHECK IN ('CONFIRMADA','DIVERGENTE')
  MOTIVO                 VARCHAR2(40 CHAR)          -- só e sempre em DIVERGENTE
  DECIDIDA_EM            TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP AT TIME ZONE 'UTC' NOT NULL
  PK (EXTRACAO_DOCUMENTO_ID, CODIGO)                -- uma decisão vigente por campo
  FK (EXTRACAO_DOCUMENTO_ID, CODIGO) REFERENCES RC_EXTRACAO_CAMPO (EXTRACAO_DOCUMENTO_ID, CODIGO)
  CHECK (MOTIVO IS NULL OR MOTIVO IN ('VALOR_DIFERENTE_DO_DOCUMENTO','CAMPO_EXISTE_E_NAO_FOI_LIDO'))
  CHECK ((DECISAO = 'CONFIRMADA' AND MOTIVO IS NULL) OR (DECISAO = 'DIVERGENTE' AND MOTIVO IS NOT NULL))
)
```

A FK composta garante que o campo existe na leitura. O banco **não** impõe "qual decisão cabe a qual situação do campo" (confirmar identificado, divergir por motivo certo, nenhuma em ilegível): isso depende da situação do campo em outra tabela e é imposto pelo serviço, sob o bloqueio do pedido, e provado pelos testes.

## Regras de decisão por situação do campo

| Situação do campo | Decisão aceita | Motivo aceito |
|---|---|---|
| `IDENTIFICADO` | `CONFIRMADA` ou `DIVERGENTE` | em `DIVERGENTE`: `VALOR_DIFERENTE_DO_DOCUMENTO` |
| `NAO_LOCALIZADO` | só `DIVERGENTE` | `CAMPO_EXISTE_E_NAO_FOI_LIDO` |
| `ILEGIVEL` | nenhuma (422) | — |
| qualquer campo de documento não lido | não existe (documento sem campos) | — |

## Situação da conferência (calculada, não gravada)

| Situação | Quando (nesta precedência) |
|---|---|
| `DESATUALIZADA` | a leitura não é a atual do pedido (revisão da leitura ≠ revisão do pedido) |
| `REQUER_CONFERENCIA` | algum documento não lido, ou algum campo `ILEGIVEL` em documento lido, ou alguma decisão `DIVERGENTE` |
| `PENDENTE` | algum campo `IDENTIFICADO` sem decisão |
| `CONFERIDA` | todo campo `IDENTIFICADO` confirmado e nenhuma das condições acima |

`NAO_LOCALIZADO` sem divergência não pesa na situação. `liberadoParaSubmissao` é sempre `false`.

## Efeitos no pedido

| O que | Efeito |
|---|---|
| `RC_SOLICITACAO.ESTADO`, `REVISAO_ATUAL`, `ATUALIZADA_EM` | **nenhum** (a decisão é anotação da leitura; D3) |
| `RC_REVISAO`, `RC_ANALISE`, `RC_EXTRACAO*`, `RC_DOCUMENTO` | **nenhum** (leitura e documento originais ficam intactos) |
| Excluir rascunho (RC-09) | apaga `RC_CONFERENCIA_CAMPO` → `RC_EXTRACAO_CAMPO` → `RC_EXTRACAO_DOCUMENTO` → `RC_EXTRACAO` → documentos → revisões → pedido |

## Invariantes (verificadas por teste)

1. Um campo tem no máximo uma decisão vigente; decidir de novo substitui; repetir a mesma não muda `DECIDIDA_EM`.
2. Nenhuma decisão existe para campo ilegível, para campo de leitura desatualizada ou para campo fora da leitura do pedido.
3. Decisão `DIVERGENTE` sempre tem motivo da lista; `CONFIRMADA` nunca tem motivo.
4. Nenhum valor digitado é aceito nem gravado; a leitura original (valores, situações, origens) é idêntica antes e depois de qualquer decisão.
5. Registrar é tudo ou nada: uma decisão inválida no lote não grava nenhuma das outras.
6. Pedido `SUBMETIDA_SIMULACAO` não aceita decisão; a consulta continua.
7. A conferência não altera estado, revisão, análise nem libera submissão.
8. Excluir rascunho deixa zero decisões, leituras, documentos e pedido.

## Entidades e DTOs (Java)

| Tipo | Papel |
|---|---|
| `TipoDecisao` (enum) | `CONFIRMADA`, `DIVERGENTE` |
| `MotivoDivergencia` (enum) | `VALOR_DIFERENTE_DO_DOCUMENTO`, `CAMPO_EXISTE_E_NAO_FOI_LIDO` |
| `DecisaoConferencia` (record) | documentoId, `CodigoCampo`, `TipoDecisao`, motivo (anulável), instante |
| `ConferenciaRepository` | `buscar(pacienteId, solicitacaoId, extracaoId)` → leitura + decisões + revisão atual; `registrar(...)` → `ResultadoRegistroDecisao` (`REGISTRADA`, `NAO_ENCONTRADO`, `SUBMETIDO`, `DESATUALIZADA`, `DOCUMENTO_FORA_DA_LEITURA`, `DECISAO_NAO_APLICAVEL`) |
| `SituacaoConferencia` | regra pura de D4 e as contagens |
| `ConferenciaRequest` | `JsonNode decisoes`, validado à mão (chaves fechadas) |
| `ConferenciaResponse` | `extracaoId`, `situacao`, `liberadoParaSubmissao` (false), `simulada`, `aviso`, `atualizadaEm`, `resumo`, `documentos[]` |
| `DocumentoConferidoResponse` | `documentoId`, `tipoDocumental`, `situacao` (`LIDO`…), `motivo`/`reenviar`/`orientacao` se não lido, `campos[]` |
| `CampoConferidoResponse` | `codigo`, `situacao`, `valor`/`origem` (só identificado), `naoVerificado`, `decisao` (tipo, motivo, `decididaEm`), `orientacao` |
