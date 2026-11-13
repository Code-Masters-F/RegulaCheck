package br.com.regulacheck.common.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.auth.CenarioAuth;
import br.com.regulacheck.auth.ServidorDeTeste;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.config.Configuracao;
import br.com.regulacheck.config.Dependencias;
import br.com.regulacheck.usuario.entity.Usuario;
import br.com.regulacheck.usuario.repository.UsuarioRepository;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** FR-029: toda resposta leva cabecalhos de seguranca, sem mudar status nem corpo. */
class CabecalhosSegurosTest {

    private static final String CHAVE = "chave-ficticia-cabecalhos";

    private static void conferir(String contexto, Resposta r) {
        assertEquals("nosniff", r.cabecalho("X-Content-Type-Options"), contexto);
        assertEquals("no-store", r.cabecalho("Cache-Control"), contexto);
        assertEquals("default-src 'none'; frame-ancestors 'none'", r.cabecalho("Content-Security-Policy"), contexto);
        assertEquals("no-referrer", r.cabecalho("Referrer-Policy"), contexto);
    }

    @Test
    void todasAsRespostasTemOsCabecalhosEOsStatusNaoMudam() throws Exception {
        CenarioAuth cenario = new CenarioAuth();
        Configuracao config = new Configuracao("127.0.0.1", 0, Configuracao.Banco.vazio(), 30, CHAVE);
        try (ServidorDeTeste api = ServidorDeTeste.iniciarComRotas(config, cenario.dependencias(), rotas -> { })) {
            String token = api.entrar(CenarioAuth.EMAIL_A, CenarioAuth.SENHA);

            Resposta saude = api.get("/health", Map.of());
            assertEquals(200, saude.status());
            conferir("200", saude);

            Resposta semToken = api.get("/auth/sessao", Map.of());
            assertEquals(401, semToken.status());
            conferir("401", semToken);

            Resposta chaveEmConta = api.get("/auth/sessao", Map.of("X-Chave-Simulador", CHAVE));
            assertEquals(403, chaveEmConta.status());
            conferir("403", chaveEmConta);

            Resposta inexistente = api.get("/nao/existe", Map.of());
            assertEquals(404, inexistente.status());
            assertEquals("NAO_ENCONTRADO", inexistente.json().get("codigo").asText());
            conferir("404", inexistente);

            Resposta metodo = api.post("/health", "", "application/json", Map.of());
            assertEquals(405, metodo.status());
            conferir("405", metodo);

            Resposta corpoRuim = api.postJson("/auth/login", "{nao e json", Map.of());
            assertEquals(400, corpoRuim.status());
            conferir("400", corpoRuim);

            Resposta tipoErrado = api.post("/auth/login", "x", "text/plain", Map.of());
            assertEquals(415, tipoErrado.status());
            conferir("415", tipoErrado);

            Resposta sessao = api.get("/auth/sessao", ServidorDeTeste.bearer(token));
            assertEquals(200, sessao.status());
            conferir("sessao 200", sessao);
        }
    }

    @Test
    void erroInternoTambemTemOsCabecalhos() throws Exception {
        CenarioAuth cenario = new CenarioAuth();
        UsuarioRepository quebrado = new UsuarioRepository() {
            @Override
            public Optional<Usuario> buscarPorEmail(String email) {
                throw new IllegalStateException("falha simulada");
            }

            @Override
            public Optional<Usuario> buscarPorId(long id) {
                return Optional.empty();
            }
        };
        try (ServidorDeTeste api = ServidorDeTeste.iniciar(
                new Dependencias(quebrado, cenario.sessoes, cenario.protocolos, cenario.solicitacoes, cenario.documentos,
                        cenario.extracoes, cenario.conferencias, new br.com.regulacheck.integracao.extracao.LeitorSimulado(),
                        cenario.relogio))) {
            Resposta r = api.postJson("/auth/login", "{\"email\":\"a@exemplo.invalid\",\"senha\":\"x\"}", Map.of());
            assertEquals(500, r.status());
            conferir("500", r);
        }
    }

    @Test
    void hstsSoAparecePorHttpsPublico() throws Exception {
        CenarioAuth cenario = new CenarioAuth();
        Configuracao comHttps = new Configuracao("127.0.0.1", 0, Configuracao.Banco.vazio(), 30, null, null,
                "https://demo-ficticia.exemplo.org");
        try (ServidorDeTeste api = ServidorDeTeste.iniciarComRotas(comHttps, cenario.dependencias(), rotas -> { })) {
            assertTrue(api.get("/health", Map.of()).cabecalho("Strict-Transport-Security").startsWith("max-age="));
        }
        try (ServidorDeTeste api = ServidorDeTeste.iniciar(cenario.dependencias())) {
            assertFalse(api.get("/health", Map.of()).http().headers().firstValue("Strict-Transport-Security")
                    .isPresent());
        }
    }
}
