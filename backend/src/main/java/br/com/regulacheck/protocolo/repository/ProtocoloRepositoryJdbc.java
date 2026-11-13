package br.com.regulacheck.protocolo.repository;

import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.config.BancoDados;
import br.com.regulacheck.protocolo.entity.Protocolo;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Leitura de {@code RC_PROTOCOLO} por JDBC, com SQL parametrizado. So protocolos ativos sao devolvidos. */
public final class ProtocoloRepositoryJdbc implements ProtocoloRepository {

    private static final Logger LOG = LoggerFactory.getLogger(ProtocoloRepositoryJdbc.class);

    private static final String COLUNAS = "SELECT ID, IDENTIFICADOR, VERSAO, LOCALIDADE, ESPECIALIDADE, AVISO_FICTICIO "
            + "FROM RC_PROTOCOLO WHERE ATIVO = 1";
    private static final String SQL_LISTAR = COLUNAS + " ORDER BY IDENTIFICADOR, VERSAO";
    // A concatenacao IDENTIFICADOR-VERSAO e feita no banco sobre colunas; o valor do cliente entra so como parametro.
    private static final String SQL_BUSCAR = COLUNAS + " AND IDENTIFICADOR || '-' || VERSAO = ?";

    private final BancoDados banco;

    /** @param banco acesso ao Oracle */
    public ProtocoloRepositoryJdbc(BancoDados banco) {
        this.banco = banco;
    }

    @Override
    public List<Protocolo> listarAtivos() {
        try (Connection conexao = banco.conexao();
                PreparedStatement consulta = conexao.prepareStatement(SQL_LISTAR);
                ResultSet resultado = consulta.executeQuery()) {
            List<Protocolo> protocolos = new ArrayList<>();
            while (resultado.next()) {
                protocolos.add(ler(resultado));
            }
            return protocolos;
        } catch (SQLException falha) {
            throw indisponivel(falha);
        }
    }

    @Override
    public Optional<Protocolo> buscarAtivoPorProtocoloId(String protocoloId) {
        try (Connection conexao = banco.conexao();
                PreparedStatement consulta = conexao.prepareStatement(SQL_BUSCAR)) {
            consulta.setString(1, protocoloId);
            try (ResultSet resultado = consulta.executeQuery()) {
                return resultado.next() ? Optional.of(ler(resultado)) : Optional.empty();
            }
        } catch (SQLException falha) {
            throw indisponivel(falha);
        }
    }

    private static Protocolo ler(ResultSet resultado) throws SQLException {
        return new Protocolo(resultado.getLong("ID"), resultado.getString("IDENTIFICADOR"),
                resultado.getString("VERSAO"), resultado.getString("LOCALIDADE"),
                resultado.getString("ESPECIALIDADE"), resultado.getString("AVISO_FICTICIO"));
    }

    private static ExcecaoNegocio indisponivel(SQLException falha) {
        LOG.warn("Falha ao acessar protocolos. tipo={} codigoFornecedor={}",
                falha.getClass().getSimpleName(), falha.getErrorCode());
        return new ExcecaoNegocio(503, "DEPENDENCIA_INDISPONIVEL",
                "Servico temporariamente indisponivel. Tente novamente em instantes.");
    }
}
