package br.com.regulacheck.config;

import br.com.regulacheck.common.exception.ExcecaoNegocio;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Acesso preguicoso ao banco Oracle por um pequeno pool de conexoes (HikariCP).
 *
 * <p>Criar esta classe <strong>nao</strong> conecta ao banco: o pool so e montado no primeiro uso.
 * Assim a API inicia sem {@code DB_*} (o {@code /health} nao depende do banco) e apenas as rotas que
 * precisam de banco falham, com 503 generico. O pool tem no maximo 3 conexoes por ser um banco
 * compartilhado da faculdade.
 *
 * <p>Nunca registra URL, usuario, senha nem texto de erro do driver na resposta; no log ficam
 * somente a classe da falha e o codigo do fornecedor.
 */
public final class BancoDados {

    private static final Logger LOG = LoggerFactory.getLogger(BancoDados.class);

    /** Tempo padrao, em ms, para obter uma conexao antes de desistir. */
    public static final long TEMPO_CONEXAO_PADRAO_MS = 5000;

    private static final String MENSAGEM_INDISPONIVEL =
            "Servico temporariamente indisponivel. Tente novamente em instantes.";

    private final Configuracao.Banco configuracao;
    private final long tempoConexaoMs;
    private HikariDataSource pool;

    private BancoDados(Configuracao.Banco configuracao, long tempoConexaoMs) {
        this.configuracao = configuracao;
        this.tempoConexaoMs = tempoConexaoMs;
    }

    /**
     * @param configuracao dados de conexao (podem estar ausentes)
     * @return acesso ao banco sem conectar ainda
     */
    public static BancoDados criar(Configuracao.Banco configuracao) {
        return new BancoDados(configuracao, TEMPO_CONEXAO_PADRAO_MS);
    }

    /**
     * Variante com tempo de conexao customizado (usada em testes para falhar rapido).
     *
     * @param configuracao dados de conexao
     * @param tempoConexaoMs tempo maximo para obter conexao, em milissegundos (minimo aceito pelo pool: 250)
     * @return acesso ao banco sem conectar ainda
     */
    public static BancoDados criar(Configuracao.Banco configuracao, long tempoConexaoMs) {
        return new BancoDados(configuracao, tempoConexaoMs);
    }

    /** @return {@code true} se URL, usuario e senha foram informados */
    public boolean configurado() {
        return configuracao.configurado();
    }

    /**
     * Obtem uma conexao do pool; quem chamar deve fecha-la (devolve ao pool).
     *
     * @return conexao pronta para uso
     * @throws ExcecaoNegocio 503 {@code DEPENDENCIA_INDISPONIVEL} se o banco nao esta configurado ou
     *     nao responde
     */
    public Connection conexao() {
        if (!configurado()) {
            throw indisponivel();
        }
        try {
            return pool().getConnection();
        } catch (SQLException falha) {
            LOG.warn("Banco indisponivel. tipo={} codigoFornecedor={}", falha.getClass().getSimpleName(),
                    falha.getErrorCode());
            throw indisponivel();
        } catch (RuntimeException falha) {
            LOG.warn("Banco indisponivel. tipo={}", falha.getClass().getSimpleName());
            throw indisponivel();
        }
    }

    /** Fecha o pool, se ele chegou a ser criado. */
    public synchronized void fechar() {
        if (pool != null) {
            pool.close();
            pool = null;
        }
    }

    private synchronized HikariDataSource pool() {
        if (pool == null) {
            HikariConfig config = new HikariConfig();
            config.setPoolName("regulacheck-pool");
            config.setJdbcUrl(configuracao.url());
            config.setUsername(configuracao.usuario());
            config.setPassword(configuracao.senha());
            config.setMaximumPoolSize(3);
            config.setMinimumIdle(0);
            config.setConnectionTimeout(Math.max(250, tempoConexaoMs));
            // Valor negativo: o pool nasce sem conectar, entao a API inicia mesmo com o banco fora do ar.
            config.setInitializationFailTimeout(-1);
            pool = new HikariDataSource(config);
        }
        return pool;
    }

    private static ExcecaoNegocio indisponivel() {
        return new ExcecaoNegocio(503, "DEPENDENCIA_INDISPONIVEL", MENSAGEM_INDISPONIVEL);
    }
}
