-- =============================================================================
-- RegulaCheck - 10_analise_historico.sql  (RC-13)
-- Finalidade : preparar o banco para gravar as analises pedidas pela paciente e o historico por revisao:
--              RC_SOLICITACAO.ANALISE_INICIADA_EM  inicio da analise em andamento (so em PROCESSANDO)
--              RC_ANALISE.CONFERENCIA_EM            instante da conferencia considerada (UTC, sem fuso)
--              RC_ANALISE.DATA_REFERENCIA           data usada pelas regras de data (R05/R06)
--              RC_PENDENCIA.MOTIVO                  motivo do nao verificavel (lista fechada)
--              RC_PENDENCIA_EVIDENCIA               documento, campo e situacao de cada evidencia da pendencia
--              e trocar RC_ANALISE_UK_REVISAO (uma analise por revisao) por RC_ANALISE_UK_CONFERENCIA (uma por
--              revisao E conferencia): a decisao de conferencia nao cria revisao (RC-11), entao completar a
--              conferencia e analisar de novo gera uma segunda analise da MESMA revisao.
-- Ordem      : depois do 09_conferencia.sql. Rode SOMENTE em banco criado ANTES da RC-13.
--              Banco criado com o 01_ atual ja tem tudo isto: nao rode este script (ORA-01430/ORA-00955).
-- Efeito     : ADITIVO. Nenhuma linha e apagada ou alterada: as colunas novas nascem nulas (as analises da
--              carga 02_ ficam sem conferencia, data de referencia, motivo e evidencias). A unica remocao e a
--              restricao RC_ANALISE_UK_REVISAO, recriada mais permissiva na instrucao seguinte; como qualquer
--              conjunto unico em (SOLICITACAO_ID, REVISAO) tambem e unico com CONFERENCIA_EM, o ADD nao falha.
-- Pre-requisito: nenhum pedido em ESTADO = 'PROCESSANDO' (a carga 02_ nao tem; a API so passa a produzir esse
--              estado com este script). Se houver, RC_SOLICITACAO_CK_ANALISE falha com ORA-02293.
-- Se falhar no meio: cada instrucao e DDL e se confirma sozinha. Confira em USER_TAB_COLUMNS e USER_CONSTRAINTS
--              o que ja existe e rode so as instrucoes que faltam. ATENCAO: se o DROP de RC_ANALISE_UK_REVISAO
--              rodou e o ADD de RC_ANALISE_UK_CONFERENCIA falhou, rode o ADD antes de usar a API.
--              Ate concluir, so as rotas de analise respondem 503 (ORA-00904/ORA-00942 no log); o resto da API
--              (pedidos, documentos, leitura, conferencia, excluir rascunho) segue funcionando.
-- Segunda execucao: cada ALTER/CREATE falha (coluna/objeto ja existe) e nada muda; o DROP falha com ORA-02443
--              porque a restricao antiga ja nao existe.
-- Banco compartilhado: so o prefixo RC_ e tocado. Dados somente sinteticos.
-- Instantes  : ANALISE_INICIADA_EM segue a convencao TIMESTAMP WITH TIME ZONE em UTC. CONFERENCIA_EM e
--              TIMESTAMP (sem fuso) gravado em UTC porque o Oracle nao aceita TIMESTAMP WITH TIME ZONE em chave
--              unica, e ela faz parte de RC_ANALISE_UK_CONFERENCIA.
-- =============================================================================

-- Analise em andamento: o pedido fica em PROCESSANDO com o instante de inicio (base do limite de 1 minuto e
-- identificacao de quem esta processando). Fora de PROCESSANDO a coluna e sempre nula.
ALTER TABLE RC_SOLICITACAO ADD (ANALISE_INICIADA_EM TIMESTAMP WITH TIME ZONE);

ALTER TABLE RC_SOLICITACAO ADD CONSTRAINT RC_SOLICITACAO_CK_ANALISE CHECK (
    (ESTADO = 'PROCESSANDO' AND ANALISE_INICIADA_EM IS NOT NULL)
    OR (ESTADO <> 'PROCESSANDO' AND ANALISE_INICIADA_EM IS NULL));

-- Entrada considerada pela analise (alem da revisao, que ja existia).
ALTER TABLE RC_ANALISE ADD (CONFERENCIA_EM TIMESTAMP(6), DATA_REFERENCIA DATE);

ALTER TABLE RC_ANALISE DROP CONSTRAINT RC_ANALISE_UK_REVISAO;

ALTER TABLE RC_ANALISE ADD CONSTRAINT RC_ANALISE_UK_CONFERENCIA UNIQUE (SOLICITACAO_ID, REVISAO, CONFERENCIA_EM);

-- Motivo do nao verificavel: so existe em pendencia NAO_VERIFICAVEL (as da carga 02_ ficam sem).
ALTER TABLE RC_PENDENCIA ADD (MOTIVO VARCHAR2(40 CHAR));

ALTER TABLE RC_PENDENCIA ADD CONSTRAINT RC_PENDENCIA_CK_MOTIVO CHECK (MOTIVO IS NULL OR MOTIVO IN (
    'CAMPO_ILEGIVEL', 'DOCUMENTO_NAO_LIDO', 'TRANSCRICAO_NAO_CONFERIDA', 'DIVERGENCIA_APONTADA',
    'VALOR_NAO_INTERPRETAVEL', 'CONDICAO_INDETERMINADA', 'DOCUMENTOS_INSUFICIENTES', 'DEPENDE_DE_DOCUMENTO_AUSENTE'));

ALTER TABLE RC_PENDENCIA ADD CONSTRAINT RC_PENDENCIA_CK_MOTIVO_TIPO CHECK (TIPO = 'NAO_VERIFICAVEL' OR MOTIVO IS NULL);

-- Evidencia de uma pendencia: SEM valor e SEM trecho. A origem (pagina, trecho) e lida de RC_EXTRACAO_CAMPO da
-- leitura da mesma revisao (uma por revisao, nunca alterada), para nao copiar de novo o conteudo do documento.
-- A situacao e gravada porque a decisao de conferencia pode mudar depois e nao permitiria recalcula-la.
CREATE TABLE RC_PENDENCIA_EVIDENCIA (
    ANALISE_ID    NUMBER NOT NULL,
    REGRA_ID      NUMBER NOT NULL,
    ORDEM         NUMBER(3) NOT NULL,
    DOCUMENTO_ID  NUMBER NOT NULL,
    CODIGO        VARCHAR2(40 CHAR) NOT NULL,
    SITUACAO      VARCHAR2(30 CHAR) NOT NULL,
    CONSTRAINT RC_PEND_EVIDENCIA_PK        PRIMARY KEY (ANALISE_ID, REGRA_ID, ORDEM),
    CONSTRAINT RC_PEND_EVIDENCIA_FK_PEND   FOREIGN KEY (ANALISE_ID, REGRA_ID)
        REFERENCES RC_PENDENCIA (ANALISE_ID, REGRA_ID),
    CONSTRAINT RC_PEND_EVIDENCIA_FK_DOC    FOREIGN KEY (DOCUMENTO_ID) REFERENCES RC_DOCUMENTO (ID),
    CONSTRAINT RC_PEND_EVIDENCIA_CK_ORDEM  CHECK (ORDEM >= 1),
    CONSTRAINT RC_PEND_EVIDENCIA_CK_CODIGO CHECK (CODIGO IN ('TIPO_DOCUMENTO', 'NOME', 'DATA_NASCIMENTO',
        'MOTIVO_ENCAMINHAMENTO', 'DATA_EMISSAO', 'AUTORIZACAO_ASSINADA')),
    CONSTRAINT RC_PEND_EVIDENCIA_CK_SIT    CHECK (SITUACAO IN ('CONFIRMADO', 'NAO_LOCALIZADO', 'ILEGIVEL',
        'NAO_CONFERIDO', 'DIVERGENCIA_APONTADA', 'DOCUMENTO_NAO_LIDO'))
);

CREATE INDEX RC_PEND_EVID_IX_DOCUMENTO ON RC_PENDENCIA_EVIDENCIA (DOCUMENTO_ID);
