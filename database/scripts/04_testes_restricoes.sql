-- =============================================================================
-- RegulaCheck - 04_testes_restricoes.sql  (RC-03)
-- Finalidade : provar que o banco REJEITA dados invalidos. Cada um dos 14 casos tenta uma
--              violacao e confere o codigo ORA esperado; imprime "OK" ou "FALHOU".
--              Os casos 13 e 14 (RC-07) exigem o motivo PROTOCOLO_ALTERADO: em banco criado
--              antes da RC-07, rode antes o 06_motivo_revisao_protocolo.sql.
-- Ordem      : 4o script, depois do 01 e do 02 (usa os dados da carga como referencia).
-- Efeito     : NAO DEIXA DADOS. Cada caso termina com ROLLBACK, e o script confere no fim
--              que a contagem de linhas voltou ao valor da carga original.
-- Saida      : exige DBMS_OUTPUT habilitado (SQL Developer: Exibir > Saida DBMS;
--              SQLcl/SQL*Plus: SET SERVEROUTPUT ON antes de executar).
-- Dados      : sinteticos. Nenhum objeto fora do prefixo RC_ e tocado.
-- =============================================================================

DECLARE
    v_paciente_a NUMBER;
    v_protocolo  NUMBER;
    v_regra      NUMBER;
    v_sol_rasc   NUMBER;  -- pedido em RASCUNHO (sem analise)
    v_sol_subm   NUMBER;  -- pedido submetido (tem analise vigente e submissao)
    v_sessao     NUMBER;
    v_an_vig     NUMBER;
    v_an_novo    NUMBER;
    v_ok         NUMBER := 0;
    v_falhou     NUMBER := 0;
    v_linhas     NUMBER;

    -- Compara o codigo obtido com o esperado. Codigo 0 significa que o banco ACEITOU o dado invalido.
    PROCEDURE confere(p_caso VARCHAR2, p_esperado NUMBER, p_obtido NUMBER) IS
    BEGIN
        IF p_obtido = p_esperado THEN
            v_ok := v_ok + 1;
            DBMS_OUTPUT.PUT_LINE('OK      ' || p_caso || ' (ORA' || TO_CHAR(p_esperado) || ')');
        ELSE
            v_falhou := v_falhou + 1;
            DBMS_OUTPUT.PUT_LINE('FALHOU  ' || p_caso || ' - esperado ORA' || TO_CHAR(p_esperado)
                || ', obtido ' || CASE WHEN p_obtido = 0 THEN 'nenhum erro (dado invalido aceito)'
                                       ELSE 'ORA' || TO_CHAR(p_obtido) END);
        END IF;
        ROLLBACK;
    END;

    -- Cria uma analise (rolada de volta depois) para o pedido em rascunho, usada nos casos 3, 11 e 12.
    FUNCTION analise_temporaria RETURN NUMBER IS
        v_id NUMBER;
    BEGIN
        INSERT INTO RC_ANALISE (SOLICITACAO_ID, REVISAO, PROTOCOLO_ID, RESULTADO_GERAL, VIGENTE)
        VALUES (v_sol_rasc, 1, v_protocolo, 'COM_PENDENCIAS', 1)
        RETURNING ID INTO v_id;
        RETURN v_id;
    END;
BEGIN
    SELECT ID INTO v_paciente_a FROM RC_USUARIO WHERE EMAIL = 'paciente.a@exemplo.invalid';
    SELECT ID INTO v_protocolo FROM RC_PROTOCOLO WHERE IDENTIFICADOR = 'PROTOCOLO-ACADEMICO-FICTICIO' AND VERSAO = 'v0.1';
    SELECT ID INTO v_regra FROM RC_REGRA WHERE PROTOCOLO_ID = v_protocolo AND CODIGO = 'RC-R01';
    SELECT ID INTO v_sol_rasc FROM RC_SOLICITACAO WHERE ESTADO = 'RASCUNHO';
    SELECT ID INTO v_sol_subm FROM RC_SOLICITACAO WHERE ESTADO = 'SUBMETIDA_SIMULACAO';
    SELECT ID INTO v_sessao FROM RC_SESSAO_CAPTURA WHERE SOLICITACAO_ID = v_sol_rasc;
    SELECT ID INTO v_an_vig FROM RC_ANALISE WHERE SOLICITACAO_ID = v_sol_subm AND VIGENTE = 1;

    -- 1. Perfil fora de PACIENTE/ADMINISTRADOR
    BEGIN
        INSERT INTO RC_USUARIO (EMAIL, NOME, SENHA_HASH, PERFIL)
        VALUES ('teste1@exemplo.invalid', 'Teste', 'hash', 'MEDICO');
        confere('perfil invalido', -2290, 0);
    EXCEPTION WHEN OTHERS THEN confere('perfil invalido', -2290, SQLCODE);
    END;

    -- 2. Estado da solicitacao fora dos 7 definidos
    BEGIN
        INSERT INTO RC_SOLICITACAO (PACIENTE_ID, PROTOCOLO_ID, ESTADO)
        VALUES (v_paciente_a, v_protocolo, 'APROVADA');
        confere('estado invalido', -2290, 0);
    EXCEPTION WHEN OTHERS THEN confere('estado invalido', -2290, SQLCODE);
    END;

    -- 3. Resultado de regra fora dos 4 valores
    BEGIN
        v_an_novo := analise_temporaria;
        INSERT INTO RC_RESULTADO_REGRA (ANALISE_ID, REGRA_ID, RESULTADO) VALUES (v_an_novo, v_regra, 'TALVEZ');
        confere('resultado de regra invalido', -2290, 0);
    EXCEPTION WHEN OTHERS THEN confere('resultado de regra invalido', -2290, SQLCODE);
    END;

    -- 4. E-mail de conta duplicado
    BEGIN
        INSERT INTO RC_USUARIO (EMAIL, NOME, SENHA_HASH, PERFIL)
        VALUES ('paciente.a@exemplo.invalid', 'Outra', 'hash', 'PACIENTE');
        confere('e-mail duplicado', -1, 0);
    EXCEPTION WHEN OTHERS THEN confere('e-mail duplicado', -1, SQLCODE);
    END;

    -- 5. Codigo de regra duplicado na mesma versao do protocolo
    BEGIN
        INSERT INTO RC_REGRA (PROTOCOLO_ID, CODIGO, DESCRICAO, EVIDENCIA_EXIGIDA, NATUREZA, MENSAGEM, RESPONSAVEL, ORDEM)
        VALUES (v_protocolo, 'RC-R01', 'Duplicada', 'x', 'OBRIGATORIA', 'x', 'PACIENTE', 9);
        confere('codigo de regra duplicado na versao', -1, 0);
    EXCEPTION WHEN OTHERS THEN confere('codigo de regra duplicado na versao', -1, SQLCODE);
    END;

    -- 6. Referencia a registro inexistente (paciente que nao existe)
    BEGIN
        INSERT INTO RC_SOLICITACAO (PACIENTE_ID, PROTOCOLO_ID, ESTADO)
        VALUES (-1, v_protocolo, 'RASCUNHO');
        confere('referencia inexistente', -2291, 0);
    EXCEPTION WHEN OTHERS THEN confere('referencia inexistente', -2291, SQLCODE);
    END;

    -- 7. Duas analises vigentes no mesmo pedido (o pedido submetido ja tem a vigente da revisao 2)
    BEGIN
        INSERT INTO RC_REVISAO (SOLICITACAO_ID, NUMERO, MOTIVO) VALUES (v_sol_subm, 3, 'CONFERENCIA_ALTERADA');
        INSERT INTO RC_ANALISE (SOLICITACAO_ID, REVISAO, PROTOCOLO_ID, RESULTADO_GERAL, VIGENTE)
        VALUES (v_sol_subm, 3, v_protocolo, 'COM_PENDENCIAS', 1);
        confere('segunda analise vigente no pedido', -1, 0);
    EXCEPTION WHEN OTHERS THEN confere('segunda analise vigente no pedido', -1, SQLCODE);
    END;

    -- 8. Segunda submissao do mesmo pedido
    BEGIN
        INSERT INTO RC_SUBMISSAO (SOLICITACAO_ID, ANALISE_ID, REFERENCIA) VALUES (v_sol_subm, v_an_vig, 'SIM-TESTE-9999');
        confere('segunda submissao do pedido', -1, 0);
    EXCEPTION WHEN OTHERS THEN confere('segunda submissao do pedido', -1, SQLCODE);
    END;

    -- 9. Documento acima de 5 MB (5242880 bytes e o limite)
    BEGIN
        INSERT INTO RC_DOCUMENTO (SOLICITACAO_ID, REVISAO_ENVIO, NOME_ORIGINAL, TIPO, TAMANHO_BYTES, PAGINAS, CAMINHO_INTERNO)
        VALUES (v_sol_rasc, 1, 'grande.pdf', 'PDF', 5242881, 1, 'sim/teste-grande.dat');
        confere('documento acima de 5 MB', -2290, 0);
    EXCEPTION WHEN OTHERS THEN confere('documento acima de 5 MB', -2290, SQLCODE);
    END;

    -- 10. Unidade incoerente com o tipo de sensor (luminosidade deve ser LUX)
    BEGIN
        INSERT INTO RC_LEITURA_SENSOR (SESSAO_ID, TIPO, VALOR, UNIDADE, MEDIDA_EM)
        VALUES (v_sessao, 'LUMINOSIDADE', 300, 'CM', SYSTIMESTAMP);
        confere('unidade incoerente com o sensor', -2290, 0);
    EXCEPTION WHEN OTHERS THEN confere('unidade incoerente com o sensor', -2290, SQLCODE);
    END;

    -- 11. Pendencia sem resultado correspondente
    BEGIN
        v_an_novo := analise_temporaria;
        INSERT INTO RC_PENDENCIA (ANALISE_ID, REGRA_ID, TIPO, MENSAGEM, RESPONSAVEL)
        VALUES (v_an_novo, v_regra, 'PENDENTE', 'x', 'PACIENTE');
        confere('pendencia sem resultado correspondente', -2291, 0);
    EXCEPTION WHEN OTHERS THEN confere('pendencia sem resultado correspondente', -2291, SQLCODE);
    END;

    -- 12. Pendencia de tipo diferente do resultado (resultado ATENDE, pendencia PENDENTE)
    BEGIN
        v_an_novo := analise_temporaria;
        INSERT INTO RC_RESULTADO_REGRA (ANALISE_ID, REGRA_ID, RESULTADO) VALUES (v_an_novo, v_regra, 'ATENDE');
        INSERT INTO RC_PENDENCIA (ANALISE_ID, REGRA_ID, TIPO, MENSAGEM, RESPONSAVEL)
        VALUES (v_an_novo, v_regra, 'PENDENTE', 'x', 'PACIENTE');
        confere('pendencia de tipo diferente do resultado', -2291, 0);
    EXCEPTION WHEN OTHERS THEN confere('pendencia de tipo diferente do resultado', -2291, SQLCODE);
    END;

    -- 13. (RC-07) Motivo PROTOCOLO_ALTERADO deve ser ACEITO (codigo 0 = nenhum erro = esperado aqui)
    BEGIN
        INSERT INTO RC_REVISAO (SOLICITACAO_ID, NUMERO, MOTIVO) VALUES (v_sol_rasc, 998, 'PROTOCOLO_ALTERADO');
        confere('motivo PROTOCOLO_ALTERADO aceito', 0, 0);
    EXCEPTION WHEN OTHERS THEN confere('motivo PROTOCOLO_ALTERADO aceito', 0, SQLCODE);
    END;

    -- 14. (RC-07) Motivo inventado continua recusado pela restricao RC_REVISAO_CK_MOTIVO
    BEGIN
        INSERT INTO RC_REVISAO (SOLICITACAO_ID, NUMERO, MOTIVO) VALUES (v_sol_rasc, 999, 'MOTIVO_INVENTADO');
        confere('motivo de revisao inventado', -2290, 0);
    EXCEPTION WHEN OTHERS THEN confere('motivo de revisao inventado', -2290, SQLCODE);
    END;

    -- Conferencia final: nenhuma linha restante (total da carga original = 77).
    SELECT (SELECT COUNT(*) FROM RC_USUARIO) + (SELECT COUNT(*) FROM RC_PROTOCOLO) + (SELECT COUNT(*) FROM RC_REGRA)
         + (SELECT COUNT(*) FROM RC_SOLICITACAO) + (SELECT COUNT(*) FROM RC_REVISAO) + (SELECT COUNT(*) FROM RC_DOCUMENTO)
         + (SELECT COUNT(*) FROM RC_ANALISE) + (SELECT COUNT(*) FROM RC_RESULTADO_REGRA) + (SELECT COUNT(*) FROM RC_PENDENCIA)
         + (SELECT COUNT(*) FROM RC_SESSAO_CAPTURA) + (SELECT COUNT(*) FROM RC_LEITURA_SENSOR) + (SELECT COUNT(*) FROM RC_SUBMISSAO)
      INTO v_linhas FROM DUAL;
    IF v_linhas = 77 THEN
        v_ok := v_ok + 1;
        DBMS_OUTPUT.PUT_LINE('OK      nenhuma linha restante (total ' || v_linhas || ')');
    ELSE
        v_falhou := v_falhou + 1;
        DBMS_OUTPUT.PUT_LINE('FALHOU  linhas restantes: total ' || v_linhas || ' (esperado 77)');
    END IF;
    ROLLBACK;
    DBMS_OUTPUT.PUT_LINE('Resumo: ' || v_ok || ' OK, ' || v_falhou || ' FALHOU');
END;
/
