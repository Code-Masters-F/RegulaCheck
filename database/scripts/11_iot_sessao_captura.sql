-- =============================================================================
-- RegulaCheck - 11_iot_sessao_captura.sql  (RC-21 + RC-22, Epico 7 - IoT simulada)
-- Finalidade : garantir no banco que cada pedido tenha NO MAXIMO UMA sessao de captura aberta
--              (ENCERRADA_EM IS NULL). A API ja serializa a abertura com o bloqueio do pedido (FOR UPDATE);
--              este indice e a rede de protecao para qualquer caminho que nao passe por ele (carga manual).
--              Sessoes encerradas geram NULL na expressao e nao entram no indice: um pedido pode ter varias.
-- Ordem      : depois do 10_analise_historico.sql. Rode SOMENTE em banco criado ANTES da RC-21.
--              Banco criado com o 01_ atual ja tem o indice: nao rode este script (ORA-00955).
-- Efeito     : ADITIVO. Nenhuma tabela, coluna ou linha e alterada.
-- Pre-requisito: nenhum pedido com mais de uma sessao aberta. Confira antes com:
--                SELECT SOLICITACAO_ID, COUNT(*) FROM RC_SESSAO_CAPTURA
--                 WHERE ENCERRADA_EM IS NULL GROUP BY SOLICITACAO_ID HAVING COUNT(*) > 1;
--              (a carga 02_ tem uma unica sessao aberta, no pedido 4). Se houver linhas, o CREATE falha com
--              ORA-01452: encerre as sessoes excedentes (UPDATE ... SET ENCERRADA_EM = SYSTIMESTAMP) e rode de novo.
-- Se falhar  : sem o indice a API continua funcionando (o bloqueio do pedido ja evita duplicidade pela API);
--              so a garantia no banco fica ausente.
-- Segunda execucao: o CREATE falha com ORA-00955 (nome ja usado) e nada muda.
-- Banco compartilhado: so o prefixo RC_ e tocado. Dados somente sinteticos.
-- =============================================================================

CREATE UNIQUE INDEX RC_SESSAO_CAPTURA_UK_ABERTA
    ON RC_SESSAO_CAPTURA (CASE WHEN ENCERRADA_EM IS NULL THEN SOLICITACAO_ID END);
