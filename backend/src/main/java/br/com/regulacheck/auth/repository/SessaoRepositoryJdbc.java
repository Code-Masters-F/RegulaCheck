package br.com.regulacheck.auth.repository;

import br.com.regulacheck.auth.entity.Sessao;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.config.BancoDados;
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

/** Persistencia de {@code RC_SESSAO_LOGIN} por JDBC, com SQL parametrizado e instantes em UTC. */
public final class SessaoRepositoryJdbc implements SessaoRepository {

    private static final Logger LOG = LoggerFactory.getLogger(SessaoRepositoryJdbc.class);

    private final BancoDados banco;

    /** @param banco acesso ao Oracle */
    public SessaoRepositoryJdbc(BancoDados banco) {
        this.banco = banco;
    }

    @Override
    public Sessao criar(long usuarioId, String tokenHash, Instant criadaEm, Instant expiraEm) {
        String sql = "INSERT INTO RC_SESSAO_LOGIN (USUARIO_ID, TOKEN_HASH, CRIADA_EM, EXPIRA_EM) VALUES (?, ?, ?, ?)";
        try (Connection conexao = banco.conexao();
                PreparedStatement comando = conexao.prepareStatement(sql, new String[] {"ID"})) {
            comando.setLong(1, usuarioId);
            comando.setString(2, tokenHash);
            comando.setObject(3, utc(criadaEm));
            comando.setObject(4, utc(expiraEm));
            comando.executeUpdate();
            try (ResultSet chaves = comando.getGeneratedKeys()) {
                if (!chaves.next()) {
                    throw new IllegalStateException("Sessao gravada sem identificador gerado.");
                }
                return new Sessao(chaves.getLong(1), usuarioId, criadaEm, expiraEm, null);
            }
        } catch (SQLIntegrityConstraintViolationException falha) {
            // Token repetido ou dado invalido e erro de programacao, nao indisponibilidade do banco.
            LOG.error("Violacao de integridade ao gravar sessao. codigoFornecedor={}", falha.getErrorCode());
            throw new IllegalStateException("Nao foi possivel gravar a sessao.");
        } catch (SQLException falha) {
            throw indisponivel(falha);
        }
    }

    @Override
    public Optional<Sessao> buscarPorTokenHash(String tokenHash) {
        String sql = "SELECT ID, USUARIO_ID, CRIADA_EM, EXPIRA_EM, ENCERRADA_EM FROM RC_SESSAO_LOGIN WHERE TOKEN_HASH = ?";
        try (Connection conexao = banco.conexao();
                PreparedStatement consulta = conexao.prepareStatement(sql)) {
            consulta.setString(1, tokenHash);
            try (ResultSet resultado = consulta.executeQuery()) {
                if (!resultado.next()) {
                    return Optional.empty();
                }
                OffsetDateTime encerrada = resultado.getObject("ENCERRADA_EM", OffsetDateTime.class);
                return Optional.of(new Sessao(
                        resultado.getLong("ID"),
                        resultado.getLong("USUARIO_ID"),
                        resultado.getObject("CRIADA_EM", OffsetDateTime.class).toInstant(),
                        resultado.getObject("EXPIRA_EM", OffsetDateTime.class).toInstant(),
                        encerrada == null ? null : encerrada.toInstant()));
            }
        } catch (SQLException falha) {
            throw indisponivel(falha);
        }
    }

    @Override
    public void encerrar(long sessaoId, Instant quando) {
        String sql = "UPDATE RC_SESSAO_LOGIN SET ENCERRADA_EM = ? WHERE ID = ? AND ENCERRADA_EM IS NULL";
        try (Connection conexao = banco.conexao();
                PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setObject(1, utc(quando));
            comando.setLong(2, sessaoId);
            comando.executeUpdate();
        } catch (SQLException falha) {
            throw indisponivel(falha);
        }
    }

    private static OffsetDateTime utc(Instant instante) {
        return OffsetDateTime.ofInstant(instante, ZoneOffset.UTC);
    }

    private static ExcecaoNegocio indisponivel(SQLException falha) {
        // Detalhe do driver so no log (sem parametros, que incluem o hash do token).
        LOG.warn("Falha ao acessar sessoes. tipo={} codigoFornecedor={}",
                falha.getClass().getSimpleName(), falha.getErrorCode());
        return new ExcecaoNegocio(503, "DEPENDENCIA_INDISPONIVEL",
                "Servico temporariamente indisponivel. Tente novamente em instantes.");
    }
}
