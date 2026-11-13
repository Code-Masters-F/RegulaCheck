-- =============================================================================
-- RegulaCheck - 09_conferencia.sql  (RC-11)
-- Finalidade : criar a tabela onde a API guarda a decisao da paciente sobre cada campo lido (confirmar ou apontar
--              divergencia), usada na conferencia da transcricao:
--              RC_CONFERENCIA_CAMPO   uma decisao VIGENTE por campo de uma leitura
-- Ordem      : depois do 08_extracao.sql. Rode SOMENTE em banco criado ANTES da RC-11.
--              Banco criado com o 01_ atual ja tem esta tabela: nao rode este script (ORA-00955).
-- Efeito     : ADITIVO. Apenas CREATE TABLE. Nenhuma tabela existente e alterada.
-- Se falhar no meio: o CREATE e atomico. Confira em USER_TABLES se RC_CONFERENCIA_CAMPO existe e rode de novo apenas
--              se faltar. Ate concluir, so a rota de conferencia responde 503 (ORA-00942 no log); o resto da API
--              (inclusive a leitura da RC-10 e excluir rascunho) segue funcionando.
-- Banco compartilhado: so o prefixo RC_ e tocado. Dados somente sinteticos.
-- A paciente so CONFIRMA ou aponta DIVERGENCIA: a tabela nao tem coluna de valor digitado. A leitura original
-- (RC_EXTRACAO*) nunca e alterada. A decisao nao muda estado nem revisao do pedido.
-- A FK composta garante que o campo existe na leitura. Qual decisao cabe a qual situacao do campo (confirmar
-- identificado; divergir pelo motivo certo; nenhuma em campo ilegivel) depende de outra tabela e e imposto pelo
-- servico, sob o bloqueio da linha do pedido.
-- =============================================================================

CREATE TABLE RC_CONFERENCIA_CAMPO (
    EXTRACAO_DOCUMENTO_ID NUMBER NOT NULL,
    CODIGO                VARCHAR2(40 CHAR) NOT NULL,
    DECISAO               VARCHAR2(20 CHAR) NOT NULL,
    MOTIVO                VARCHAR2(40 CHAR),
    DECIDIDA_EM           TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP AT TIME ZONE 'UTC' NOT NULL,
    CONSTRAINT RC_CONFERENCIA_CAMPO_PK      PRIMARY KEY (EXTRACAO_DOCUMENTO_ID, CODIGO),
    CONSTRAINT RC_CONFERENCIA_CAMPO_FK_CAMPO FOREIGN KEY (EXTRACAO_DOCUMENTO_ID, CODIGO)
        REFERENCES RC_EXTRACAO_CAMPO (EXTRACAO_DOCUMENTO_ID, CODIGO),
    CONSTRAINT RC_CONFERENCIA_CAMPO_CK_DEC  CHECK (DECISAO IN ('CONFIRMADA', 'DIVERGENTE')),
    CONSTRAINT RC_CONFERENCIA_CAMPO_CK_MOT  CHECK (MOTIVO IS NULL OR MOTIVO IN ('VALOR_DIFERENTE_DO_DOCUMENTO',
        'CAMPO_EXISTE_E_NAO_FOI_LIDO')),
    CONSTRAINT RC_CONFERENCIA_CAMPO_CK_PAR  CHECK ((DECISAO = 'CONFIRMADA' AND MOTIVO IS NULL)
        OR (DECISAO = 'DIVERGENTE' AND MOTIVO IS NOT NULL))
);
