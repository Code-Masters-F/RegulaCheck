-- =============================================================================
-- RegulaCheck - 05_sessao_login.sql  (RC-04)
-- Finalidade : criar a tabela das sessoes de login da API (POST /auth/login).
-- Ordem      : depois do 01_criar_esquema.sql e do 02_carga_sintetica.sql.
--              Pode rodar antes ou depois do 03 e do 04; nenhum deles depende dela.
-- Efeito     : ADITIVO. Cria 1 tabela (RC_SESSAO_LOGIN) e 1 indice. Nao altera as
--              tabelas da RC-03 nem apaga dados.
-- Pre-requisito: RC_USUARIO existente. Se RC_SESSAO_LOGIN ja existir, cada CREATE falha
--                com ORA-00955 e nada e alterado.
-- Banco compartilhado: todo objeto tem o prefixo RC_; nenhum outro objeto e referenciado.
-- Seguranca  : a tabela NUNCA guarda o token em claro nem senha. TOKEN_HASH e o SHA-256
--              (hexadecimal minusculo, 64 caracteres) do token entregue a paciente; quem
--              ler a tabela nao consegue usar as sessoes.
-- Nao confundir com RC_SESSAO_CAPTURA (leituras dos sensores simulados).
-- Instantes  : TIMESTAMP WITH TIME ZONE, gravados em UTC.
-- =============================================================================

CREATE TABLE RC_SESSAO_LOGIN (
    ID            NUMBER GENERATED ALWAYS AS IDENTITY,
    USUARIO_ID    NUMBER NOT NULL,
    TOKEN_HASH    CHAR(64 CHAR) NOT NULL,
    CRIADA_EM     TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP AT TIME ZONE 'UTC' NOT NULL,
    EXPIRA_EM     TIMESTAMP WITH TIME ZONE NOT NULL,
    ENCERRADA_EM  TIMESTAMP WITH TIME ZONE,
    CONSTRAINT RC_SESSAO_LOGIN_PK          PRIMARY KEY (ID),
    CONSTRAINT RC_SESSAO_LOGIN_UK_TOKEN    UNIQUE (TOKEN_HASH),
    CONSTRAINT RC_SESSAO_LOGIN_FK_USUARIO  FOREIGN KEY (USUARIO_ID) REFERENCES RC_USUARIO (ID),
    CONSTRAINT RC_SESSAO_LOGIN_CK_TOKEN    CHECK (REGEXP_LIKE(TOKEN_HASH, '^[0-9a-f]{64}$')),
    CONSTRAINT RC_SESSAO_LOGIN_CK_PRAZO    CHECK (EXPIRA_EM > CRIADA_EM)
);

CREATE INDEX RC_SESSAO_LOGIN_IX_USUARIO ON RC_SESSAO_LOGIN (USUARIO_ID);
