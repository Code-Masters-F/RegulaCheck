-- =============================================================================
-- RegulaCheck - 02_carga_sintetica.sql  (RC-03)
-- Finalidade : inserir os dados sinteticos de demonstracao: 3 contas (paciente A,
--              paciente B e administrador), o protocolo PROTOCOLO-ACADEMICO-FICTICIO v0.1,
--              as regras RC-R01 a RC-R06 e 4 pedidos de exemplo em estados e revisoes
--              diferentes, com documentos, analises, resultados por regra, pendencias,
--              uma sessao de captura com leituras dos dois sensores e uma submissao simulada.
-- Ordem      : 2o script, depois do 01_criar_esquema.sql e antes do 03/04.
-- Efeito     : ADITIVO (somente INSERT). Nao apaga nem altera nada.
-- Atomicidade: tudo e um UNICO bloco PL/SQL com COMMIT apenas no fim. Se qualquer INSERT
--              falhar, o Oracle desfaz o bloco inteiro. Executar uma segunda vez falha em
--              ORA-00001 (e-mail ja cadastrado) e NAO duplica nada.
-- Dados      : exclusivamente sinteticos. E-mails no dominio reservado .invalid, nomes
--              genericos, documentos inexistentes (so metadados). Protocolo ficticio,
--              sem valor oficial. Nao ha diagnostico, urgencia nem autorizacao.
-- Senhas     : SENHA_HASH recebe somente hash BCrypt (versao 2a, custo 12), gerado com a
--              biblioteca at.favre.lib:bcrypt 0.10.2 a partir da senha de demonstracao
--              documentada em database/scripts/README.md. Nenhuma senha em texto puro.
-- Estados da carga: SUBMETIDA_SIMULACAO, COM_PENDENCIAS, REQUER_CONFERENCIA e RASCUNHO.
-- =============================================================================

DECLARE
    v_paciente_a   NUMBER;
    v_paciente_b   NUMBER;
    v_admin        NUMBER;
    v_protocolo    NUMBER;
    v_r01 NUMBER;
    v_r02 NUMBER;
    v_r03 NUMBER;
    v_r04 NUMBER;
    v_r05 NUMBER;
    v_r06 NUMBER;
    v_sol1 NUMBER;
    v_sol2 NUMBER;
    v_sol3 NUMBER;
    v_sol4 NUMBER;
    v_an   NUMBER;
    v_an_vigente_sol1 NUMBER;
    v_sessao NUMBER;

    c_msg_nv CONSTANT VARCHAR2(400) := 'Não conseguimos ler esta informação no documento enviado. '
        || 'Isso não significa que ela esteja faltando. Envie uma foto ou arquivo mais nítido ou confira a transcrição.';

    -- Cria uma regra do protocolo e devolve o identificador gerado.
    FUNCTION nova_regra(p_codigo VARCHAR2, p_desc VARCHAR2, p_evid VARCHAR2, p_nat VARCHAR2,
                        p_cond VARCHAR2, p_msg VARCHAR2, p_resp VARCHAR2, p_ordem NUMBER) RETURN NUMBER IS
        v_id NUMBER;
    BEGIN
        INSERT INTO RC_REGRA (PROTOCOLO_ID, CODIGO, DESCRICAO, EVIDENCIA_EXIGIDA, NATUREZA,
                              CONDICAO_APLICACAO, MENSAGEM, RESPONSAVEL, ORDEM)
        VALUES (v_protocolo, p_codigo, p_desc, p_evid, p_nat, p_cond, p_msg, p_resp, p_ordem)
        RETURNING ID INTO v_id;
        RETURN v_id;
    END;

    -- Cria uma solicitacao (revisao atual ja informada) e devolve o identificador.
    FUNCTION nova_solicitacao(p_paciente NUMBER, p_estado VARCHAR2, p_rev_atual NUMBER) RETURN NUMBER IS
        v_id NUMBER;
    BEGIN
        INSERT INTO RC_SOLICITACAO (PACIENTE_ID, PROTOCOLO_ID, ESTADO, REVISAO_ATUAL)
        VALUES (p_paciente, v_protocolo, p_estado, p_rev_atual)
        RETURNING ID INTO v_id;
        RETURN v_id;
    END;

    PROCEDURE nova_revisao(p_sol NUMBER, p_numero NUMBER, p_motivo VARCHAR2) IS
    BEGIN
        INSERT INTO RC_REVISAO (SOLICITACAO_ID, NUMERO, MOTIVO) VALUES (p_sol, p_numero, p_motivo);
    END;

    PROCEDURE novo_documento(p_sol NUMBER, p_rev_envio NUMBER, p_rev_remocao NUMBER, p_nome VARCHAR2,
                             p_tipo VARCHAR2, p_bytes NUMBER, p_paginas NUMBER, p_caminho VARCHAR2) IS
    BEGIN
        INSERT INTO RC_DOCUMENTO (SOLICITACAO_ID, REVISAO_ENVIO, REVISAO_REMOCAO, NOME_ORIGINAL, TIPO,
                                  TAMANHO_BYTES, PAGINAS, CAMINHO_INTERNO)
        VALUES (p_sol, p_rev_envio, p_rev_remocao, p_nome, p_tipo, p_bytes, p_paginas, p_caminho);
    END;

    -- Cria a analise de uma revisao e devolve o identificador. O resumo e SIMULADO (adaptador mockado).
    FUNCTION nova_analise(p_sol NUMBER, p_rev NUMBER, p_geral VARCHAR2, p_vigente NUMBER, p_resumo VARCHAR2)
        RETURN NUMBER IS
        v_id NUMBER;
    BEGIN
        INSERT INTO RC_ANALISE (SOLICITACAO_ID, REVISAO, PROTOCOLO_ID, RESULTADO_GERAL, VIGENTE, SIMULADO,
                                RESUMO_TEXTO, RESUMO_ORIGEM)
        VALUES (p_sol, p_rev, v_protocolo, p_geral, p_vigente, 1, p_resumo, 'ADAPTADOR_MOCKADO')
        RETURNING ID INTO v_id;
        RETURN v_id;
    END;

    -- Registra o resultado de uma regra e, se for PENDENTE ou NAO_VERIFICAVEL, a pendencia correspondente.
    PROCEDURE resultado(p_analise NUMBER, p_regra NUMBER, p_res VARCHAR2) IS
        v_msg  VARCHAR2(400 CHAR);
        v_resp VARCHAR2(20 CHAR);
    BEGIN
        INSERT INTO RC_RESULTADO_REGRA (ANALISE_ID, REGRA_ID, RESULTADO) VALUES (p_analise, p_regra, p_res);
        IF p_res = 'PENDENTE' THEN
            SELECT MENSAGEM, RESPONSAVEL INTO v_msg, v_resp FROM RC_REGRA WHERE ID = p_regra;
            INSERT INTO RC_PENDENCIA (ANALISE_ID, REGRA_ID, TIPO, MENSAGEM, RESPONSAVEL)
            VALUES (p_analise, p_regra, 'PENDENTE', v_msg, v_resp);
        ELSIF p_res = 'NAO_VERIFICAVEL' THEN
            INSERT INTO RC_PENDENCIA (ANALISE_ID, REGRA_ID, TIPO, MENSAGEM, RESPONSAVEL)
            VALUES (p_analise, p_regra, 'NAO_VERIFICAVEL', c_msg_nv, 'PACIENTE');
        END IF;
    END;

    -- Registra os seis resultados de uma analise na ordem R01..R06.
    PROCEDURE resultados(p_analise NUMBER, p1 VARCHAR2, p2 VARCHAR2, p3 VARCHAR2, p4 VARCHAR2, p5 VARCHAR2,
                         p6 VARCHAR2) IS
    BEGIN
        resultado(p_analise, v_r01, p1);
        resultado(p_analise, v_r02, p2);
        resultado(p_analise, v_r03, p3);
        resultado(p_analise, v_r04, p4);
        resultado(p_analise, v_r05, p5);
        resultado(p_analise, v_r06, p6);
    END;
BEGIN
    -- ------------------------------------------------------------------
    -- Contas (hash BCrypt custo 12). Falha aqui se a carga ja foi feita (ORA-00001).
    -- ------------------------------------------------------------------
    INSERT INTO RC_USUARIO (EMAIL, NOME, SENHA_HASH, PERFIL)
    VALUES ('paciente.a@exemplo.invalid', 'Paciente A (fictícia)',
            '$2a$12$Vq2hwuKC.UIpLJdePpPae.c4VuTZ1k7ACu.wr2A88/suhOdeglm7q', 'PACIENTE')
    RETURNING ID INTO v_paciente_a;
    INSERT INTO RC_USUARIO (EMAIL, NOME, SENHA_HASH, PERFIL)
    VALUES ('paciente.b@exemplo.invalid', 'Paciente B (fictícia)',
            '$2a$12$hWjfsYX/eYjwi3gwFL0lh.02rbmDjsJ3OVKiEh6/VO5X4FZqi9X56', 'PACIENTE')
    RETURNING ID INTO v_paciente_b;
    INSERT INTO RC_USUARIO (EMAIL, NOME, SENHA_HASH, PERFIL)
    VALUES ('admin@exemplo.invalid', 'Administrador (fictício)',
            '$2a$12$8wVg27NH5l.T.r5stYvqM.sYtvrpBtvCRtLwmZyMvtJaMF2Twdju.', 'ADMINISTRADOR')
    RETURNING ID INTO v_admin;

    -- ------------------------------------------------------------------
    -- Protocolo academico ficticio v0.1 e suas 6 regras (docs/protocolo-academico.md).
    -- ------------------------------------------------------------------
    INSERT INTO RC_PROTOCOLO (IDENTIFICADOR, VERSAO, LOCALIDADE, ESPECIALIDADE, AVISO_FICTICIO)
    VALUES ('PROTOCOLO-ACADEMICO-FICTICIO', 'v0.1', 'Município Exemplo', 'Consulta ambulatorial em cardiologia',
            'Documento acadêmico fictício. Não é exigência oficial de nenhum serviço de saúde ou do SUS.')
    RETURNING ID INTO v_protocolo;

    v_r01 := nova_regra('RC-R01', 'Encaminhamento presente',
        'Documento de encaminhamento para a especialidade', 'OBRIGATORIA', NULL,
        'Não encontramos o encaminhamento. Envie o documento emitido pela sua unidade.', 'PACIENTE', 1);
    v_r02 := nova_regra('RC-R02', 'Identificação coerente entre documentos',
        'Nome e data de nascimento iguais nos documentos enviados', 'OBRIGATORIA', NULL,
        'Nome ou data de nascimento diferem entre os documentos. Confira os dados.', 'PACIENTE', 2);
    v_r03 := nova_regra('RC-R03', 'Documento de identificação',
        'Documento de identificação ou cartão do SUS (sintéticos) legível', 'OBRIGATORIA', NULL,
        'Envie um documento de identificação ou o cartão do SUS.', 'PACIENTE', 3);
    v_r04 := nova_regra('RC-R04', 'Motivo do encaminhamento informado',
        'Campo de motivo/justificativa preenchido no encaminhamento', 'OBRIGATORIA', NULL,
        'O encaminhamento não traz o motivo. Peça à unidade emissora que complete o documento.',
        'UNIDADE_EMISSORA', 4);
    v_r05 := nova_regra('RC-R05', 'Encaminhamento dentro da validade',
        'Data de emissão com no máximo 180 dias de antiguidade (validade demonstrativa)', 'OBRIGATORIA', NULL,
        'A data do encaminhamento está fora da validade deste protocolo de teste. Peça um novo à unidade emissora.',
        'UNIDADE_EMISSORA', 5);
    v_r06 := nova_regra('RC-R06', 'Autorização do responsável legal',
        'Autorização assinada do responsável', 'CONDICIONAL', 'Paciente com menos de 18 anos na data do envio',
        'Para pacientes menores de 18 anos, envie a autorização do responsável.', 'PACIENTE', 6);

    -- ------------------------------------------------------------------
    -- Pedido 1 (paciente A): corrigido entre as revisoes 1 e 2 e submetido em simulacao.
    -- Rev 1: so o encaminhamento -> R02 nao verificavel, R03 pendente.
    -- Rev 2: documento de identificacao enviado -> sem pendencias.
    -- ------------------------------------------------------------------
    v_sol1 := nova_solicitacao(v_paciente_a, 'SUBMETIDA_SIMULACAO', 2);
    nova_revisao(v_sol1, 1, 'CRIACAO');
    nova_revisao(v_sol1, 2, 'DOCUMENTO_ENVIADO');
    novo_documento(v_sol1, 1, NULL, 'encaminhamento_sintetico_a.pdf', 'PDF', 182400, 1, 'sim/doc-0001.dat');
    novo_documento(v_sol1, 2, NULL, 'identificacao_sintetica_a.png', 'PNG', 96300, 1, 'sim/doc-0002.dat');
    v_an := nova_analise(v_sol1, 1, 'COM_PENDENCIAS', 0,
        '[SIMULADO] Falta o documento de identificação e a identificação entre documentos não pôde ser conferida.');
    resultados(v_an, 'ATENDE', 'NAO_VERIFICAVEL', 'PENDENTE', 'ATENDE', 'ATENDE', 'NAO_APLICAVEL');
    v_an_vigente_sol1 := nova_analise(v_sol1, 2, 'SEM_PENDENCIAS_IDENTIFICADAS', 1,
        '[SIMULADO] Nenhuma pendência identificada nesta revisão. Isso não significa aprovação do SUS.');
    resultados(v_an_vigente_sol1, 'ATENDE', 'ATENDE', 'ATENDE', 'ATENDE', 'ATENDE', 'NAO_APLICAVEL');
    INSERT INTO RC_SUBMISSAO (SOLICITACAO_ID, ANALISE_ID, REFERENCIA)
    VALUES (v_sol1, v_an_vigente_sol1, 'SIM-2026-0001');

    -- ------------------------------------------------------------------
    -- Pedido 2 (paciente A): com pendencias a corrigir pela unidade emissora.
    -- ------------------------------------------------------------------
    v_sol2 := nova_solicitacao(v_paciente_a, 'COM_PENDENCIAS', 1);
    nova_revisao(v_sol2, 1, 'CRIACAO');
    novo_documento(v_sol2, 1, NULL, 'encaminhamento_sintetico_a2.pdf', 'PDF', 204800, 2, 'sim/doc-0003.dat');
    novo_documento(v_sol2, 1, NULL, 'identificacao_sintetica_a2.jpg', 'JPEG', 88200, 1, 'sim/doc-0004.dat');
    v_an := nova_analise(v_sol2, 1, 'COM_PENDENCIAS', 1,
        '[SIMULADO] O encaminhamento não traz o motivo e a data está fora da validade deste protocolo de teste.');
    resultados(v_an, 'ATENDE', 'ATENDE', 'ATENDE', 'PENDENTE', 'PENDENTE', 'NAO_APLICAVEL');

    -- ------------------------------------------------------------------
    -- Pedido 3 (paciente B): imagem ilegivel substituida; ainda requer conferencia.
    -- Rev 1: duas regras nao verificaveis mais a condicional indeterminada.
    -- Rev 2: documento substituido -> sobra R03 nao verificavel.
    -- ------------------------------------------------------------------
    v_sol3 := nova_solicitacao(v_paciente_b, 'REQUER_CONFERENCIA', 2);
    nova_revisao(v_sol3, 1, 'CRIACAO');
    nova_revisao(v_sol3, 2, 'DOCUMENTO_SUBSTITUIDO');
    novo_documento(v_sol3, 1, NULL, 'encaminhamento_sintetico_b.pdf', 'PDF', 150000, 1, 'sim/doc-0005.dat');
    novo_documento(v_sol3, 1, 2, 'identificacao_borrada_b.jpg', 'JPEG', 45000, 1, 'sim/doc-0006.dat');
    novo_documento(v_sol3, 2, NULL, 'identificacao_nitida_b.jpg', 'JPEG', 120000, 1, 'sim/doc-0007.dat');
    v_an := nova_analise(v_sol3, 1, 'REQUER_CONFERENCIA', 0,
        '[SIMULADO] Algumas informações do documento de identificação não puderam ser lidas.');
    resultados(v_an, 'ATENDE', 'NAO_VERIFICAVEL', 'NAO_VERIFICAVEL', 'ATENDE', 'ATENDE', 'NAO_VERIFICAVEL');
    v_an := nova_analise(v_sol3, 2, 'REQUER_CONFERENCIA', 1,
        '[SIMULADO] Ainda há uma informação que não pôde ser lida. Confira a transcrição ou envie um arquivo mais nítido.');
    resultados(v_an, 'ATENDE', 'ATENDE', 'NAO_VERIFICAVEL', 'ATENDE', 'ATENDE', 'NAO_APLICAVEL');

    -- ------------------------------------------------------------------
    -- Pedido 4 (paciente A): rascunho, com sessao de captura e leituras dos dois sensores.
    -- ------------------------------------------------------------------
    v_sol4 := nova_solicitacao(v_paciente_a, 'RASCUNHO', 1);
    nova_revisao(v_sol4, 1, 'CRIACAO');
    novo_documento(v_sol4, 1, NULL, 'encaminhamento_sintetico_a4.pdf', 'PDF', 130000, 1, 'sim/doc-0008.dat');
    INSERT INTO RC_SESSAO_CAPTURA (SOLICITACAO_ID) VALUES (v_sol4) RETURNING ID INTO v_sessao;
    INSERT INTO RC_LEITURA_SENSOR (SESSAO_ID, TIPO, VALOR, UNIDADE, MEDIDA_EM)
    VALUES (v_sessao, 'LUMINOSIDADE', 320.5, 'LUX', TIMESTAMP '2026-11-12 14:00:00 +00:00');
    INSERT INTO RC_LEITURA_SENSOR (SESSAO_ID, TIPO, VALOR, UNIDADE, MEDIDA_EM)
    VALUES (v_sessao, 'DISTANCIA', 28.0, 'CM', TIMESTAMP '2026-11-12 14:00:01 +00:00');
    INSERT INTO RC_LEITURA_SENSOR (SESSAO_ID, TIPO, VALOR, UNIDADE, MEDIDA_EM)
    VALUES (v_sessao, 'LUMINOSIDADE', 410.0, 'LUX', TIMESTAMP '2026-11-12 14:00:05 +00:00');
    INSERT INTO RC_LEITURA_SENSOR (SESSAO_ID, TIPO, VALOR, UNIDADE, MEDIDA_EM)
    VALUES (v_sessao, 'DISTANCIA', 31.5, 'CM', TIMESTAMP '2026-11-12 14:00:06 +00:00');

    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Carga sintetica concluida: 3 contas, 1 protocolo, 6 regras, 4 pedidos.');
END;
/
