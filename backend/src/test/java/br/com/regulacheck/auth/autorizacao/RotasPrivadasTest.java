package br.com.regulacheck.auth.autorizacao;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.auth.CenarioAuth;
import br.com.regulacheck.auth.controller.AutenticacaoFiltro;
import br.com.regulacheck.common.http.ServletJson;
import br.com.regulacheck.config.Composicao;
import br.com.regulacheck.config.Configuracao;
import br.com.regulacheck.usuario.entity.Perfil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Set;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.junit.jupiter.api.Test;

/**
 * US3 e US5 / SC-004: nao se registra rota privada sem perfil, nao se declara duas vezes, e uma rota
 * publicada fora do registro faz a montagem falhar (negar por omissao, verificado no build).
 */
class RotasPrivadasTest {

    /** Servlet trivial so para registrar caminhos. */
    private static final class Vazio extends ServletJson {
        private static final long serialVersionUID = 1L;

        Vazio() {
            super("GET");
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
            resp.setStatus(204);
        }
    }

    private static RotasPrivadas novoRegistro(ServletContextHandler contexto) {
        CenarioAuth cenario = new CenarioAuth();
        ChaveSimulador chave = new ChaveSimulador("chave-ficticia");
        return new RotasPrivadas(contexto, new AutenticacaoFiltro(cenario.servico, chave),
                new SimuladorFiltro(chave, cenario.servico));
    }

    @Test
    void rotaSemPerfilNaoPodeSerRegistrada() {
        ServletContextHandler contexto = new ServletContextHandler("/");
        RotasPrivadas rotas = novoRegistro(contexto);

        assertThrows(IllegalArgumentException.class, () -> rotas.registrar("/nova/*", new Vazio()));
        assertThrows(IllegalArgumentException.class,
                () -> new AutorizacaoFiltro(Set.<Perfil>of()));
    }

    @Test
    void caminhoPublicoOuRepetidoNaoPodeSerDeclaradoPrivado() {
        RotasPrivadas rotas = novoRegistro(new ServletContextHandler("/"));
        rotas.registrar("/nova/*", new Vazio(), Perfil.PACIENTE);

        assertThrows(IllegalArgumentException.class, () -> rotas.registrar("/nova/*", new Vazio(), Perfil.PACIENTE));
        assertThrows(IllegalArgumentException.class, () -> rotas.registrar("/health", new Vazio(), Perfil.PACIENTE));
        assertThrows(IllegalArgumentException.class, () -> rotas.registrar("/auth/login", new Vazio(), Perfil.PACIENTE));
        assertEquals(Set.of("/nova/*"), rotas.declaradas());
    }

    @Test
    void rotaDeclaradaPassaNaVerificacaoDeOmissao() {
        ServletContextHandler contexto = new ServletContextHandler("/");
        RotasPrivadas rotas = novoRegistro(contexto);
        rotas.registrar("/nova/*", new Vazio(), Perfil.PACIENTE, Perfil.ADMINISTRADOR);
        rotas.registrarTelemetria("/telemetria/*", new Vazio());
        contexto.addServlet(new ServletHolder(new Vazio()), "/health");

        assertDoesNotThrow(rotas::exigirTudoDeclarado);
    }

    @Test
    void rotaPublicadaForaDoRegistroFazAMontagemFalhar() {
        ServletContextHandler contexto = new ServletContextHandler("/");
        RotasPrivadas rotas = novoRegistro(contexto);
        rotas.registrar("/declarada/*", new Vazio(), Perfil.PACIENTE);
        // Esquecimento tipico: publicar direto no contexto, sem passar por RotasPrivadas.
        contexto.addServlet(new ServletHolder(new Vazio()), "/esquecida/*");

        IllegalStateException erro = assertThrows(IllegalStateException.class, rotas::exigirTudoDeclarado);

        assertTrue(erro.getMessage().contains("/esquecida/*"), erro.getMessage());
        assertTrue(!erro.getMessage().contains("/declarada/*"), "so deve listar as rotas esquecidas");
    }

    @Test
    void aMontagemDeProducaoNaoTemNenhumaRotaForaDoRegistro() {
        CenarioAuth cenario = new CenarioAuth();

        // Composicao.montar chama exigirTudoDeclarado ao final; se alguem esquecer uma rota, este teste falha.
        assertDoesNotThrow(() -> Composicao.montar(new Configuracao("127.0.0.1", 0), cenario.dependencias()));
    }
}
