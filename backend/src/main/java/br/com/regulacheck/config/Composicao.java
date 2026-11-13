package br.com.regulacheck.config;

import br.com.regulacheck.auth.autorizacao.ChaveSimulador;
import br.com.regulacheck.auth.autorizacao.RotasPrivadas;
import br.com.regulacheck.auth.autorizacao.SimuladorFiltro;
import br.com.regulacheck.auth.controller.AutenticacaoFiltro;
import br.com.regulacheck.auth.controller.LoginController;
import br.com.regulacheck.auth.controller.LogoutController;
import br.com.regulacheck.auth.controller.SessaoController;
import br.com.regulacheck.auth.service.AutenticacaoService;
import br.com.regulacheck.auth.service.GeradorToken;
import br.com.regulacheck.auth.service.VerificadorSenha;
import br.com.regulacheck.common.http.CabecalhosSegurosFiltro;
import br.com.regulacheck.common.http.RotaNaoEncontradaController;
import br.com.regulacheck.saude.SaudeController;
import br.com.regulacheck.saude.SaudeRepositoryMemoria;
import br.com.regulacheck.saude.SaudeService;
import br.com.regulacheck.usuario.entity.Perfil;
import jakarta.servlet.DispatcherType;
import java.time.Duration;
import java.util.EnumSet;
import java.util.function.Consumer;
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
        return montar(configuracao, dependencias, rotas -> { });
    }

    /**
     * Igual a {@link #montar(Configuracao, Dependencias)}, com um ponto de extensao para registrar rotas
     * adicionais pelo mesmo {@link RotasPrivadas} da producao. Em producao a extensao e vazia; os testes de
     * autorizacao o usam para publicar rotas de exemplo (a RC-05 nao cria rotas de negocio).
     *
     * @param configuracao configuracao de execucao
     * @param dependencias repositorios e relogio
     * @param extensao rotas extras, registradas antes da rota coringa
     * @return contexto raiz pronto para ser entregue ao servidor
     * @throws IllegalStateException se alguma rota ficar publicada fora de {@link RotasPrivadas}
     */
    public static ServletContextHandler montar(Configuracao configuracao, Dependencias dependencias,
            Consumer<RotasPrivadas> extensao) {
        ServletContextHandler contexto = new ServletContextHandler("/");
        // Cabecalhos de seguranca em todas as respostas (filtro apenas de cabecalho; nao decide acesso).
        contexto.addFilter(new FilterHolder(new CabecalhosSegurosFiltro(ConfiguracaoDeCanal.ehHttps(configuracao))),
                "/*", EnumSet.of(DispatcherType.REQUEST, DispatcherType.ERROR));

        SaudeService saude = new SaudeService(new SaudeRepositoryMemoria());
        contexto.addServlet(new ServletHolder(new SaudeController(saude)), "/health");

        AutenticacaoService autenticacao = new AutenticacaoService(dependencias.usuarios(), dependencias.sessoes(),
                new VerificadorSenha(), new GeradorToken(), dependencias.relogio(),
                Duration.ofMinutes(configuracao.ttlSessaoMinutos()));
        contexto.addServlet(new ServletHolder(new LoginController(autenticacao)), "/auth/login");
        // Rotas privadas: sempre por RotasPrivadas (401 sem sessao valida, 403 se o perfil nao serve).
        ChaveSimulador chaveSimulador = new ChaveSimulador(configuracao.chaveSimulador());
        RotasPrivadas rotas = new RotasPrivadas(contexto, new AutenticacaoFiltro(autenticacao, chaveSimulador),
                new SimuladorFiltro(chaveSimulador, autenticacao));
        rotas.registrar("/auth/sessao", new SessaoController(), Perfil.PACIENTE, Perfil.ADMINISTRADOR);
        rotas.registrar("/auth/logout", new LogoutController(autenticacao), Perfil.PACIENTE, Perfil.ADMINISTRADOR);
        extensao.accept(rotas);

        // Rota coringa: qualquer caminho sem controller devolve 404 no formato padrao.
        contexto.addServlet(new ServletHolder(new RotaNaoEncontradaController()), "/*");
        rotas.exigirTudoDeclarado();
        return contexto;
    }
}
