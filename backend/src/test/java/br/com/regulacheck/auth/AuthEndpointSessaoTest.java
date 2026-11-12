package br.com.regulacheck.auth;

import static br.com.regulacheck.auth.CenarioAuth.EMAIL_A;
import static br.com.regulacheck.auth.CenarioAuth.EMAIL_B;
import static br.com.regulacheck.auth.CenarioAuth.SENHA;
import static br.com.regulacheck.auth.ServidorDeTeste.bearer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** GET /auth/sessao e POST /auth/logout, com expiracao controlada pelo relogio de teste. */
class AuthEndpointSessaoTest {

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

    private static void exigeNaoAutenticado(Resposta r) {
        assertEquals(401, r.status());
        assertEquals("NAO_AUTENTICADO", r.json().get("codigo").asText());
    }

    @Test
    void sessaoValidaMostraIdentidadeEExpiracao() throws Exception {
        String token = api.entrar(EMAIL_A, SENHA);

        Resposta r = api.get("/auth/sessao", bearer(token));

        assertEquals(200, r.status());
        assertEquals("Paciente A (ficticia)", r.json().get("usuario").get("nome").asText());
        assertEquals("PACIENTE", r.json().get("usuario").get("perfil").asText());
        assertEquals("2026-11-12T20:30:00Z", r.json().get("expiraEm").asText());
        assertEquals(List.of("usuario", "expiraEm"), ServidorDeTeste.campos(r.json()));
    }

    @Test
    void logoutDevolve204EOTokenPassaAserRecusado() throws Exception {
        String token = api.entrar(EMAIL_A, SENHA);

        Resposta saida = api.postJson("/auth/logout", "", bearer(token));

        assertEquals(204, saida.status());
        assertEquals("", saida.corpo());
        exigeNaoAutenticado(api.get("/auth/sessao", bearer(token)));
    }

    @Test
    void logoutRepetidoComOMesmoTokenDevolve401() throws Exception {
        String token = api.entrar(EMAIL_A, SENHA);
        api.postJson("/auth/logout", "", bearer(token));

        exigeNaoAutenticado(api.postJson("/auth/logout", "", bearer(token)));
    }

    @Test
    void todasAsFormasDeSessaoInvalidaTemOMesmoCorpo() throws Exception {
        String logoutFeito = api.entrar(EMAIL_A, SENHA);
        api.postJson("/auth/logout", "", bearer(logoutFeito));
        String expirado = api.entrar(EMAIL_A, SENHA);
        cenario.relogio.avancar(Duration.ofMinutes(31));

        List<Resposta> respostas = List.of(
                api.get("/auth/sessao", Map.of()),
                api.get("/auth/sessao", Map.of("Authorization", "Bearer")),
                api.get("/auth/sessao", Map.of("Authorization", "Bearer lixo")),
                api.get("/auth/sessao", Map.of("Authorization", "Basic dXN1YXJpbzpzZW5oYQ==")),
                api.get("/auth/sessao", bearer("A".repeat(43))),
                api.get("/auth/sessao", bearer(logoutFeito)),
                api.get("/auth/sessao", bearer(expirado)));

        for (Resposta r : respostas) {
            exigeNaoAutenticado(r);
            assertEquals(respostas.get(0).corpo(), r.corpo());
        }
    }

    @Test
    void sessaoExpiraNoPrazo() throws Exception {
        String token = api.entrar(EMAIL_A, SENHA);

        cenario.relogio.avancar(Duration.ofMinutes(29));
        assertEquals(200, api.get("/auth/sessao", bearer(token)).status());

        cenario.relogio.avancar(Duration.ofMinutes(2));
        exigeNaoAutenticado(api.get("/auth/sessao", bearer(token)));
    }

    @Test
    void contaDesativadaDepoisDoLoginPerdeASessao() throws Exception {
        String token = api.entrar(EMAIL_A, SENHA);

        cenario.usuarios.desativar(1);

        exigeNaoAutenticado(api.get("/auth/sessao", bearer(token)));
    }

    @Test
    void logoutDeUmaSessaoNaoAfetaAsOutrasNemOutrasContas() throws Exception {
        String primeiro = api.entrar(EMAIL_A, SENHA);
        String segundo = api.entrar(EMAIL_A, SENHA);
        String deB = api.entrar(EMAIL_B, SENHA);

        api.postJson("/auth/logout", "", bearer(primeiro));

        exigeNaoAutenticado(api.get("/auth/sessao", bearer(primeiro)));
        assertEquals(200, api.get("/auth/sessao", bearer(segundo)).status());
        assertEquals(200, api.get("/auth/sessao", bearer(deB)).status());
    }

    @Test
    void identidadeVemSempreDaSessaoENuncaDoCorpo() throws Exception {
        String tokenA = api.entrar(EMAIL_A, SENHA);
        String tokenB = api.entrar(EMAIL_B, SENHA);

        // O corpo tenta apontar a conta B; so a sessao do token (A) pode ser encerrada.
        Resposta saida = api.postJson("/auth/logout", "{\"usuarioId\":2,\"email\":\"" + EMAIL_B + "\"}",
                bearer(tokenA));

        assertEquals(204, saida.status());
        exigeNaoAutenticado(api.get("/auth/sessao", bearer(tokenA)));
        assertEquals(200, api.get("/auth/sessao", bearer(tokenB)).status());
    }

    @Test
    void esquemaBearerAceitaMaiusculasEMinusculas() throws Exception {
        String token = api.entrar(EMAIL_A, SENHA);

        assertEquals(200, api.get("/auth/sessao", Map.of("Authorization", "bearer " + token)).status());
        assertEquals(200, api.get("/auth/sessao", Map.of("Authorization", "BEARER " + token)).status());
    }

    @Test
    void metodosErradosNasRotasProtegidasDevolve405() throws Exception {
        String token = api.entrar(EMAIL_A, SENHA);

        assertEquals(405, api.postJson("/auth/sessao", "", bearer(token)).status());
        assertEquals(405, api.get("/auth/logout", bearer(token)).status());
    }

    @Test
    void erro401NuncaExpoeOMotivoExato() throws Exception {
        Resposta r = api.get("/auth/sessao", bearer("A".repeat(43)));

        String corpo = r.corpo().toLowerCase();
        assertTrue(!corpo.contains("expirou") && !corpo.contains("encerrada") && !corpo.contains("desativada"));
    }
}
