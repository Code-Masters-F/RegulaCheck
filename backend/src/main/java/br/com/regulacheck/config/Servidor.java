package br.com.regulacheck.config;

import java.io.IOException;
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

    private Servidor(Server servidor, ServerConnector conector) {
        this.servidor = servidor;
        this.conector = conector;
    }

    /**
     * Prepara o servidor sem inicia-lo.
     *
     * @param configuracao host e porta de escuta ({@code 0} escolhe uma porta livre)
     * @return servidor pronto para {@link #iniciar()}
     */
    public static Servidor criar(Configuracao configuracao) {
        Server servidor = new Server();
        ServerConnector conector = new ServerConnector(servidor);
        conector.setHost(configuracao.host());
        conector.setPort(configuracao.porta());
        servidor.addConnector(conector);

        ErrorHandler erros = new ErrorHandler();
        erros.setShowStacks(false);
        servidor.setErrorHandler(erros);

        servidor.setHandler(Composicao.montar());
        return new Servidor(servidor, conector);
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
    }

    /** Bloqueia a thread atual ate o servidor parar. */
    public void aguardar() throws InterruptedException {
        servidor.join();
    }
}
