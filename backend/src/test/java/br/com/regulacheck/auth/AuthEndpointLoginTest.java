package br.com.regulacheck.auth;

import static br.com.regulacheck.auth.CenarioAuth.EMAIL_A;
import static br.com.regulacheck.auth.CenarioAuth.EMAIL_INATIVA;
import static br.com.regulacheck.auth.CenarioAuth.SENHA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.config.Configuracao;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** POST /auth/login com a API real em porta livre e repositorios em memoria. */
class AuthEndpointLoginTest {

    private CenarioAuth cenario;
    private ServidorDeTeste api;

    @BeforeEach
    void iniciar() throws Exception {
        cenario = new CenarioAuth();
        api = ServidorDeTeste.iniciar(cenario.dependencias());
    }

    @AfterEach
    void parar() throws Exception {
        api.close();
    }

    private Resposta login(String email, String senha) throws Exception {
        return api.postJson("/auth/login", "{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}", Map.of());
    }

    @Test
    void loginValidoDevolve200ComOFormatoDoContrato() throws Exception {
        Resposta r = login(EMAIL_A, SENHA);

        assertEquals(200, r.status());
        assertTrue(r.cabecalho("Content-Type").startsWith("application/json"));
        assertEquals("no-store", r.cabecalho("Cache-Control"));
        JsonNode json = r.json();
        assertEquals(43, json.get("token").asText().length());
        assertEquals("Bearer", json.get("tipo").asText());
        assertEquals("2026-11-12T20:30:00Z", json.get("expiraEm").asText());
        assertEquals("Paciente A (ficticia)", json.get("usuario").get("nome").asText());
        assertEquals("PACIENTE", json.get("usuario").get("perfil").asText());
    }

    @Test
    void respostaNuncaTemHashDeSenhaNemIdInterno() throws Exception {
        Resposta r = login(EMAIL_A, SENHA);

        assertFalse(r.corpo().contains("$2a$"));
        assertFalse(r.corpo().toLowerCase().contains("hash"));
        assertFalse(r.corpo().toLowerCase().contains("senha"));
        JsonNode usuario = r.json().get("usuario");
        assertEquals(List.of("nome", "perfil"), ServidorDeTeste.campos(usuario));
    }

    @Test
    void quatroFalhasDeCredencialTemCorpoIdenticoENaoAbremSessao() throws Exception {
        Resposta senhaErrada = login(EMAIL_A, "senha-errada");
        Resposta emailInexistente = login("ninguem@exemplo.invalid", SENHA);
        Resposta contaInativa = login(EMAIL_INATIVA, SENHA);
        Resposta senhaLonga = login(EMAIL_A, "a".repeat(80));

        for (Resposta r : List.of(senhaErrada, emailInexistente, contaInativa, senhaLonga)) {
            assertEquals(401, r.status());
            assertEquals("CREDENCIAIS_INVALIDAS", r.json().get("codigo").asText());
            assertEquals(senhaErrada.corpo(), r.corpo());
        }
        assertEquals(0, cenario.sessoes.todas().size());
    }

    @Test
    void camposAusentesOuVaziosDevolve400ComDetalhePorCampo() throws Exception {
        Resposta semNada = api.postJson("/auth/login", "{}", Map.of());
        Resposta soEmail = api.postJson("/auth/login", "{\"email\":\"a@b.c\"}", Map.of());
        Resposta vazios = api.postJson("/auth/login", "{\"email\":\"  \",\"senha\":\"\"}", Map.of());

        assertEquals(400, semNada.status());
        assertEquals("REQUISICAO_INVALIDA", semNada.json().get("codigo").asText());
        assertEquals(2, semNada.json().get("detalhes").size());
        assertEquals("senha", soEmail.json().get("detalhes").get(0).get("campo").asText());
        assertEquals(2, vazios.json().get("detalhes").size());
        assertEquals(0, cenario.sessoes.todas().size());
    }

    @Test
    void emailMaiorQue254CaracteresDevolve400() throws Exception {
        Resposta r = login("a".repeat(250) + "@b.cc", SENHA);

        assertEquals(400, r.status());
        assertEquals("email", r.json().get("detalhes").get(0).get("campo").asText());
    }

    @Test
    void jsonIlegivelECorpoVazioDevolve400() throws Exception {
        assertEquals(400, api.postJson("/auth/login", "{nao e json", Map.of()).status());
        assertEquals(400, api.postJson("/auth/login", "", Map.of()).status());
    }

    @Test
    void contentTypeQueNaoEJsonDevolve415() throws Exception {
        Resposta r = api.post("/auth/login", "email=a&senha=b", "application/x-www-form-urlencoded", Map.of());

        assertEquals(415, r.status());
        assertEquals("TIPO_NAO_SUPORTADO", r.json().get("codigo").asText());
    }

    @Test
    void metodoDiferenteDePostDevolve405() throws Exception {
        Resposta r = api.get("/auth/login", Map.of());

        assertEquals(405, r.status());
        assertEquals("METODO_NAO_PERMITIDO", r.json().get("codigo").asText());
    }

    @Test
    void doisLoginsSeguidosComAsMesmasCredenciaisDaoSessoesIndependentes() throws Exception {
        String primeiro = api.entrar(EMAIL_A, SENHA);
        String segundo = api.entrar(EMAIL_A, SENHA);

        assertNotEquals(primeiro, segundo);
        assertEquals(200, api.get("/auth/sessao", ServidorDeTeste.bearer(primeiro)).status());
        assertEquals(200, api.get("/auth/sessao", ServidorDeTeste.bearer(segundo)).status());
        assertEquals(2, cenario.sessoes.todas().size());
    }

    @Test
    void semBancoConfiguradoOLoginDevolve503ENenhumaSessaoMasOHealthFunciona() throws Exception {
        try (ServidorDeTeste semBanco = ServidorDeTeste.iniciarReal(new Configuracao("127.0.0.1", 0))) {
            Resposta r = semBanco.postJson("/auth/login",
                    "{\"email\":\"" + EMAIL_A + "\",\"senha\":\"" + SENHA + "\"}", Map.of());

            assertEquals(503, r.status());
            assertEquals("DEPENDENCIA_INDISPONIVEL", r.json().get("codigo").asText());
            assertFalse(r.corpo().contains("ORA-"));
            assertFalse(r.corpo().toLowerCase().contains("jdbc"));
            assertEquals(200, semBanco.get("/health", Map.of()).status());
        }
    }
}
