package br.com.regulacheck.indicador.repository;

import br.com.regulacheck.common.exception.ExcecaoNegocio;
import br.com.regulacheck.config.BancoDados;
import br.com.regulacheck.indicador.repository.ContagensIndicadores.PendenciasDaRegra;
import br.com.regulacheck.validacao.model.ResultadoGeral;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contagens dos indicadores do administrador por JDBC (RC-20). As tres consultas sao agregadas no banco ({@code COUNT}
 * e {@code SUM ... GROUP BY}) e nao recebem valor do cliente; rodam na mesma conexao para que os numeros venham do
 * mesmo momento tanto quanto o isolamento padrao do Oracle permite (sem transacao: so leitura).
 */
public final class IndicadorRepositoryJdbc implements IndicadorRepository {

    private static final Logger LOG = LoggerFactory.getLogger(IndicadorRepositoryJdbc.class);

    // NVL: SUM sobre zero linhas devolve NULL.
    private static final String SQL_PEDIDOS = "SELECT COUNT(*) AS TOTAL, "
            + "NVL(SUM(CASE WHEN ESTADO = 'FALHA_ANALISE' THEN 1 ELSE 0 END), 0) AS FALHAS FROM RC_SOLICITACAO";
    private static final String SQL_ANALISES = "SELECT RESULTADO_GERAL, COUNT(*) AS QUANTIDADE FROM RC_ANALISE "
            + "WHERE VIGENTE = 1 GROUP BY RESULTADO_GERAL";
    private static final String SQL_PENDENCIAS = "SELECT R.CODIGO, R.DESCRICAO, "
            + "SUM(CASE WHEN P.TIPO = 'PENDENTE' THEN 1 ELSE 0 END) AS PENDENTES, "
            + "SUM(CASE WHEN P.TIPO = 'NAO_VERIFICAVEL' THEN 1 ELSE 0 END) AS NAO_VERIFICAVEIS "
            + "FROM RC_PENDENCIA P JOIN RC_ANALISE A ON A.ID = P.ANALISE_ID JOIN RC_REGRA R ON R.ID = P.REGRA_ID "
            + "WHERE A.VIGENTE = 1 GROUP BY R.CODIGO, R.DESCRICAO";

    private final BancoDados banco;

    /** @param banco acesso ao Oracle */
    public IndicadorRepositoryJdbc(BancoDados banco) {
        this.banco = banco;
    }

    @Override
    public ContagensIndicadores contar() {
        try (Connection conexao = banco.conexao()) {
            long total;
            long falhas;
            try (PreparedStatement consulta = conexao.prepareStatement(SQL_PEDIDOS);
                    ResultSet resultado = consulta.executeQuery()) {
                resultado.next();
                total = resultado.getLong("TOTAL");
                falhas = resultado.getLong("FALHAS");
            }
            return new ContagensIndicadores(total, falhas, contarAnalises(conexao), contarPendencias(conexao));
        } catch (SQLException falha) {
            LOG.warn("Falha ao contar os indicadores. tipo={} codigoFornecedor={}",
                    falha.getClass().getSimpleName(), falha.getErrorCode());
            throw new ExcecaoNegocio(503, "DEPENDENCIA_INDISPONIVEL",
                    "Servico temporariamente indisponivel. Tente novamente em instantes.");
        }
    }

    /**
     * O {@code CHECK} de {@code RC_ANALISE.RESULTADO_GERAL} so admite os valores de {@link ResultadoGeral}; um valor
     * fora dele e erro de dados e falha com {@link IllegalArgumentException} (500), nao vira contagem silenciosa.
     */
    private static Map<ResultadoGeral, Long> contarAnalises(Connection conexao) throws SQLException {
        Map<ResultadoGeral, Long> porResultado = new EnumMap<>(ResultadoGeral.class);
        try (PreparedStatement consulta = conexao.prepareStatement(SQL_ANALISES);
                ResultSet resultado = consulta.executeQuery()) {
            while (resultado.next()) {
                porResultado.put(ResultadoGeral.valueOf(resultado.getString("RESULTADO_GERAL")),
                        resultado.getLong("QUANTIDADE"));
            }
        }
        return porResultado;
    }

    private static List<PendenciasDaRegra> contarPendencias(Connection conexao) throws SQLException {
        List<PendenciasDaRegra> regras = new ArrayList<>();
        try (PreparedStatement consulta = conexao.prepareStatement(SQL_PENDENCIAS);
                ResultSet resultado = consulta.executeQuery()) {
            while (resultado.next()) {
                regras.add(new PendenciasDaRegra(resultado.getString("CODIGO"), resultado.getString("DESCRICAO"),
                        resultado.getLong("PENDENTES"), resultado.getLong("NAO_VERIFICAVEIS")));
            }
        }
        return regras;
    }
}
