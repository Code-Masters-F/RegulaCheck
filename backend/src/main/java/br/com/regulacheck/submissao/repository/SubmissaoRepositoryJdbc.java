package br.com.regulacheck.submissao.repository;

import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.config.BancoDados;
import br.com.regulacheck.submissao.entity.Submissao;
import br.com.regulacheck.submissao.repository.ResultadoSubmissao.Situacao;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Persistencia de {@code RC_SUBMISSAO} por JDBC, com SQL parametrizado e instantes em UTC (RC-17).
 *
 * <p>A conclusao e uma transacao curta sob o bloqueio da linha do pedido ({@code SELECT … FOR UPDATE} em
 * {@code RC_SOLICITACAO}), o mesmo de envio, substituicao e remocao de documento, decisao de conferencia e analise.
 * Assim a vigencia da analise, a revisao e o estado conferidos sao os mesmos do instante da gravacao: uma alteracao
 * de documento ou de conferencia que tire a vigencia espera esta transacao ou e vista por ela. {@code COMMIT} so
 * depois de gravar a submissao e mudar o estado; {@code ROLLBACK} explicito em qualquer falha. A restricao
 * {@code RC_SUBMISSAO_UK_SOLIC} e a rede de protecao contra uma segunda submissao do mesmo pedido.
 *
 * <p>Logs so com ids e desfechos; nunca conteudo de documento.
 */
public final class SubmissaoRepositoryJdbc implements SubmissaoRepository {

    private static final Logger LOG = LoggerFactory.getLogger(SubmissaoRepositoryJdbc.class);

    /** Codigo do Oracle para violacao de chave/indice unico (ORA-00001). */
    private static final int ORA_UNICO_VIOLADO = 1;

    private static final String SQL_BLOQUEAR = """
            SELECT ESTADO, REVISAO_ATUAL FROM RC_SOLICITACAO WHERE ID = ? AND PACIENTE_ID = ? FOR UPDATE""";
    private static final String SQL_SUBMISSAO = """
            SELECT s.ID, s.SOLICITACAO_ID, s.ANALISE_ID, s.REFERENCIA, s.SUBMETIDA_EM, a.REVISAO,
                   p.IDENTIFICADOR, p.VERSAO, p.LOCALIDADE, p.ESPECIALIDADE, p.AVISO_FICTICIO
              FROM RC_SUBMISSAO s
              JOIN RC_SOLICITACAO sol ON sol.ID = s.SOLICITACAO_ID
              JOIN RC_ANALISE a ON a.ID = s.ANALISE_ID
              JOIN RC_PROTOCOLO p ON p.ID = a.PROTOCOLO_ID
             WHERE s.SOLICITACAO_ID = ? AND sol.PACIENTE_ID = ?""";
    // Resultados em aberto contados por defesa: o resultado geral SEM_PENDENCIAS_IDENTIFICADAS ja os exclui.
    private static final String SQL_ANALISE_VIGENTE = """
            SELECT a.ID, a.RESULTADO_GERAL,
                   (SELECT COUNT(*) FROM RC_RESULTADO_REGRA rr
                     WHERE rr.ANALISE_ID = a.ID AND rr.RESULTADO IN ('PENDENTE', 'NAO_VERIFICAVEL')) AS EM_ABERTO
              FROM RC_ANALISE a
             WHERE a.SOLICITACAO_ID = ? AND a.VIGENTE = 1 AND a.REVISAO = ?""";
    private static final String SQL_INSERIR = """
            INSERT INTO RC_SUBMISSAO (SOLICITACAO_ID, ANALISE_ID, REFERENCIA, SIMULADA, SUBMETIDA_EM)
            VALUES (?, ?, ?, 1, ?)""";
    private static final String SQL_SUBMETER_PEDIDO = """
            UPDATE RC_SOLICITACAO SET ESTADO = 'SUBMETIDA_SIMULACAO', ATUALIZADA_EM = ? WHERE ID = ?""";

    private final BancoDados banco;

    /** @param banco pool de conexoes do Oracle */
    public SubmissaoRepositoryJdbc(BancoDados banco) {
        this.banco = banco;
    }

    @Override
    public ResultadoSubmissao submeter(long pacienteId, long solicitacaoId, String referencia, Instant agora) {
        try (Connection conexao = banco.conexao()) {
            conexao.setAutoCommit(false);
            try {
                ResultadoSubmissao resultado = submeterBloqueado(conexao, pacienteId, solicitacaoId, referencia,
                        agora);
                // COMMIT tambem nos desfechos sem escrita: libera o bloqueio da linha do pedido.
                conexao.commit();
                return resultado;
            } catch (SQLIntegrityConstraintViolationException duplicada) {
                desfazer(conexao);
                if (duplicada.getErrorCode() != ORA_UNICO_VIOLADO) {
                    throw indisponivel(duplicada);
                }
                // Uma submissao entrou por fora do bloqueio (carga manual): a operacao e idempotente, devolve ela.
                LOG.info("Submissao ja registrada por outro caminho. solicitacaoId={}", solicitacaoId);
                return ResultadoSubmissao.jaSubmetida(lerSubmissao(conexao, pacienteId, solicitacaoId)
                        .orElseThrow(() -> indisponivel(duplicada)));
            } catch (SQLException | RuntimeException falha) {
                desfazer(conexao);
                throw traduzir(falha);
            }
        } catch (SQLException falha) {
            throw indisponivel(falha);
        }
    }

    private ResultadoSubmissao submeterBloqueado(Connection conexao, long pacienteId, long solicitacaoId,
            String referencia, Instant agora) throws SQLException {
        String estado;
        int revisaoAtual;
        try (PreparedStatement bloqueio = conexao.prepareStatement(SQL_BLOQUEAR)) {
            bloqueio.setLong(1, solicitacaoId);
            bloqueio.setLong(2, pacienteId);
            try (ResultSet linha = bloqueio.executeQuery()) {
                if (!linha.next()) {
                    return ResultadoSubmissao.sem(Situacao.NAO_ENCONTRADO);
                }
                estado = linha.getString("ESTADO");
                revisaoAtual = linha.getInt("REVISAO_ATUAL");
            }
        }
        Optional<Submissao> existente = lerSubmissao(conexao, pacienteId, solicitacaoId);
        if (existente.isPresent()) {
            return ResultadoSubmissao.jaSubmetida(existente.get());
        }
        if ("SUBMETIDA_SIMULACAO".equals(estado)) {
            return ResultadoSubmissao.sem(Situacao.SOMENTE_CONSULTA);
        }
        if ("PROCESSANDO".equals(estado)) {
            return ResultadoSubmissao.sem(Situacao.EM_ANDAMENTO);
        }
        long analiseId;
        try (PreparedStatement consulta = conexao.prepareStatement(SQL_ANALISE_VIGENTE)) {
            consulta.setLong(1, solicitacaoId);
            consulta.setInt(2, revisaoAtual);
            try (ResultSet linha = consulta.executeQuery()) {
                if (!linha.next()) {
                    // Sem vigente da revisao atual: nunca submete sobre analise que deixou de valer.
                    return ResultadoSubmissao.sem(Situacao.SEM_ANALISE_VIGENTE);
                }
                if (!"SEM_PENDENCIAS_IDENTIFICADAS".equals(linha.getString("RESULTADO_GERAL"))
                        || linha.getInt("EM_ABERTO") > 0) {
                    return ResultadoSubmissao.sem(Situacao.COM_PENDENCIAS);
                }
                if (!"SEM_PENDENCIAS_IDENTIFICADAS".equals(estado)) {
                    // Estado incoerente com a vigente (dado montado fora do fluxo): so a transicao prevista e aceita.
                    return ResultadoSubmissao.sem(Situacao.SEM_ANALISE_VIGENTE);
                }
                analiseId = linha.getLong("ID");
            }
        }
        try (PreparedStatement inserir = conexao.prepareStatement(SQL_INSERIR)) {
            inserir.setLong(1, solicitacaoId);
            inserir.setLong(2, analiseId);
            inserir.setString(3, referencia);
            inserir.setObject(4, utc(agora));
            inserir.executeUpdate();
        }
        try (PreparedStatement submeter = conexao.prepareStatement(SQL_SUBMETER_PEDIDO)) {
            submeter.setObject(1, utc(agora));
            submeter.setLong(2, solicitacaoId);
            submeter.executeUpdate();
        }
        Submissao gravada = lerSubmissao(conexao, pacienteId, solicitacaoId)
                .orElseThrow(() -> new SQLException("Submissao recem-gravada nao foi encontrada."));
        LOG.info("Submissao simulada registrada. solicitacaoId={} submissaoId={} analiseId={}", solicitacaoId,
                gravada.id(), analiseId);
        return ResultadoSubmissao.submetida(gravada);
    }

    @Override
    public Optional<Submissao> buscar(long pacienteId, long solicitacaoId) {
        try (Connection conexao = banco.conexao()) {
            return lerSubmissao(conexao, pacienteId, solicitacaoId);
        } catch (SQLException falha) {
            throw indisponivel(falha);
        }
    }

    private static Optional<Submissao> lerSubmissao(Connection conexao, long pacienteId, long solicitacaoId)
            throws SQLException {
        try (PreparedStatement consulta = conexao.prepareStatement(SQL_SUBMISSAO)) {
            consulta.setLong(1, solicitacaoId);
            consulta.setLong(2, pacienteId);
            try (ResultSet linha = consulta.executeQuery()) {
                if (!linha.next()) {
                    return Optional.empty();
                }
                return Optional.of(new Submissao(linha.getLong("ID"), linha.getLong("SOLICITACAO_ID"),
                        linha.getLong("ANALISE_ID"), linha.getString("REFERENCIA"),
                        linha.getObject("SUBMETIDA_EM", OffsetDateTime.class).toInstant(), linha.getInt("REVISAO"),
                        linha.getString("IDENTIFICADOR"), linha.getString("VERSAO"), linha.getString("LOCALIDADE"),
                        linha.getString("ESPECIALIDADE"), linha.getString("AVISO_FICTICIO")));
            }
        }
    }

    private static OffsetDateTime utc(Instant instante) {
        return OffsetDateTime.ofInstant(instante, ZoneOffset.UTC);
    }

    private static void desfazer(Connection conexao) {
        try {
            conexao.rollback();
        } catch (SQLException falha) {
            LOG.warn("Falha ao desfazer transacao. tipo={} codigoFornecedor={}",
                    falha.getClass().getSimpleName(), falha.getErrorCode());
        }
    }

    private static RuntimeException traduzir(Exception falha) {
        if (falha instanceof ExcecaoNegocio negocio) {
            return negocio;
        }
        if (falha instanceof SQLException sql) {
            return indisponivel(sql);
        }
        return (RuntimeException) falha;
    }

    private static ExcecaoNegocio indisponivel(SQLException falha) {
        // Detalhe do driver so no log (sem parametros).
        LOG.warn("Falha ao acessar submissoes. tipo={} codigoFornecedor={}", falha.getClass().getSimpleName(),
                falha.getErrorCode());
        return new ExcecaoNegocio(503, "DEPENDENCIA_INDISPONIVEL",
                "Servico temporariamente indisponivel. Tente novamente em instantes.");
    }
}
