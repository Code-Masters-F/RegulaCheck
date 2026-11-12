package br.com.regulacheck.config;

import br.com.regulacheck.auth.repository.SessaoRepositoryJdbc;
import br.com.regulacheck.usuario.repository.UsuarioRepositoryJdbc;
import java.io.IOException;
import br.com.regulacheck.auth.autorizacao.RotasPrivadas;
import java.time.Clock;
import java.util.function.Consumer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servidor HTTP embutido (Jetty) que hospeda os controllers da aplicacao.
 *
 * <p>As paginas de erro do proprio servidor nao exibem stack trace.
 */
public final class Servidor {

    private static final Logger LOG = LoggerFactory.getLogger(Servidor.class);

    private final Server servidor;
    private final ServerConnector conector;
    private final BancoDados banco;

    private Servidor(Server servidor, ServerConnector conector, BancoDados banco) {
        this.servidor = servidor;
        this.conector = conector;
        this.banco = banco;
    }

    /**
     * Prepara o servidor com as dependencias reais (Oracle por JDBC, relogio do sistema) sem inicia-lo.
     * O banco so e acessado quando uma rota que o usa for chamada; sem {@code DB_*}, a API sobe e essas
     * rotas respondem 503.
     *
     * @param configuracao host, porta, dados do banco e validade da sessao ({@code 0} escolhe porta livre)
     * @return servidor pronto para {@link #iniciar()}
     */
    public static Servidor criar(Configuracao configuracao) {
        BancoDados banco = BancoDados.criar(configuracao.banco());
        Dependencias reais = new Dependencias(new UsuarioRepositoryJdbc(banco), new SessaoRepositoryJdbc(banco),
                Clock.systemUTC());
        return montar(configuracao, reais, banco, rotas -> { });
    }

    /**
     * Prepara o servidor com dependencias fornecidas, sem banco (usado em testes com repositorios em
     * memoria).
     *
     * @param configuracao host, porta e validade da sessao
     * @param dependencias repositorios e relogio
     * @return servidor pronto para {@link #iniciar()}
     */
    public static Servidor criar(Configuracao configuracao, Dependencias dependencias) {
        return montar(configuracao, dependencias, null, rotas -> { });
    }

    /**
     * Como {@link #criar(Configuracao, Dependencias)}, com rotas extras registradas pelo mesmo
     * {@link RotasPrivadas} da producao (usado nos testes de autorizacao com rotas de exemplo).
     *
     * @param configuracao host, porta, validade da sessao e chave do simulador
     * @param dependencias repositorios e relogio
     * @param extensao rotas extras, registradas antes da rota coringa
     * @return servidor pronto para {@link #iniciar()}
     */
    public static Servidor criar(Configuracao configuracao, Dependencias dependencias,
            Consumer<RotasPrivadas> extensao) {
        return montar(configuracao, dependencias, null, extensao);
    }

    private static Servidor montar(Configuracao configuracao, Dependencias dependencias, BancoDados banco,
            Consumer<RotasPrivadas> extensao) {
        Server servidor = new Server();
        ServerConnector conector = new ServerConnector(servidor);
        conector.setHost(configuracao.host());
        conector.setPort(configuracao.porta());
        servidor.addConnector(conector);

        ErrorHandler erros = new ErrorHandler();
        erros.setShowStacks(false);
        servidor.setErrorHandler(erros);

        servidor.setHandler(Composicao.montar(configuracao, dependencias, extensao));
        return new Servidor(servidor, conector, banco);
    }

    /**
     * Inicia o servidor.
     *
     * @throws IOException se a porta estiver ocupada ou o servidor nao subir; a mensagem orienta a
     *     usar {@code SERVER_PORT}
     */
    public void iniciar() throws IOException {
        try {
            servidor.start();
        } catch (Exception falha) {
            // Detalhe tecnico no log; a mensagem para quem executa e curta e acionavel.
            LOG.error("Falha ao iniciar o servidor.", falha);
            throw new IOException("Nao foi possivel iniciar a API na porta " + conector.getPort()
                    + ". Verifique se a porta esta em uso ou defina SERVER_PORT.", falha);
        }
        LOG.info("API iniciada em {}:{} (GET /health)", conector.getHost(), porta());
    }

    /**
     * @return porta efetiva de escuta (util quando a porta configurada e 0)
     */
    public int porta() {
        return conector.getLocalPort();
    }

    /**
     * Para o servidor e libera a porta.
     *
     * @throws Exception se a parada falhar
     */
    public void parar() throws Exception {
        servidor.stop();
        if (banco != null) {
            banco.fechar();
        }
    }

    /** Bloqueia a thread atual ate o servidor parar. */
    public void aguardar() throws InterruptedException {
        servidor.join();
    }
}
