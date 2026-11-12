-- =============================================================================
-- RegulaCheck - 03_consultas_uso.sql  (RC-03)
-- Finalidade : 6 consultas de uso (JOIN e agregacao) que respondem as perguntas do negocio
--              sobre a carga sintetica do 02_carga_sintetica.sql.
-- Ordem      : 3o script, depois do 01 e do 02.
-- Efeito     : SOMENTE LEITURA (apenas SELECT). Pode ser executado quantas vezes quiser.
-- Dados      : sinteticos. O "resultado esperado" de cada consulta vale para a carga original.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Consulta 1 - Pedidos por paciente
-- Finalidade : quantos pedidos cada paciente tem. A conta de administrador nao aparece.
-- Esperado   : Paciente A (fictícia) = 3 pedidos; Paciente B (fictícia) = 1 pedido.
-- -----------------------------------------------------------------------------
SELECT u.NOME AS paciente, COUNT(s.ID) AS pedidos
  FROM RC_USUARIO u
  JOIN RC_SOLICITACAO s ON s.PACIENTE_ID = u.ID
 WHERE u.PERFIL = 'PACIENTE'
 GROUP BY u.NOME
 ORDER BY u.NOME;

-- -----------------------------------------------------------------------------
-- Consulta 2 - Analises por pedido
-- Finalidade : historico de analises de cada pedido, em ordem de revisao, com o resultado
--              geral, a versao do protocolo usada e se a analise e a vigente.
-- Esperado   : 5 linhas. Pedidos 1 e 3 tem duas revisoes (a revisao 2 e a vigente);
--              o pedido 2 tem uma. O pedido em rascunho nao tem analise.
-- -----------------------------------------------------------------------------
SELECT s.ID AS pedido, u.NOME AS paciente, a.REVISAO, a.RESULTADO_GERAL,
       p.IDENTIFICADOR || ' ' || p.VERSAO AS protocolo,
       CASE a.VIGENTE WHEN 1 THEN 'SIM' ELSE 'NAO' END AS vigente
  FROM RC_SOLICITACAO s
  JOIN RC_USUARIO u   ON u.ID = s.PACIENTE_ID
  JOIN RC_ANALISE a   ON a.SOLICITACAO_ID = s.ID
  JOIN RC_PROTOCOLO p ON p.ID = a.PROTOCOLO_ID
 ORDER BY s.ID, a.REVISAO;

-- -----------------------------------------------------------------------------
-- Consulta 3 - Pedidos por estado
-- Finalidade : distribuicao dos pedidos pelos estados presentes (base de indicadores).
-- Esperado   : COM_PENDENCIAS 1, RASCUNHO 1, REQUER_CONFERENCIA 1, SUBMETIDA_SIMULACAO 1.
-- -----------------------------------------------------------------------------
SELECT ESTADO, COUNT(*) AS pedidos
  FROM RC_SOLICITACAO
 GROUP BY ESTADO
 ORDER BY ESTADO;

-- -----------------------------------------------------------------------------
-- Consulta 4 - Pendencias por regra
-- Finalidade : quais regras mais geram pendencia e quem as corrige. Conta todas as analises
--              (inclusive as antigas); PENDENTE e NAO_VERIFICAVEL aparecem separados.
-- Esperado   : RC-R02 NAO_VERIFICAVEL 2; RC-R03 NAO_VERIFICAVEL 2 e PENDENTE 1;
--              RC-R04 PENDENTE 1; RC-R05 PENDENTE 1; RC-R06 NAO_VERIFICAVEL 1 (total 8).
-- -----------------------------------------------------------------------------
SELECT r.CODIGO, r.RESPONSAVEL, pe.TIPO, COUNT(*) AS pendencias
  FROM RC_PENDENCIA pe
  JOIN RC_REGRA r ON r.ID = pe.REGRA_ID
 GROUP BY r.CODIGO, r.RESPONSAVEL, pe.TIPO
 ORDER BY r.CODIGO, pe.TIPO;

-- -----------------------------------------------------------------------------
-- Consulta 5 - Pedidos corrigidos entre revisoes
-- Finalidade : pendencias que existiam numa revisao e deixaram de existir na revisao seguinte
--              do mesmo pedido (a paciente corrigiu ou reenviou e a regra foi resolvida).
-- Esperado   : 4 linhas - pedido 1: RC-R02 e RC-R03 (revisao 1 -> 2);
--              pedido 3: RC-R02 e RC-R06 (revisao 1 -> 2). RC-R03 do pedido 3 continua pendente.
-- -----------------------------------------------------------------------------
SELECT a1.SOLICITACAO_ID AS pedido, r.CODIGO AS regra,
       a1.REVISAO AS revisao_com_pendencia, a2.REVISAO AS revisao_sem_pendencia
  FROM RC_PENDENCIA pe1
  JOIN RC_ANALISE a1 ON a1.ID = pe1.ANALISE_ID
  JOIN RC_ANALISE a2 ON a2.SOLICITACAO_ID = a1.SOLICITACAO_ID AND a2.REVISAO = a1.REVISAO + 1
  JOIN RC_REGRA r    ON r.ID = pe1.REGRA_ID
 WHERE NOT EXISTS (SELECT 1 FROM RC_PENDENCIA pe2
                    WHERE pe2.ANALISE_ID = a2.ID AND pe2.REGRA_ID = pe1.REGRA_ID)
 ORDER BY a1.SOLICITACAO_ID, r.CODIGO;

-- -----------------------------------------------------------------------------
-- Consulta 6 - Resultado por regra da analise vigente de um pedido
-- Finalidade : base da tela de resultado - cada regra, seu resultado e o responsavel pela
--              correcao, para a analise vigente do pedido. Aqui, o pedido de numero menor
--              entre os que tem analise (pedido 1, revisao 2).
-- Esperado   : 6 linhas (RC-R01 a RC-R06); todas ATENDE, exceto RC-R06 NAO_APLICAVEL.
-- -----------------------------------------------------------------------------
SELECT r.CODIGO, r.DESCRICAO, rr.RESULTADO, r.RESPONSAVEL
  FROM RC_ANALISE a
  JOIN RC_RESULTADO_REGRA rr ON rr.ANALISE_ID = a.ID
  JOIN RC_REGRA r            ON r.ID = rr.REGRA_ID
 WHERE a.VIGENTE = 1
   AND a.SOLICITACAO_ID = (SELECT MIN(SOLICITACAO_ID) FROM RC_ANALISE)
 ORDER BY r.ORDEM;
