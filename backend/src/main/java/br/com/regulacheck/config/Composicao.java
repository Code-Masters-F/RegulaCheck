package br.com.regulacheck.config;

import br.com.regulacheck.common.http.RotaNaoEncontradaController;
import br.com.regulacheck.saude.SaudeController;
import br.com.regulacheck.saude.SaudeRepositoryMemoria;
import br.com.regulacheck.saude.SaudeService;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;

/**
 * Raiz de composicao: monta manualmente, por construtor, Repository, Service e Controller e
 * registra as rotas. Nao ha container de injecao de dependencia.
 *
 * <p>Cada nova funcionalidade acrescenta aqui sua propria montagem. A ordem de registro importa:
 * a rota coringa {@code /*} (404 padronizado) deve ser a ultima.
 */
public final class Composicao {

    private Composicao() {
    }

    /**
     * Cria o contexto de servlets com todas as rotas da aplicacao.
     *
     * @return contexto raiz ({@code /}) pronto para ser entregue ao servidor
     */
    public static ServletContextHandler montar() {
        ServletContextHandler contexto = new ServletContextHandler("/");

        SaudeService saude = new SaudeService(new SaudeRepositoryMemoria());
        contexto.addServlet(new ServletHolder(new SaudeController(saude)), "/health");

        // Rota coringa: qualquer caminho sem controller devolve 404 no formato padrao.
        contexto.addServlet(new ServletHolder(new RotaNaoEncontradaController()), "/*");
        return contexto;
    }
}
