-- =============================================================================
-- RegulaCheck - 07_documento_tipo_e_tentativa.sql  (RC-08)
-- Finalidade : acrescentar a RC_DOCUMENTO o que o envio de documentos precisa guardar:
--              TIPO_DOCUMENTAL (encaminhamento, identificacao, autorizacao do responsavel ou outro),
--              CHAVE_ENVIO (identificacao da tentativa, que permite repetir um envio sem duplicar) e
--              HASH_CONTEUDO (SHA-256 do arquivo, para distinguir repeticao de reutilizacao indevida da chave).
-- Ordem      : depois do 01_criar_esquema.sql. Rode SOMENTE em banco criado ANTES da RC-08.
--              Banco criado com o 01_ atual ja tem estas colunas: nao rode este script.
-- Efeito     : ADITIVO. Apenas ALTER TABLE ... ADD. Nenhuma linha e apagada ou reescrita; as linhas da
--              carga sintetica (02_) recebem TIPO_DOCUMENTAL = 'OUTRO' pelo DEFAULT e ficam sem chave e sem hash.
-- Pre-requisito: RC_DOCUMENTO existente e ainda sem estas colunas (ORA-01430 se ja existirem).
-- Se falhar no meio: cada ALTER TABLE e atomico. Se o 1o passar e um seguinte falhar, rode de novo apenas os
--              que faltam (confira em USER_TAB_COLUMNS e USER_CONSTRAINTS). Ate concluir, a API nao consegue
--              gravar documentos (ORA-00904 no log; a resposta e o erro interno generico).
-- Banco compartilhado: so o prefixo RC_ e tocado.
-- Sem este script num banco antigo: pedidos, rascunhos, listar e trocar servico funcionam; apenas o ENVIO de
--              documento falha ao gravar, e a consulta do pedido (que conta documentos) continua funcionando.
-- =============================================================================

ALTER TABLE RC_DOCUMENTO ADD (
    TIPO_DOCUMENTAL  VARCHAR2(30 CHAR) DEFAULT 'OUTRO' NOT NULL,
    CHAVE_ENVIO      VARCHAR2(64 CHAR),
    HASH_CONTEUDO    VARCHAR2(64 CHAR)
);

ALTER TABLE RC_DOCUMENTO ADD CONSTRAINT RC_DOCUMENTO_CK_TIPO_DOC CHECK (TIPO_DOCUMENTAL IN
    ('ENCAMINHAMENTO', 'IDENTIFICACAO', 'AUTORIZACAO_RESPONSAVEL', 'OUTRO'));

ALTER TABLE RC_DOCUMENTO ADD CONSTRAINT RC_DOCUMENTO_CK_HASH CHECK (HASH_CONTEUDO IS NULL
    OR REGEXP_LIKE(HASH_CONTEUDO, '^[0-9a-f]{64}$'));

-- Unicidade da chave por pedido, permitindo varios envios sem chave no mesmo pedido. UNIQUE (SOLICITACAO_ID,
-- CHAVE_ENVIO) nao serve: no Oracle, chave composta com so uma coluna nula conta como valor (ORA-02299/ORA-00001
-- para o 2o documento sem chave). O indice funcional vira (NULL, NULL) sem chave, e entradas todas nulas nao entram.
CREATE UNIQUE INDEX RC_DOCUMENTO_UK_CHAVE ON RC_DOCUMENTO (
    CASE WHEN CHAVE_ENVIO IS NOT NULL THEN SOLICITACAO_ID END, CHAVE_ENVIO);
