-- =============================================================================
-- RegulaCheck - 90_limpeza_DESTRUTIVA.sql  (RC-03)
--
--   *** ATENCAO: SCRIPT DESTRUTIVO. APAGA TODAS AS TABELAS RC_* E OS SEUS DADOS. ***
--
-- Finalidade : recomecar o esquema em AMBIENTE DE DESENVOLVIMENTO (depois, rode 01 e 02).
-- Ordem      : fora da sequencia normal. NENHUM outro script chama este arquivo.
-- Efeito     : DROP das 17 tabelas RC_* (12 da RC-03 + RC_SESSAO_LOGIN da RC-04 + 3 RC_EXTRACAO* da RC-10
--              + RC_CONFERENCIA_CAMPO da RC-11) (e, com elas, seus indices, restricoes e
--              sequencias de identity). Os dados sao perdidos. Nao ha desfazer.
-- Seguranca  : - so remove objetos com o prefixo RC_, citados um a um neste arquivo
--                (nao ha busca dinamica por tabelas); nunca toca PEQUENOVAREJO, VENDA ou
--                objetos de outros trabalhos do mesmo esquema;
--              - so executa se a variavel de confirmacao abaixo for trocada de NAO para SIM;
--              - por padrao falha com ORA-20001 sem apagar nada.
-- Como usar  : 1) confira que esta conectado ao esquema CERTO e que ninguem precisa destes dados;
--              2) troque 'NAO' por 'SIM' na linha marcada com <<CONFIRMACAO>>;
--              3) execute; 4) volte o valor para 'NAO' antes de salvar o arquivo.
-- =============================================================================

DECLARE
    v_confirmacao VARCHAR2(3) := 'NAO'; -- <<CONFIRMACAO>> troque NAO por SIM para permitir a limpeza

    -- Ordem inversa de dependencia: filhas antes das tabelas pai.
    TYPE t_tabelas IS TABLE OF VARCHAR2(30);
    v_tabelas t_tabelas := t_tabelas(
        'RC_SESSAO_LOGIN', 'RC_SUBMISSAO', 'RC_LEITURA_SENSOR', 'RC_SESSAO_CAPTURA', 'RC_PENDENCIA',
        'RC_RESULTADO_REGRA', 'RC_ANALISE', 'RC_CONFERENCIA_CAMPO', 'RC_EXTRACAO_CAMPO', 'RC_EXTRACAO_DOCUMENTO', 'RC_EXTRACAO',
        'RC_DOCUMENTO', 'RC_REVISAO',
        'RC_SOLICITACAO', 'RC_REGRA', 'RC_PROTOCOLO', 'RC_USUARIO');
BEGIN
    IF v_confirmacao <> 'SIM' THEN
        RAISE_APPLICATION_ERROR(-20001,
            'Limpeza NAO executada: confirmacao ausente. Troque NAO por SIM na linha <<CONFIRMACAO>> para apagar as tabelas RC_*.');
    END IF;

    FOR i IN 1 .. v_tabelas.COUNT LOOP
        EXECUTE IMMEDIATE 'DROP TABLE ' || v_tabelas(i) || ' PURGE';
        DBMS_OUTPUT.PUT_LINE('Removida: ' || v_tabelas(i));
    END LOOP;
END;
/
