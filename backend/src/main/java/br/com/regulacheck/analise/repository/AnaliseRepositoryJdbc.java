package br.com.regulacheck.analise.repository;

import br.com.regulacheck.analise.entity.Analise;
import br.com.regulacheck.analise.entity.DocumentoDaAnalise;
import br.com.regulacheck.analise.entity.EvidenciaGravada;
import br.com.regulacheck.analise.entity.HistoricoDeAnalises;
import br.com.regulacheck.analise.entity.PendenciaGravada;
import br.com.regulacheck.analise.entity.ResultadoGravado;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.config.BancoDados;
import br.com.regulacheck.documento.entity.TipoDocumental;
import br.com.regulacheck.integracao.extracao.CodigoCampo;
import br.com.regulacheck.protocolo.entity.ResponsavelCorrecao;
import br.com.regulacheck.solicitacao.entity.EstadoSolicitacao;
import br.com.regulacheck.validacao.model.Evidencia;
import br.com.regulacheck.validacao.model.MotivoNaoVerificavel;
import br.com.regulacheck.validacao.model.Pendencia;
import br.com.regulacheck.validacao.model.Resultado;
import br.com.regulacheck.validacao.model.ResultadoDeRegra;
import br.com.regulacheck.validacao.model.ResultadoGeral;
import br.com.regulacheck.validacao.model.SituacaoEvidencia;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Persistencia de {@code RC_ANALISE}, {@code RC_RESULTADO_REGRA}, {@code RC_PENDENCIA},
 * {@code RC_PENDENCIA_EVIDENCIA} e da analise em andamento ({@code RC_SOLICITACAO.ESTADO = 'PROCESSANDO'} com
 * {@code ANALISE_INICIADA_EM}) por JDBC, com SQL parametrizado e instantes em UTC.
 *
 * <p>Toda operacao filtra a dona do pedido na propria instrucao (regra de dona da RC-05). As escritas sao
 * transacoes curtas, sem chamada externa: {@code SELECT ... FOR UPDATE} da linha do pedido (o mesmo bloqueio das
 * escritas de documentos, leitura e conferencia), conferencias, {@code COMMIT} so depois de todos os passos e
 * {@code ROLLBACK} explicito em qualquer falha. Os logs trazem apenas identificadores, codigos e contagens, nunca
 * valor, trecho ou nome de arquivo.
 */
public final class AnaliseRepositoryJdbc implements AnaliseRepository {

    private static final Logger LOG = LoggerFactory.getLogger(AnaliseRepositoryJdbc.class);

    /** Codigo do Oracle para "tabela ou visao inexistente": esquema sem o script 10. */
    private static final int ORA_TABELA_INEXISTENTE = 942;
    /** Codigo do Oracle para "identificador invalido" (coluna inexistente): esquema sem o script 10. */
    private static final int ORA_COLUNA_INEXISTENTE = 904;

    // So colunas de RC_SOLICITACAO e filtro pela dona: FOR UPDATE nao se aplica a juncao.
    private static final String SQL_BLOQUEAR = """
            SELECT ESTADO, REVISAO_ATUAL, ANALISE_INICIADA_EM
              FROM RC_SOLICITACAO
             WHERE ID = ? AND PACIENTE_ID = ?
               FOR UPDATE""";
    private static final String SQL_PEDIDO = """
            SELECT ESTADO, REVISAO_ATUAL, ANALISE_INICIADA_EM FROM RC_SOLICITACAO WHERE ID = ? AND PACIENTE_ID = ?""";
    private static final String SQL_ALTERAR_ESTADO = """
            UPDATE RC_SOLICITACAO SET ESTADO = ?, ANALISE_INICIADA_EM = ?, ATUALIZADA_EM = ? WHERE ID = ?""";
    private static final String SQL_VIGENTE_DA_REVISAO = """
            SELECT ID FROM RC_ANALISE WHERE SOLICITACAO_ID = ? AND VIGENTE = 1 AND REVISAO = ?""";
    private static final String SQL_LEITURA_DA_REVISAO = """
            SELECT COUNT(*) FROM RC_EXTRACAO WHERE SOLICITACAO_ID = ? AND REVISAO = ?""";
    // Mesma definicao de ConferenciaService.visao: ultima decisao da leitura ou, sem decisao, o instante da leitura.
    private static final String SQL_CONFERENCIA_ATUAL = """
            SELECT e.REVISAO, e.CRIADA_EM,
                   (SELECT MAX(c.DECIDIDA_EM)
                      FROM RC_CONFERENCIA_CAMPO c
                      JOIN RC_EXTRACAO_DOCUMENTO ed ON ed.ID = c.EXTRACAO_DOCUMENTO_ID
                     WHERE ed.EXTRACAO_ID = e.ID) AS DECIDIDA_EM
              FROM RC_EXTRACAO e
             WHERE e.ID = ? AND e.SOLICITACAO_ID = ?""";
    private static final String SQL_MAIS_RECENTE = """
            SELECT RESULTADO_GERAL FROM RC_ANALISE WHERE SOLICITACAO_ID = ?
             ORDER BY CRIADA_EM DESC, ID DESC
             FETCH FIRST 1 ROW ONLY""";
    private static final String SQL_REGRAS = "SELECT ID, CODIGO FROM RC_REGRA WHERE PROTOCOLO_ID = ?";
    private static final String SQL_TIRAR_VIGENCIA = """
            UPDATE RC_ANALISE SET VIGENTE = 0 WHERE SOLICITACAO_ID = ? AND VIGENTE = 1""";
    private static final String SQL_INSERIR_ANALISE = """
            INSERT INTO RC_ANALISE (SOLICITACAO_ID, REVISAO, PROTOCOLO_ID, RESULTADO_GERAL, VIGENTE, SIMULADO, CRIADA_EM,
                                    CONFERENCIA_EM, DATA_REFERENCIA)
            VALUES (?, ?, ?, ?, 1, 1, ?, ?, ?)""";
    private static final String SQL_INSERIR_RESULTADO = """
            INSERT INTO RC_RESULTADO_REGRA (ANALISE_ID, REGRA_ID, RESULTADO) VALUES (?, ?, ?)""";
    private static final String SQL_INSERIR_PENDENCIA = """
            INSERT INTO RC_PENDENCIA (ANALISE_ID, REGRA_ID, TIPO, MENSAGEM, RESPONSAVEL, MOTIVO)
            VALUES (?, ?, ?, ?, ?, ?)""";
    private static final String SQL_INSERIR_EVIDENCIA = """
            INSERT INTO RC_PENDENCIA_EVIDENCIA (ANALISE_ID, REGRA_ID, ORDEM, DOCUMENTO_ID, CODIGO, SITUACAO)
            VALUES (?, ?, ?, ?, ?, ?)""";

    // Leitura do historico. As analises carregadas sao as N mais recentes do pedido, ou uma so (somenteId); a mesma
    // selecao filtra o cabecalho e as consultas auxiliares, para que so as linhas das analises da resposta sejam
    // lidas. Parametros de ANALISES_SELECIONADAS, nesta ordem: pedido, id (ou nulo), id (ou nulo), limite.
    private static final String ANALISES_SELECIONADAS = """
            SELECT ID FROM (SELECT ID FROM RC_ANALISE
                             WHERE SOLICITACAO_ID = ? AND (? IS NULL OR ID = ?)
                             ORDER BY CRIADA_EM DESC, ID DESC
                             FETCH FIRST ? ROWS ONLY)""";
    private static final String SQL_ANALISES = """
            SELECT a.ID, a.REVISAO, a.VIGENTE, a.CRIADA_EM, a.CONFERENCIA_EM, a.DATA_REFERENCIA, a.RESULTADO_GERAL,
                   p.IDENTIFICADOR, p.VERSAO, a.RESUMO_TEXTO, a.RESUMO_ORIGEM
              FROM RC_ANALISE a
              JOIN RC_PROTOCOLO p ON p.ID = a.PROTOCOLO_ID""" + " WHERE a.ID IN (" + ANALISES_SELECIONADAS
            + ") ORDER BY a.CRIADA_EM DESC, a.ID DESC";
    private static final String SQL_RESULTADOS = """
            SELECT rr.ANALISE_ID, r.CODIGO, r.DESCRICAO, rr.RESULTADO, pe.MOTIVO
              FROM RC_RESULTADO_REGRA rr
              JOIN RC_REGRA r ON r.ID = rr.REGRA_ID
              LEFT JOIN RC_PENDENCIA pe ON pe.ANALISE_ID = rr.ANALISE_ID AND pe.REGRA_ID = rr.REGRA_ID""" + " WHERE rr.ANALISE_ID IN (" + ANALISES_SELECIONADAS
            + ") ORDER BY rr.ANALISE_ID, r.ORDEM, r.CODIGO";
    private static final String SQL_PENDENCIAS = """
            SELECT pe.ANALISE_ID, pe.REGRA_ID, r.CODIGO, r.DESCRICAO, pe.TIPO, pe.MENSAGEM, pe.RESPONSAVEL, pe.MOTIVO
              FROM RC_PENDENCIA pe
              JOIN RC_REGRA r ON r.ID = pe.REGRA_ID""" + " WHERE pe.ANALISE_ID IN (" + ANALISES_SELECIONADAS
            + ") ORDER BY pe.ANALISE_ID, r.ORDEM, r.CODIGO";
    // A origem vem da leitura da mesma revisao (uma por revisao, nunca alterada): o trecho nao e copiado.
    private static final String SQL_EVIDENCIAS = """
            SELECT ev.ANALISE_ID, ev.REGRA_ID, ev.DOCUMENTO_ID, ev.CODIGO, ev.SITUACAO, c.PAGINA, c.TRECHO
              FROM RC_PENDENCIA_EVIDENCIA ev
              JOIN RC_ANALISE a ON a.ID = ev.ANALISE_ID
              LEFT JOIN RC_EXTRACAO e ON e.SOLICITACAO_ID = a.SOLICITACAO_ID AND e.REVISAO = a.REVISAO
              LEFT JOIN RC_EXTRACAO_DOCUMENTO ed ON ed.EXTRACAO_ID = e.ID AND ed.DOCUMENTO_ID = ev.DOCUMENTO_ID
              LEFT JOIN RC_EXTRACAO_CAMPO c ON c.EXTRACAO_DOCUMENTO_ID = ed.ID AND c.CODIGO = ev.CODIGO""" + " WHERE ev.ANALISE_ID IN (" + ANALISES_SELECIONADAS
            + ") ORDER BY ev.ANALISE_ID, ev.REGRA_ID, ev.ORDEM";
    // O historico le o pedido e as analises num unico retrato do banco (consistencia de leitura do Oracle).
    private static final String SQL_SOMENTE_LEITURA = "SET TRANSACTION READ ONLY";
    private static final String SQL_DOCUMENTOS = """
            SELECT ID, NOME_ORIGINAL, TIPO_DOCUMENTAL, REVISAO_ENVIO, REVISAO_REMOCAO
              FROM RC_DOCUMENTO
             WHERE SOLICITACAO_ID = ?
             ORDER BY ENVIADO_EM, ID""";
    private static final String SQL_RESUMO_ATUAL = """
            SELECT RESUMO_ORIGEM FROM RC_ANALISE
             WHERE ID = ? AND SOLICITACAO_ID = ? AND VIGENTE = 1""";
    private static final String SQL_GUARDAR_RESUMO = """
            UPDATE RC_ANALISE SET RESUMO_TEXTO = ?, RESUMO_ORIGEM = ?
             WHERE ID = ? AND SOLICITACAO_ID = ? AND VIGENTE = 1""";

    private final BancoDados banco;

    /** @param banco acesso ao Oracle */
    public AnaliseRepositoryJdbc(BancoDados banco) {
        this.banco = banco;
    }

    @Override
    public InicioAnalise iniciar(long pacienteId, long solicitacaoId, Instant agora, Duration limite) {
        return emTransacao(conexao -> iniciarBloqueado(conexao, pacienteId, solicitacaoId, agora, limite));
    }

    @Override
    public PublicacaoAnalise publicar(long pacienteId, long solicitacaoId, Instant iniciadaEm,
            AnaliseParaGravar analise, Instant agora, Duration limite) {
        return emTransacao(conexao -> publicarBloqueado(conexao, pacienteId, solicitacaoId, iniciadaEm, analise,
                agora, limite));
    }

    @Override
    public boolean encerrar(long pacienteId, long solicitacaoId, Instant iniciadaEm, Encerramento tipo,
            Instant agora) {
        return emTransacao(conexao -> {
            Pedido pedido = bloquear(conexao, pacienteId, solicitacaoId);
            if (pedido == null || !pedido.emAndamentoPor(iniciadaEm)) {
                return false;
            }
            EstadoSolicitacao estado = tipo == Encerramento.FALHA ? EstadoSolicitacao.FALHA_ANALISE
                    : estadoDepoisDeDescarte(conexao, solicitacaoId);
            alterarEstado(conexao, solicitacaoId, estado, null, agora);
            return true;
        });
    }

    @Override
    public Optional<HistoricoDeAnalises> historico(long pacienteId, long solicitacaoId, int limite) {
        try (Connection conexao = banco.conexao()) {
            // Uma transacao so de leitura: o estado do pedido e as analises vem do mesmo instante, sem bloqueio. Sem
            // isso, uma publicacao confirmada entre as consultas poderia mostrar PROCESSANDO junto da analise nova.
            conexao.setAutoCommit(false);
            try {
                Optional<HistoricoDeAnalises> historico = historicoNoRetrato(conexao, pacienteId, solicitacaoId,
                        limite);
                conexao.commit();
                return historico;
            } catch (SQLException | RuntimeException falha) {
                desfazer(conexao);
                throw traduzir(falha);
            }
        } catch (SQLException falha) {
            throw traduzir(falha);
        }
    }

    @Override
    public Optional<Analise> guardarResumo(long pacienteId, long solicitacaoId, long analiseId, String texto,
            String origem) {
        return emTransacao(conexao -> {
            Pedido pedido = bloquear(conexao, pacienteId, solicitacaoId);
            if (pedido == null) {
                return Optional.empty();
            }
            String origemAtual;
            try (PreparedStatement consulta = conexao.prepareStatement(SQL_RESUMO_ATUAL)) {
                consulta.setLong(1, analiseId);
                consulta.setLong(2, solicitacaoId);
                try (ResultSet linha = consulta.executeQuery()) {
                    if (!linha.next()) {
                        return Optional.empty();
                    }
                    origemAtual = linha.getString(1);
                }
            }
            if (!"ADAPTADOR_MOCKADO".equals(origemAtual)) {
                try (PreparedStatement atualizacao = conexao.prepareStatement(SQL_GUARDAR_RESUMO)) {
                    atualizacao.setString(1, texto);
                    atualizacao.setString(2, origem);
                    atualizacao.setLong(3, analiseId);
                    atualizacao.setLong(4, solicitacaoId);
                    atualizacao.executeUpdate();
                }
            }
            return Optional.of(carregar(conexao, solicitacaoId, analiseId, 1).get(0));
        });
    }

    private static Optional<HistoricoDeAnalises> historicoNoRetrato(Connection conexao, long pacienteId,
            long solicitacaoId, int limite) throws SQLException {
        try (PreparedStatement somenteLeitura = conexao.prepareStatement(SQL_SOMENTE_LEITURA)) {
            somenteLeitura.execute();
        }
        EstadoSolicitacao estado;
        int revisaoAtual;
        Instant iniciadaEm;
        try (PreparedStatement consulta = conexao.prepareStatement(SQL_PEDIDO)) {
            consulta.setLong(1, solicitacaoId);
            consulta.setLong(2, pacienteId);
            try (ResultSet linha = consulta.executeQuery()) {
                if (!linha.next()) {
                    return Optional.empty();
                }
                estado = EstadoSolicitacao.valueOf(linha.getString("ESTADO"));
                revisaoAtual = linha.getInt("REVISAO_ATUAL");
                iniciadaEm = instanteOuNulo(linha, "ANALISE_INICIADA_EM");
            }
        }
        List<Analise> analises = carregar(conexao, solicitacaoId, null, limite);
        return Optional.of(new HistoricoDeAnalises(estado, revisaoAtual, iniciadaEm, analises));
    }

    // ---------------------------------------------------------------------------------------------------------
    // Escrita
    // ---------------------------------------------------------------------------------------------------------

    private InicioAnalise iniciarBloqueado(Connection conexao, long pacienteId, long solicitacaoId, Instant agora,
            Duration limite) throws SQLException {
        Pedido pedido = bloquear(conexao, pacienteId, solicitacaoId);
        if (pedido == null) {
            return InicioAnalise.naoEncontrado();
        }
        if (pedido.estado() == EstadoSolicitacao.SUBMETIDA_SIMULACAO) {
            return InicioAnalise.submetido();
        }
        boolean interrompeu = false;
        if (pedido.estado() == EstadoSolicitacao.PROCESSANDO) {
            if (pedido.iniciadaEm() != null && agora.isBefore(pedido.iniciadaEm().plus(limite))) {
                return InicioAnalise.emAndamento(pedido.iniciadaEm());
            }
            // Passou do limite sem concluir (a requisicao travou ou caiu): e uma falha recuperavel. O processamento
            // antigo, se ainda terminar, encontra outro instante de inicio e nao publica nada.
            alterarEstado(conexao, solicitacaoId, EstadoSolicitacao.FALHA_ANALISE, null, agora);
            interrompeu = true;
        }
        Long vigente = vigenteDaRevisao(conexao, solicitacaoId, pedido.revisaoAtual());
        if (vigente != null) {
            return InicioAnalise.vigente(carregar(conexao, solicitacaoId, vigente, 1).get(0), interrompeu);
        }
        if (!temLeitura(conexao, solicitacaoId, pedido.revisaoAtual())) {
            return InicioAnalise.semLeituraAtual(interrompeu);
        }
        alterarEstado(conexao, solicitacaoId, EstadoSolicitacao.PROCESSANDO, agora, agora);
        return InicioAnalise.iniciada(agora, interrompeu);
    }

    private PublicacaoAnalise publicarBloqueado(Connection conexao, long pacienteId, long solicitacaoId,
            Instant iniciadaEm, AnaliseParaGravar analise, Instant agora, Duration limite) throws SQLException {
        Pedido pedido = bloquear(conexao, pacienteId, solicitacaoId);
        if (pedido == null) {
            return PublicacaoAnalise.naoEncontrado();
        }
        if (!pedido.emAndamentoPor(iniciadaEm)) {
            return PublicacaoAnalise.substituida();
        }
        if (!agora.isBefore(iniciadaEm.plus(limite))) {
            // Passou do limite sem ninguem assumir: e uma analise interrompida, nao publicada (FR-018). Esta ainda e a
            // dona do PROCESSANDO, entao o libera como falha recuperavel, como a tomada da trava faria.
            alterarEstado(conexao, solicitacaoId, EstadoSolicitacao.FALHA_ANALISE, null, agora);
            return PublicacaoAnalise.prazoEsgotado();
        }
        // Revisao e conferencia relidas sob o bloqueio: nenhuma escrita de documento, leitura ou conferencia do mesmo
        // pedido pode acontecer entre esta conferencia e o COMMIT.
        if (pedido.revisaoAtual() != analise.revisao()
                || !analise.conferenciaEm().equals(conferenciaAtual(conexao, solicitacaoId, analise))) {
            alterarEstado(conexao, solicitacaoId, estadoDepoisDeDescarte(conexao, solicitacaoId), null, agora);
            return PublicacaoAnalise.entradaMudou();
        }
        Map<String, Long> regras = regrasDaVersao(conexao, analise.protocoloId());
        executar(conexao, SQL_TIRAR_VIGENCIA, solicitacaoId);
        long analiseId = inserirAnalise(conexao, solicitacaoId, analise, agora);
        inserirResultados(conexao, analiseId, regras, analise.avaliacao().resultados());
        inserirPendencias(conexao, analiseId, regras, analise.avaliacao().pendencias());
        alterarEstado(conexao, solicitacaoId,
                EstadoSolicitacao.valueOf(analise.avaliacao().resultadoGeral().name()), null, agora);
        return PublicacaoAnalise.publicada(carregar(conexao, solicitacaoId, analiseId, 1).get(0));
    }

    /** Retrato do pedido lido sob bloqueio. */
    private record Pedido(EstadoSolicitacao estado, int revisaoAtual, Instant iniciadaEm) {

        /** @return {@code true} se a analise em andamento e a que comecou em {@code inicio} */
        boolean emAndamentoPor(Instant inicio) {
            return estado == EstadoSolicitacao.PROCESSANDO && inicio.equals(iniciadaEm);
        }
    }

    private static Pedido bloquear(Connection conexao, long pacienteId, long solicitacaoId) throws SQLException {
        try (PreparedStatement bloqueio = conexao.prepareStatement(SQL_BLOQUEAR)) {
            bloqueio.setLong(1, solicitacaoId);
            bloqueio.setLong(2, pacienteId);
            try (ResultSet linha = bloqueio.executeQuery()) {
                if (!linha.next()) {
                    return null;
                }
                return new Pedido(EstadoSolicitacao.valueOf(linha.getString("ESTADO")), linha.getInt("REVISAO_ATUAL"),
                        instanteOuNulo(linha, "ANALISE_INICIADA_EM"));
            }
        }
    }

    private static void alterarEstado(Connection conexao, long solicitacaoId, EstadoSolicitacao estado,
            Instant iniciadaEm, Instant agora) throws SQLException {
        try (PreparedStatement comando = conexao.prepareStatement(SQL_ALTERAR_ESTADO)) {
            comando.setString(1, estado.name());
            if (iniciadaEm == null) {
                comando.setNull(2, Types.TIMESTAMP_WITH_TIMEZONE);
            } else {
                comando.setObject(2, utc(iniciadaEm));
            }
            comando.setObject(3, utc(agora));
            comando.setLong(4, solicitacaoId);
            comando.executeUpdate();
        }
    }

    private static Long vigenteDaRevisao(Connection conexao, long solicitacaoId, int revisao) throws SQLException {
        try (PreparedStatement consulta = conexao.prepareStatement(SQL_VIGENTE_DA_REVISAO)) {
            consulta.setLong(1, solicitacaoId);
            consulta.setInt(2, revisao);
            try (ResultSet linha = consulta.executeQuery()) {
                return linha.next() ? linha.getLong("ID") : null;
            }
        }
    }

    private static boolean temLeitura(Connection conexao, long solicitacaoId, int revisao) throws SQLException {
        try (PreparedStatement consulta = conexao.prepareStatement(SQL_LEITURA_DA_REVISAO)) {
            consulta.setLong(1, solicitacaoId);
            consulta.setInt(2, revisao);
            try (ResultSet linha = consulta.executeQuery()) {
                linha.next();
                return linha.getInt(1) > 0;
            }
        }
    }

    /**
     * Instante atual da conferencia da leitura avaliada; {@code null} se a leitura nao existe mais ou nao e da
     * revisao avaliada (nao combina com nenhum instante, entao a publicacao e descartada).
     */
    private static Instant conferenciaAtual(Connection conexao, long solicitacaoId, AnaliseParaGravar analise)
            throws SQLException {
        try (PreparedStatement consulta = conexao.prepareStatement(SQL_CONFERENCIA_ATUAL)) {
            consulta.setLong(1, analise.extracaoId());
            consulta.setLong(2, solicitacaoId);
            try (ResultSet linha = consulta.executeQuery()) {
                if (!linha.next() || linha.getInt("REVISAO") != analise.revisao()) {
                    return null;
                }
                Instant decidida = instanteOuNulo(linha, "DECIDIDA_EM");
                return decidida != null ? decidida : instanteOuNulo(linha, "CRIADA_EM");
            }
        }
    }

    /** Resultado geral da analise concluida mais recente, ou {@code RASCUNHO} se o pedido nunca foi analisado. */
    private static EstadoSolicitacao estadoDepoisDeDescarte(Connection conexao, long solicitacaoId)
            throws SQLException {
        try (PreparedStatement consulta = conexao.prepareStatement(SQL_MAIS_RECENTE)) {
            consulta.setLong(1, solicitacaoId);
            try (ResultSet linha = consulta.executeQuery()) {
                return linha.next() ? EstadoSolicitacao.valueOf(linha.getString("RESULTADO_GERAL"))
                        : EstadoSolicitacao.RASCUNHO;
            }
        }
    }

    private static Map<String, Long> regrasDaVersao(Connection conexao, long protocoloId) throws SQLException {
        Map<String, Long> regras = new HashMap<>();
        try (PreparedStatement consulta = conexao.prepareStatement(SQL_REGRAS)) {
            consulta.setLong(1, protocoloId);
            try (ResultSet linha = consulta.executeQuery()) {
                while (linha.next()) {
                    regras.put(linha.getString("CODIGO"), linha.getLong("ID"));
                }
            }
        }
        return regras;
    }

    private static long regra(Map<String, Long> regras, String codigo) {
        Long id = regras.get(codigo);
        if (id == null) {
            // O motor so avalia regras lidas desta mesma versao: codigo ausente e erro de programacao (rollback).
            throw new IllegalStateException("Regra da avaliacao ausente na versao do protocolo.");
        }
        return id;
    }

    private static long inserirAnalise(Connection conexao, long solicitacaoId, AnaliseParaGravar analise,
            Instant agora) throws SQLException {
        try (PreparedStatement insercao = conexao.prepareStatement(SQL_INSERIR_ANALISE, new String[] {"ID"})) {
            insercao.setLong(1, solicitacaoId);
            insercao.setInt(2, analise.revisao());
            insercao.setLong(3, analise.protocoloId());
            insercao.setString(4, analise.avaliacao().resultadoGeral().name());
            insercao.setObject(5, utc(agora));
            // CONFERENCIA_EM e TIMESTAMP sem fuso (faz parte da chave unica): grava a hora UTC.
            insercao.setObject(6, LocalDateTime.ofInstant(analise.conferenciaEm(), ZoneOffset.UTC));
            insercao.setObject(7, analise.dataReferencia());
            insercao.executeUpdate();
            try (ResultSet chaves = insercao.getGeneratedKeys()) {
                if (!chaves.next()) {
                    throw new IllegalStateException("Identificador da analise nao devolvido.");
                }
                return chaves.getLong(1);
            }
        }
    }

    private static void inserirResultados(Connection conexao, long analiseId, Map<String, Long> regras,
            List<ResultadoDeRegra> resultados) throws SQLException {
        try (PreparedStatement insercao = conexao.prepareStatement(SQL_INSERIR_RESULTADO)) {
            for (ResultadoDeRegra resultado : resultados) {
                insercao.setLong(1, analiseId);
                insercao.setLong(2, regra(regras, resultado.regra().codigo()));
                insercao.setString(3, resultado.resultado().name());
                insercao.addBatch();
            }
            insercao.executeBatch();
        }
    }

    private static void inserirPendencias(Connection conexao, long analiseId, Map<String, Long> regras,
            List<Pendencia> pendencias) throws SQLException {
        if (pendencias.isEmpty()) {
            return;
        }
        try (PreparedStatement pendencia = conexao.prepareStatement(SQL_INSERIR_PENDENCIA);
                PreparedStatement evidencia = conexao.prepareStatement(SQL_INSERIR_EVIDENCIA)) {
            boolean temEvidencia = false;
            for (Pendencia item : pendencias) {
                long regraId = regra(regras, item.codigoRegra());
                pendencia.setLong(1, analiseId);
                pendencia.setLong(2, regraId);
                pendencia.setString(3, item.tipo().name());
                pendencia.setString(4, item.explicacao());
                pendencia.setString(5, item.responsavel().name());
                pendencia.setString(6, item.motivo() == null ? null : item.motivo().name());
                pendencia.addBatch();
                int ordem = 1;
                for (Evidencia prova : item.evidencias()) {
                    evidencia.setLong(1, analiseId);
                    evidencia.setLong(2, regraId);
                    evidencia.setInt(3, ordem++);
                    evidencia.setLong(4, prova.documentoId());
                    evidencia.setString(5, prova.codigo().name());
                    evidencia.setString(6, prova.situacao().name());
                    evidencia.addBatch();
                    temEvidencia = true;
                }
            }
            // As pendencias antes das evidencias: a FK da evidencia aponta para a pendencia.
            pendencia.executeBatch();
            if (temEvidencia) {
                evidencia.executeBatch();
            }
        }
    }

    private static void executar(Connection conexao, String sql, long solicitacaoId) throws SQLException {
        try (PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setLong(1, solicitacaoId);
            comando.executeUpdate();
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Leitura
    // ---------------------------------------------------------------------------------------------------------

    /** Cabecalho de uma analise antes de juntar resultados, pendencias e documentos. */
    private record Cabecalho(long id, int revisao, boolean vigente, Instant executadaEm, Instant conferenciaEm,
            LocalDate dataReferencia, ResultadoGeral resultadoGeral, String identificador, String versao,
            String resumoTexto, String resumoOrigem) {
    }

    /** Documento do pedido com o intervalo de revisoes em que esteve disponivel. */
    private record DocumentoDoPedido(long id, String nome, TipoDocumental tipo, int revisaoEnvio,
            Integer revisaoRemocao) {

        boolean fazParteDa(int revisao) {
            return revisaoEnvio <= revisao && (revisaoRemocao == null || revisaoRemocao > revisao);
        }
    }

    /** Quais analises carregar: as {@code limite} mais recentes do pedido ou so a {@code somenteId}. */
    private record Selecao(long solicitacaoId, Long somenteId, int limite) {

        /** Preenche, a partir de {@code primeiro}, os quatro parametros de {@link #ANALISES_SELECIONADAS}. */
        void aplicar(PreparedStatement consulta, int primeiro) throws SQLException {
            consulta.setLong(primeiro, solicitacaoId);
            if (somenteId == null) {
                consulta.setNull(primeiro + 1, Types.NUMERIC);
                consulta.setNull(primeiro + 2, Types.NUMERIC);
            } else {
                consulta.setLong(primeiro + 1, somenteId);
                consulta.setLong(primeiro + 2, somenteId);
            }
            consulta.setInt(primeiro + 3, limite);
        }
    }

    /**
     * Carrega as analises do pedido (uma, se {@code somenteId} vier preenchido), da mais recente para a mais antiga.
     * Resultados, pendencias e evidencias vem em uma consulta cada, so das analises selecionadas; documentos, em uma
     * consulta por pedido.
     */
    private static List<Analise> carregar(Connection conexao, long solicitacaoId, Long somenteId, int limite)
            throws SQLException {
        Selecao selecao = new Selecao(solicitacaoId, somenteId, somenteId == null ? limite : 1);
        List<Cabecalho> cabecalhos = cabecalhos(conexao, selecao);
        if (cabecalhos.isEmpty()) {
            return List.of();
        }
        Map<Long, List<ResultadoGravado>> resultados = resultados(conexao, selecao);
        Map<Long, Map<Long, List<EvidenciaGravada>>> evidencias = evidencias(conexao, selecao);
        Map<Long, List<PendenciaGravada>> pendencias = pendencias(conexao, selecao, evidencias);
        List<DocumentoDoPedido> documentos = documentos(conexao, solicitacaoId);
        List<Analise> analises = new ArrayList<>();
        for (Cabecalho c : cabecalhos) {
            List<DocumentoDaAnalise> avaliados = new ArrayList<>();
            for (DocumentoDoPedido documento : documentos) {
                if (documento.fazParteDa(c.revisao())) {
                    avaliados.add(new DocumentoDaAnalise(documento.id(), documento.nome(), documento.tipo(),
                            documento.revisaoRemocao() == null));
                }
            }
            analises.add(new Analise(c.id(), c.revisao(), c.vigente(), c.executadaEm(), c.identificador(),
                    c.versao(), c.conferenciaEm(), c.dataReferencia(), c.resultadoGeral(), avaliados,
                    resultados.getOrDefault(c.id(), List.of()), pendencias.getOrDefault(c.id(), List.of()),
                    c.resumoTexto(), c.resumoOrigem()));
        }
        return analises;
    }

    private static List<Cabecalho> cabecalhos(Connection conexao, Selecao selecao) throws SQLException {
        List<Cabecalho> cabecalhos = new ArrayList<>();
        try (PreparedStatement consulta = conexao.prepareStatement(SQL_ANALISES)) {
            selecao.aplicar(consulta, 1);
            try (ResultSet linha = consulta.executeQuery()) {
                while (linha.next()) {
                    LocalDateTime conferencia = linha.getObject("CONFERENCIA_EM", LocalDateTime.class);
                    cabecalhos.add(new Cabecalho(linha.getLong("ID"), linha.getInt("REVISAO"),
                            linha.getInt("VIGENTE") == 1, instanteOuNulo(linha, "CRIADA_EM"),
                            conferencia == null ? null : conferencia.toInstant(ZoneOffset.UTC),
                            linha.getObject("DATA_REFERENCIA", LocalDate.class),
                            ResultadoGeral.valueOf(linha.getString("RESULTADO_GERAL")),
                            linha.getString("IDENTIFICADOR"), linha.getString("VERSAO"),
                            linha.getString("RESUMO_TEXTO"), linha.getString("RESUMO_ORIGEM")));
                }
            }
        }
        return cabecalhos;
    }

    private static Map<Long, List<ResultadoGravado>> resultados(Connection conexao, Selecao selecao)
            throws SQLException {
        Map<Long, List<ResultadoGravado>> porAnalise = new HashMap<>();
        try (PreparedStatement consulta = conexao.prepareStatement(SQL_RESULTADOS)) {
            selecao.aplicar(consulta, 1);
            try (ResultSet linha = consulta.executeQuery()) {
                while (linha.next()) {
                    porAnalise.computeIfAbsent(linha.getLong("ANALISE_ID"), id -> new ArrayList<>()).add(
                            new ResultadoGravado(linha.getString("CODIGO"), linha.getString("DESCRICAO"),
                                    Resultado.valueOf(linha.getString("RESULTADO")), motivo(linha)));
                }
            }
        }
        return porAnalise;
    }

    private static Map<Long, List<PendenciaGravada>> pendencias(Connection conexao, Selecao selecao,
            Map<Long, Map<Long, List<EvidenciaGravada>>> evidencias) throws SQLException {
        Map<Long, List<PendenciaGravada>> porAnalise = new HashMap<>();
        try (PreparedStatement consulta = conexao.prepareStatement(SQL_PENDENCIAS)) {
            selecao.aplicar(consulta, 1);
            try (ResultSet linha = consulta.executeQuery()) {
                while (linha.next()) {
                    long analiseId = linha.getLong("ANALISE_ID");
                    List<EvidenciaGravada> daPendencia = evidencias.getOrDefault(analiseId, Map.of())
                            .getOrDefault(linha.getLong("REGRA_ID"), List.of());
                    porAnalise.computeIfAbsent(analiseId, id -> new ArrayList<>()).add(new PendenciaGravada(
                            linha.getString("CODIGO"), linha.getString("DESCRICAO"),
                            Resultado.valueOf(linha.getString("TIPO")), linha.getString("MENSAGEM"), motivo(linha),
                            ResponsavelCorrecao.valueOf(linha.getString("RESPONSAVEL")), daPendencia));
                }
            }
        }
        return porAnalise;
    }

    /** Evidencias por analise e por regra, na ordem gravada. */
    private static Map<Long, Map<Long, List<EvidenciaGravada>>> evidencias(Connection conexao, Selecao selecao)
            throws SQLException {
        Map<Long, Map<Long, List<EvidenciaGravada>>> porAnalise = new HashMap<>();
        try (PreparedStatement consulta = conexao.prepareStatement(SQL_EVIDENCIAS)) {
            selecao.aplicar(consulta, 1);
            try (ResultSet linha = consulta.executeQuery()) {
                while (linha.next()) {
                    Integer pagina = linha.getObject("PAGINA") == null ? null : linha.getInt("PAGINA");
                    porAnalise.computeIfAbsent(linha.getLong("ANALISE_ID"), id -> new LinkedHashMap<>())
                            .computeIfAbsent(linha.getLong("REGRA_ID"), id -> new ArrayList<>())
                            .add(new EvidenciaGravada(linha.getLong("DOCUMENTO_ID"),
                                    CodigoCampo.valueOf(linha.getString("CODIGO")),
                                    SituacaoEvidencia.valueOf(linha.getString("SITUACAO")), pagina,
                                    linha.getString("TRECHO")));
                }
            }
        }
        return porAnalise;
    }

    private static List<DocumentoDoPedido> documentos(Connection conexao, long solicitacaoId) throws SQLException {
        List<DocumentoDoPedido> documentos = new ArrayList<>();
        try (PreparedStatement consulta = conexao.prepareStatement(SQL_DOCUMENTOS)) {
            consulta.setLong(1, solicitacaoId);
            try (ResultSet linha = consulta.executeQuery()) {
                while (linha.next()) {
                    Integer remocao = linha.getObject("REVISAO_REMOCAO") == null ? null
                            : linha.getInt("REVISAO_REMOCAO");
                    documentos.add(new DocumentoDoPedido(linha.getLong("ID"), linha.getString("NOME_ORIGINAL"),
                            TipoDocumental.valueOf(linha.getString("TIPO_DOCUMENTAL")), linha.getInt("REVISAO_ENVIO"),
                            remocao));
                }
            }
        }
        return documentos;
    }

    private static MotivoNaoVerificavel motivo(ResultSet linha) throws SQLException {
        String motivo = linha.getString("MOTIVO");
        return motivo == null ? null : MotivoNaoVerificavel.valueOf(motivo);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Infraestrutura
    // ---------------------------------------------------------------------------------------------------------

    /** Passo de uma transacao. */
    @FunctionalInterface
    private interface Passo<T> {
        T executar(Connection conexao) throws SQLException;
    }

    /**
     * Executa o passo numa transacao: {@code COMMIT} tambem nos desfechos sem escrita (libera o bloqueio da linha) e
     * {@code ROLLBACK} explicito em qualquer falha, para nunca deixar analise pela metade.
     */
    private <T> T emTransacao(Passo<T> passo) {
        try (Connection conexao = banco.conexao()) {
            conexao.setAutoCommit(false);
            try {
                T resultado = passo.executar(conexao);
                conexao.commit();
                return resultado;
            } catch (SQLException | RuntimeException falha) {
                desfazer(conexao);
                throw traduzir(falha);
            }
        } catch (SQLException falha) {
            throw traduzir(falha);
        }
    }

    private static Instant instanteOuNulo(ResultSet linha, String coluna) throws SQLException {
        OffsetDateTime valor = linha.getObject(coluna, OffsetDateTime.class);
        return valor == null ? null : valor.toInstant();
    }

    private static void desfazer(Connection conexao) {
        try {
            conexao.rollback();
        } catch (SQLException falha) {
            LOG.warn("Falha ao desfazer transacao. tipo={} codigoFornecedor={}",
                    falha.getClass().getSimpleName(), falha.getErrorCode());
        }
    }

    private static OffsetDateTime utc(Instant instante) {
        return OffsetDateTime.ofInstant(instante, ZoneOffset.UTC);
    }

    /**
     * Traduz uma falha. Tabela ou coluna inexistente (ORA-00942/ORA-00904) indica esquema sem o script 10: so as
     * rotas de analise ficam indisponiveis (503), com a orientacao apenas no log. Demais falhas de banco viram 503;
     * erros de programacao seguem como estao (500 generico, sem detalhe na resposta).
     */
    private static RuntimeException traduzir(Exception falha) {
        if (falha instanceof ExcecaoNegocio negocio) {
            return negocio;
        }
        if (falha instanceof SQLException sql) {
            if (sql.getErrorCode() == ORA_TABELA_INEXISTENTE || sql.getErrorCode() == ORA_COLUNA_INEXISTENTE) {
                LOG.error("Esquema sem as alteracoes de analise. codigoFornecedor={} "
                        + "(execute database/scripts/10_analise_historico.sql)", sql.getErrorCode());
            } else {
                LOG.warn("Falha ao acessar analises. tipo={} codigoFornecedor={}",
                        sql.getClass().getSimpleName(), sql.getErrorCode());
            }
            return new ExcecaoNegocio(503, "DEPENDENCIA_INDISPONIVEL",
                    "Servico temporariamente indisponivel. Tente novamente em instantes.");
        }
        return (RuntimeException) falha;
    }
}
