-- =============================================================================
-- RegulaCheck - 08_extracao.sql  (RC-10)
-- Finalidade : criar as tabelas onde a API guarda a leitura (SIMULADA) dos documentos de um pedido:
--              RC_EXTRACAO            uma leitura por (pedido, revisao do conjunto documental)
--              RC_EXTRACAO_DOCUMENTO  a leitura de cada documento (lido, protegido ou leitura vazia)
--              RC_EXTRACAO_CAMPO      os campos extraidos de um documento lido
-- Ordem      : depois do 01_criar_esquema.sql. Rode SOMENTE em banco criado ANTES da RC-10.
--              Banco criado com o 01_ atual ja tem estas tabelas: nao rode este script (ORA-00955).
-- Efeito     : ADITIVO. Apenas CREATE TABLE / CREATE INDEX. Nenhuma tabela existente e alterada.
-- Se falhar no meio: cada CREATE e atomico. Confira em USER_TABLES o que ja foi criado e rode de novo apenas o que
--              faltar. Ate concluir, so a rota de extracao responde 503 (ORA-00942 no log); o resto da API segue.
-- Banco compartilhado: so o prefixo RC_ e tocado. Dados somente sinteticos.
-- "Atual" nao e coluna: a leitura e ATUAL quando RC_EXTRACAO.REVISAO = RC_SOLICITACAO.REVISAO_ATUAL.
-- =============================================================================

CREATE TABLE RC_EXTRACAO (
    ID              NUMBER GENERATED ALWAYS AS IDENTITY,
    SOLICITACAO_ID  NUMBER NOT NULL,
    REVISAO         NUMBER(6) NOT NULL,
    SIMULADA        NUMBER(1) DEFAULT 1 NOT NULL,
    CRIADA_EM       TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP AT TIME ZONE 'UTC' NOT NULL,
    CONSTRAINT RC_EXTRACAO_PK           PRIMARY KEY (ID),
    CONSTRAINT RC_EXTRACAO_UK_REVISAO   UNIQUE (SOLICITACAO_ID, REVISAO),
    CONSTRAINT RC_EXTRACAO_FK_REVISAO   FOREIGN KEY (SOLICITACAO_ID, REVISAO)
        REFERENCES RC_REVISAO (SOLICITACAO_ID, NUMERO),
    CONSTRAINT RC_EXTRACAO_CK_SIMULADA  CHECK (SIMULADA = 1)
);

CREATE TABLE RC_EXTRACAO_DOCUMENTO (
    ID           NUMBER GENERATED ALWAYS AS IDENTITY,
    EXTRACAO_ID  NUMBER NOT NULL,
    DOCUMENTO_ID NUMBER NOT NULL,
    SITUACAO     VARCHAR2(20 CHAR) NOT NULL,
    CONSTRAINT RC_EXTRACAO_DOC_PK        PRIMARY KEY (ID),
    CONSTRAINT RC_EXTRACAO_DOC_UK        UNIQUE (EXTRACAO_ID, DOCUMENTO_ID),
    CONSTRAINT RC_EXTRACAO_DOC_FK_EXTR   FOREIGN KEY (EXTRACAO_ID) REFERENCES RC_EXTRACAO (ID),
    CONSTRAINT RC_EXTRACAO_DOC_FK_DOC    FOREIGN KEY (DOCUMENTO_ID) REFERENCES RC_DOCUMENTO (ID),
    CONSTRAINT RC_EXTRACAO_DOC_CK_SIT    CHECK (SITUACAO IN ('LIDO', 'PROTEGIDO', 'LEITURA_VAZIA'))
);

-- Valor e origem so existem em campo IDENTIFICADO; campo IDENTIFICADO sempre tem valor. Ilegivel e nao
-- localizado nunca carregam valor (o sistema nao completa informacao por suposicao).
CREATE TABLE RC_EXTRACAO_CAMPO (
    EXTRACAO_DOCUMENTO_ID NUMBER NOT NULL,
    CODIGO                VARCHAR2(40 CHAR) NOT NULL,
    SITUACAO              VARCHAR2(20 CHAR) NOT NULL,
    VALOR                 VARCHAR2(200 CHAR),
    PAGINA                NUMBER(3),
    TRECHO                VARCHAR2(200 CHAR),
    CONSTRAINT RC_EXTRACAO_CAMPO_PK       PRIMARY KEY (EXTRACAO_DOCUMENTO_ID, CODIGO),
    CONSTRAINT RC_EXTRACAO_CAMPO_FK_DOC   FOREIGN KEY (EXTRACAO_DOCUMENTO_ID)
        REFERENCES RC_EXTRACAO_DOCUMENTO (ID),
    CONSTRAINT RC_EXTRACAO_CAMPO_CK_COD   CHECK (CODIGO IN ('TIPO_DOCUMENTO', 'NOME', 'DATA_NASCIMENTO',
        'MOTIVO_ENCAMINHAMENTO', 'DATA_EMISSAO', 'AUTORIZACAO_ASSINADA')),
    CONSTRAINT RC_EXTRACAO_CAMPO_CK_SIT   CHECK (SITUACAO IN ('IDENTIFICADO', 'NAO_LOCALIZADO', 'ILEGIVEL')),
    CONSTRAINT RC_EXTRACAO_CAMPO_CK_PAG   CHECK (PAGINA IS NULL OR PAGINA >= 1),
    CONSTRAINT RC_EXTRACAO_CAMPO_CK_VALOR CHECK (VALOR IS NULL OR SITUACAO = 'IDENTIFICADO'),
    CONSTRAINT RC_EXTRACAO_CAMPO_CK_IDENT CHECK (SITUACAO <> 'IDENTIFICADO' OR VALOR IS NOT NULL),
    CONSTRAINT RC_EXTRACAO_CAMPO_CK_ORIG  CHECK (SITUACAO = 'IDENTIFICADO' OR (PAGINA IS NULL AND TRECHO IS NULL))
);

CREATE INDEX RC_EXTRACAO_DOC_IX_DOCUMENTO ON RC_EXTRACAO_DOCUMENTO (DOCUMENTO_ID);
