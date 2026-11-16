package br.com.regulacheck.iot.repository;

import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.config.BancoDados;
import br.com.regulacheck.iot.entity.LeituraSensor;
import br.com.regulacheck.iot.entity.SessaoCaptura;
import br.com.regulacheck.iot.entity.TipoSensor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Persistencia de {@code RC_SESSAO_CAPTURA} e {@code RC_LEITURA_SENSOR} por JDBC, com SQL parametrizado e
 * instantes em UTC (RC-21/RC-22).
 *
 * <p>As duas escritas usam uma transacao curta com o bloqueio da linha do pedido ({@code SELECT … FOR UPDATE} em
 * {@code RC_SOLICITACAO}), o mesmo usado por envio de documento, troca de servico, analise e exclusao do rascunho:
 * <ul>
 *   <li>abrir sessao: chamadas simultaneas do mesmo pedido sao serializadas, entao a segunda ve a sessao criada
 *       pela primeira (no maximo uma aberta; o indice {@code RC_SESSAO_CAPTURA_UK_ABERTA} e a rede de
 *       protecao);</li>
 *   <li>gravar leitura: uma leitura nunca entra entre o apagar das leituras e o das sessoes na exclusao do
 *       rascunho, que tornaria a exclusao impossivel (FK).</li>
 * </ul>
 * {@code COMMIT} so depois de todos os passos e {@code ROLLBACK} explicito em qualquer falha. Logs so com ids e
 * codigos: nunca valores de leitura nem a chave do simulador.
 */
public final class CapturaRepositoryJdbc implements CapturaRepository {

    private static final Logger LOG = LoggerFactory.getLogger(CapturaRepositoryJdbc.class);

    /** Codigo do Oracle para violacao de chave/indice unico (ORA-00001). */
    private static final int ORA_UNICO_VIOLADO = 1;

    private static final String SQL_BLOQUEAR_PEDIDO_DA_PACIENTE = """
            SELECT ESTADO FROM RC_SOLICITACAO WHERE ID = ? AND PACIENTE_ID = ? FOR UPDATE""";
    private static final String SQL_BLOQUEAR_PEDIDO = "SELECT ID FROM RC_SOLICITACAO WHERE ID = ? FOR UPDATE";
    private static final String SQL_SESSAO_ABERTA_DO_PEDIDO = """
            SELECT ID, SOLICITACAO_ID, INICIADA_EM, ENCERRADA_EM FROM RC_SESSAO_CAPTURA
             WHERE SOLICITACAO_ID = ? AND ENCERRADA_EM IS NULL""";
    private static final String SQL_INSERIR_SESSAO = """
            INSERT INTO RC_SESSAO_CAPTURA (SOLICITACAO_ID, INICIADA_EM) VALUES (?, ?)""";
    private static final String SQL_PEDIDO_DA_SESSAO = """
            SELECT SOLICITACAO_ID FROM RC_SESSAO_CAPTURA WHERE ID = ?""";
    private static final String SQL_SESSAO_ABERTA = """
            SELECT COUNT(*) FROM RC_SESSAO_CAPTURA WHERE ID = ? AND ENCERRADA_EM IS NULL""";
    private static final String SQL_INSERIR_LEITURA = """
            INSERT INTO RC_LEITURA_SENSOR (SESSAO_ID, TIPO, VALOR, UNIDADE, MEDIDA_EM, RECEBIDA_EM)
            VALUES (?, ?, ?, ?, ?, ?)""";
    private static final String SQL_PEDIDO_DA_PACIENTE = """
            SELECT ID FROM RC_SOLICITACAO WHERE ID = ? AND PACIENTE_ID = ?""";
    private static final String SQL_SESSAO_MAIS_RECENTE = """
            SELECT ID, SOLICITACAO_ID, INICIADA_EM, ENCERRADA_EM FROM RC_SESSAO_CAPTURA
             WHERE SOLICITACAO_ID = ?
             ORDER BY INICIADA_EM DESC, ID DESC FETCH FIRST 1 ROW ONLY""";
    private static final String SQL_ULTIMA_LEITURA = """
            SELECT ID, SESSAO_ID, TIPO, VALOR, MEDIDA_EM, RECEBIDA_EM FROM RC_LEITURA_SENSOR
             WHERE SESSAO_ID = ? AND TIPO = ?
             ORDER BY MEDIDA_EM DESC, ID DESC FETCH FIRST 1 ROW ONLY""";

    private final BancoDados banco;

    /**
     * @param banco pool de conexoes do Oracle
     */
    public CapturaRepositoryJdbc(BancoDados banco) {
        this.banco = banco;
    }

    @Override
    public AberturaSessao abrirSessao(long solicitacaoId, long pacienteId, Instant agora) {
        try (Connection conexao = banco.conexao()) {
            conexao.setAutoCommit(false);
            try {
                AberturaSessao abertura = abrirBloqueado(conexao, solicitacaoId, pacienteId, agora);
                // COMMIT tambem nos desfechos sem escrita: libera o bloqueio da linha do pedido.
                conexao.commit();
                return abertura;
            } catch (SQLIntegrityConstraintViolationException duplicada) {
                desfazer(conexao);
                if (duplicada.getErrorCode() != ORA_UNICO_VIOLADO) {
                    throw indisponivel(duplicada);
                }
                // Outra sessao aberta entrou por fora do bloqueio (carga manual): devolve a que ficou.
                LOG.info("Sessao de captura ja aberta por outro caminho. solicitacaoId={}", solicitacaoId);
                return AberturaSessao.existente(sessaoAberta(conexao, solicitacaoId)
                        .orElseThrow(() -> indisponivel(duplicada)));
            } catch (SQLException | RuntimeException falha) {
                desfazer(conexao);
                throw traduzir(falha);
            }
        } catch (SQLException falha) {
            throw indisponivel(falha);
        }
    }

    private AberturaSessao abrirBloqueado(Connection conexao, long solicitacaoId, long pacienteId, Instant agora)
            throws SQLException {
        String estado;
        try (PreparedStatement bloqueio = conexao.prepareStatement(SQL_BLOQUEAR_PEDIDO_DA_PACIENTE)) {
            bloqueio.setLong(1, solicitacaoId);
            bloqueio.setLong(2, pacienteId);
            try (ResultSet linha = bloqueio.executeQuery()) {
                if (!linha.next()) {
                    return AberturaSessao.naoEncontrado();
                }
                estado = linha.getString("ESTADO");
            }
        }
        if ("SUBMETIDA_SIMULACAO".equals(estado)) {
            return AberturaSessao.submetido();
        }
        Optional<SessaoCaptura> aberta = sessaoAberta(conexao, solicitacaoId);
        if (aberta.isPresent()) {
            return AberturaSessao.existente(aberta.get());
        }
        try (PreparedStatement comando = conexao.prepareStatement(SQL_INSERIR_SESSAO, new String[] {"ID"})) {
            comando.setLong(1, solicitacaoId);
            comando.setObject(2, utc(agora));
            comando.executeUpdate();
            try (ResultSet chaves = comando.getGeneratedKeys()) {
                if (!chaves.next()) {
                    throw new SQLException("Identificador da sessao de captura nao devolvido.");
                }
                return AberturaSessao.criada(new SessaoCaptura(chaves.getLong(1), solicitacaoId, agora, null));
            }
        }
    }

    @Override
    public boolean sessaoAbertaExiste(long sessaoId) {
        try (Connection conexao = banco.conexao();
                PreparedStatement consulta = conexao.prepareStatement(SQL_SESSAO_ABERTA)) {
            consulta.setLong(1, sessaoId);
            try (ResultSet linha = consulta.executeQuery()) {
                return linha.next() && linha.getInt(1) > 0;
            }
        } catch (SQLException falha) {
            throw indisponivel(falha);
        }
    }

    @Override
    public Optional<LeituraSensor> gravarLeitura(NovaLeitura nova) {
        try (Connection conexao = banco.conexao()) {
            conexao.setAutoCommit(false);
            try {
                Optional<LeituraSensor> gravada = gravarBloqueado(conexao, nova);
                conexao.commit();
                return gravada;
            } catch (SQLException | RuntimeException falha) {
                desfazer(conexao);
                throw traduzir(falha);
            }
        } catch (SQLException falha) {
            throw indisponivel(falha);
        }
    }

    private Optional<LeituraSensor> gravarBloqueado(Connection conexao, NovaLeitura nova) throws SQLException {
        long solicitacaoId;
        // O pedido de uma sessao nunca muda: basta descobri-lo e bloquear a linha dele.
        try (PreparedStatement consulta = conexao.prepareStatement(SQL_PEDIDO_DA_SESSAO)) {
            consulta.setLong(1, nova.sessaoId());
            try (ResultSet linha = consulta.executeQuery()) {
                if (!linha.next()) {
                    return Optional.empty();
                }
                solicitacaoId = linha.getLong(1);
            }
        }
        try (PreparedStatement bloqueio = conexao.prepareStatement(SQL_BLOQUEAR_PEDIDO)) {
            bloqueio.setLong(1, solicitacaoId);
            try (ResultSet linha = bloqueio.executeQuery()) {
                if (!linha.next()) {
                    return Optional.empty();
                }
            }
        }
        // Conferida de novo sob o bloqueio: a sessao pode ter sido apagada com o rascunho ou encerrada.
        try (PreparedStatement consulta = conexao.prepareStatement(SQL_SESSAO_ABERTA)) {
            consulta.setLong(1, nova.sessaoId());
            try (ResultSet linha = consulta.executeQuery()) {
                if (!linha.next() || linha.getInt(1) == 0) {
                    return Optional.empty();
                }
            }
        }
        try (PreparedStatement comando = conexao.prepareStatement(SQL_INSERIR_LEITURA, new String[] {"ID"})) {
            comando.setLong(1, nova.sessaoId());
            comando.setString(2, nova.tipo().name());
            comando.setBigDecimal(3, nova.valor());
            comando.setString(4, nova.tipo().unidade());
            comando.setObject(5, utc(nova.medidaEm()));
            comando.setObject(6, utc(nova.recebidaEm()));
            comando.executeUpdate();
            try (ResultSet chaves = comando.getGeneratedKeys()) {
                if (!chaves.next()) {
                    throw new SQLException("Identificador da leitura nao devolvido.");
                }
                return Optional.of(new LeituraSensor(chaves.getLong(1), nova.sessaoId(), nova.tipo(), nova.valor(),
                        nova.medidaEm(), nova.recebidaEm()));
            }
        }
    }

    @Override
    public Optional<TelemetriaDoPedido> telemetriaDaPaciente(long solicitacaoId, long pacienteId) {
        try (Connection conexao = banco.conexao()) {
            try (PreparedStatement consulta = conexao.prepareStatement(SQL_PEDIDO_DA_PACIENTE)) {
                consulta.setLong(1, solicitacaoId);
                consulta.setLong(2, pacienteId);
                try (ResultSet linha = consulta.executeQuery()) {
                    if (!linha.next()) {
                        return Optional.empty();
                    }
                }
            }
            SessaoCaptura sessao;
            try (PreparedStatement consulta = conexao.prepareStatement(SQL_SESSAO_MAIS_RECENTE)) {
                consulta.setLong(1, solicitacaoId);
                try (ResultSet linha = consulta.executeQuery()) {
                    if (!linha.next()) {
                        return Optional.of(TelemetriaDoPedido.semSessao());
                    }
                    sessao = lerSessao(linha);
                }
            }
            Map<TipoSensor, LeituraSensor> ultimas = new EnumMap<>(TipoSensor.class);
            for (TipoSensor tipo : TipoSensor.values()) {
                try (PreparedStatement consulta = conexao.prepareStatement(SQL_ULTIMA_LEITURA)) {
                    consulta.setLong(1, sessao.id());
                    consulta.setString(2, tipo.name());
                    try (ResultSet linha = consulta.executeQuery()) {
                        if (linha.next()) {
                            ultimas.put(tipo, lerLeitura(linha));
                        }
                    }
                }
            }
            return Optional.of(new TelemetriaDoPedido(sessao, ultimas));
        } catch (SQLException falha) {
            throw indisponivel(falha);
        }
    }

    private static Optional<SessaoCaptura> sessaoAberta(Connection conexao, long solicitacaoId) throws SQLException {
        try (PreparedStatement consulta = conexao.prepareStatement(SQL_SESSAO_ABERTA_DO_PEDIDO)) {
            consulta.setLong(1, solicitacaoId);
            try (ResultSet linha = consulta.executeQuery()) {
                return linha.next() ? Optional.of(lerSessao(linha)) : Optional.empty();
            }
        }
    }

    private static SessaoCaptura lerSessao(ResultSet linha) throws SQLException {
        OffsetDateTime encerrada = linha.getObject("ENCERRADA_EM", OffsetDateTime.class);
        return new SessaoCaptura(linha.getLong("ID"), linha.getLong("SOLICITACAO_ID"),
                linha.getObject("INICIADA_EM", OffsetDateTime.class).toInstant(),
                encerrada == null ? null : encerrada.toInstant());
    }

    private static LeituraSensor lerLeitura(ResultSet linha) throws SQLException {
        return new LeituraSensor(linha.getLong("ID"), linha.getLong("SESSAO_ID"),
                TipoSensor.valueOf(linha.getString("TIPO")), linha.getBigDecimal("VALOR"),
                linha.getObject("MEDIDA_EM", OffsetDateTime.class).toInstant(),
                linha.getObject("RECEBIDA_EM", OffsetDateTime.class).toInstant());
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
        // Detalhe do driver so no log (sem parametros). ORA-00942/00904: tabela ou coluna ausente no banco.
        LOG.warn("Falha ao acessar sessoes de captura. tipo={} codigoFornecedor={}",
                falha.getClass().getSimpleName(), falha.getErrorCode());
        return new ExcecaoNegocio(503, "DEPENDENCIA_INDISPONIVEL",
                "Servico temporariamente indisponivel. Tente novamente em instantes.");
    }
}
