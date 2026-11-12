package br.com.regulacheck.config;

import br.com.regulacheck.auth.controller.AutenticacaoFiltro;
import br.com.regulacheck.auth.controller.LoginController;
import br.com.regulacheck.auth.controller.LogoutController;
import br.com.regulacheck.auth.controller.SessaoController;
import br.com.regulacheck.auth.service.AutenticacaoService;
import br.com.regulacheck.auth.service.GeradorToken;
import br.com.regulacheck.auth.service.VerificadorSenha;
import br.com.regulacheck.common.http.RotaNaoEncontradaController;
import br.com.regulacheck.saude.SaudeController;
import br.com.regulacheck.saude.SaudeRepositoryMemoria;
import br.com.regulacheck.saude.SaudeService;
import jakarta.servlet.DispatcherType;
import java.time.Duration;
import java.util.EnumSet;
import org.eclipse.jetty.ee10.servlet.FilterHolder;
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
     * @param configuracao configuracao de execucao (usa a validade da sessao)
     * @param dependencias repositorios e relogio; reais em producao, em memoria nos testes
     * @return contexto raiz ({@code /}) pronto para ser entregue ao servidor
     */
    public static ServletContextHandler montar(Configuracao configuracao, Dependencias dependencias) {
        ServletContextHandler contexto = new ServletContextHandler("/");

        SaudeService saude = new SaudeService(new SaudeRepositoryMemoria());
        contexto.addServlet(new ServletHolder(new SaudeController(saude)), "/health");

        AutenticacaoService autenticacao = new AutenticacaoService(dependencias.usuarios(), dependencias.sessoes(),
                new VerificadorSenha(), new GeradorToken(), dependencias.relogio(),
                Duration.ofMinutes(configuracao.ttlSessaoMinutos()));
        contexto.addServlet(new ServletHolder(new LoginController(autenticacao)), "/auth/login");
        // Rotas protegidas: o filtro identifica a conta e responde 401 sem sessao valida.
        AutenticacaoFiltro protecao = new AutenticacaoFiltro(autenticacao);
        contexto.addFilter(new FilterHolder(protecao), "/auth/sessao", EnumSet.of(DispatcherType.REQUEST));
        contexto.addFilter(new FilterHolder(protecao), "/auth/logout", EnumSet.of(DispatcherType.REQUEST));
        contexto.addServlet(new ServletHolder(new SessaoController()), "/auth/sessao");
        contexto.addServlet(new ServletHolder(new LogoutController(autenticacao)), "/auth/logout");

        // Rota coringa: qualquer caminho sem controller devolve 404 no formato padrao.
        contexto.addServlet(new ServletHolder(new RotaNaoEncontradaController()), "/*");
        return contexto;
    }
}
