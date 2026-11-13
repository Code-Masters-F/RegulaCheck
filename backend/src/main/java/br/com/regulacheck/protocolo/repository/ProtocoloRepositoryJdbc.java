package br.com.regulacheck.protocolo.repository;

import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.config.BancoDados;
import br.com.regulacheck.protocolo.entity.NaturezaRegra;
import br.com.regulacheck.protocolo.entity.Protocolo;
import br.com.regulacheck.protocolo.entity.RegraDoProtocolo;
import br.com.regulacheck.protocolo.entity.ResponsavelCorrecao;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Leitura de {@code RC_PROTOCOLO} e {@code RC_REGRA} por JDBC, com SQL parametrizado. Na busca de protocolos, so os
 * ativos sao devolvidos; as regras sao lidas pela versao, ativa ou nao.
 */
public final class ProtocoloRepositoryJdbc implements ProtocoloRepository {

    private static final Logger LOG = LoggerFactory.getLogger(ProtocoloRepositoryJdbc.class);

    private static final String COLUNAS = "SELECT ID, IDENTIFICADOR, VERSAO, LOCALIDADE, ESPECIALIDADE, AVISO_FICTICIO "
            + "FROM RC_PROTOCOLO WHERE ATIVO = 1";
    private static final String SQL_LISTAR = COLUNAS + " ORDER BY IDENTIFICADOR, VERSAO";
    // A concatenacao IDENTIFICADOR-VERSAO e feita no banco sobre colunas; o valor do cliente entra so como parametro.
    private static final String SQL_BUSCAR = COLUNAS + " AND IDENTIFICADOR || '-' || VERSAO = ?";
    private static final String SQL_REGRAS = "SELECT CODIGO, DESCRICAO, EVIDENCIA_EXIGIDA, NATUREZA, "
            + "CONDICAO_APLICACAO, MENSAGEM, RESPONSAVEL, ORDEM FROM RC_REGRA WHERE PROTOCOLO_ID = ? "
            + "ORDER BY ORDEM, CODIGO";

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

    @Override
    public List<RegraDoProtocolo> listarRegras(long protocoloId) {
        try (Connection conexao = banco.conexao();
                PreparedStatement consulta = conexao.prepareStatement(SQL_REGRAS)) {
            consulta.setLong(1, protocoloId);
            try (ResultSet resultado = consulta.executeQuery()) {
                List<RegraDoProtocolo> regras = new ArrayList<>();
                while (resultado.next()) {
                    regras.add(lerRegra(resultado));
                }
                return regras;
            }
        } catch (SQLException falha) {
            throw indisponivel(falha);
        }
    }

    /**
     * Converte uma linha de {@code RC_REGRA}. Natureza e responsavel sao garantidos pelos {@code CHECK}s do esquema;
     * um valor fora deles e erro de dados, nao entrada da paciente, e falha com {@link IllegalArgumentException}.
     */
    private static RegraDoProtocolo lerRegra(ResultSet resultado) throws SQLException {
        return new RegraDoProtocolo(resultado.getString("CODIGO"), resultado.getString("DESCRICAO"),
                resultado.getString("EVIDENCIA_EXIGIDA"), NaturezaRegra.valueOf(resultado.getString("NATUREZA")),
                resultado.getString("CONDICAO_APLICACAO"), resultado.getString("MENSAGEM"),
                ResponsavelCorrecao.valueOf(resultado.getString("RESPONSAVEL")), resultado.getInt("ORDEM"));
    }

    private static Protocolo ler(ResultSet resultado) throws SQLException {
        return new Protocolo(resultado.getLong("ID"), resultado.getString("IDENTIFICADOR"),
                resultado.getString("VERSAO"), resultado.getString("LOCALIDADE"),
                resultado.getString("ESPECIALIDADE"), resultado.getString("AVISO_FICTICIO"));
    }

    private static ExcecaoNegocio indisponivel(SQLException falha) {
        LOG.warn("Falha ao acessar protocolos ou regras. tipo={} codigoFornecedor={}",
                falha.getClass().getSimpleName(), falha.getErrorCode());
        return new ExcecaoNegocio(503, "DEPENDENCIA_INDISPONIVEL",
                "Servico temporariamente indisponivel. Tente novamente em instantes.");
    }
}
