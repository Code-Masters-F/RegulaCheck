package br.com.regulacheck.auth.autorizacao;

import static br.com.regulacheck.auth.autorizacao.AmbienteDeAutorizacao.CHAVE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.auth.autorizacao.RecursosDeExemplo.Tipo;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** US4 / SC-005: a chave do simulador serve so para a telemetria; contas nao usam a telemetria. */
class SimuladorEndpointTest {

    private static Map<String, String> chave(String valor) {
        return Map.of(ChaveSimulador.CABECALHO, valor);
    }

    private static String telemetria(long id) {
        return "/teste/telemetria/" + id;
    }

    @Test
    void chaveValidaEnviaParaSessaoAbertaEEncerradaOuInexistenteDa404Identico() throws Exception {
        try (AmbienteDeAutorizacao amb = new AmbienteDeAutorizacao()) {
            Resposta aberta = amb.api.postJson(telemetria(RecursosDeExemplo.capturaAbertaId()), "{}", chave(CHAVE));
            Resposta encerrada = amb.api.postJson(telemetria(RecursosDeExemplo.capturaEncerradaId()), "{}", chave(CHAVE));
            Resposta inexistente = amb.api.postJson(telemetria(999), "{}", chave(CHAVE));

            assertEquals(200, aberta.status());
            assertEquals(404, encerrada.status());
            assertEquals(encerrada.corpo(), inexistente.corpo(), "encerrada e inexistente nao podem se distinguir");
        }
    }

    @Test
    void chaveAusenteOuErradaDa401() throws Exception {
        try (AmbienteDeAutorizacao amb = new AmbienteDeAutorizacao()) {
            long aberta = RecursosDeExemplo.capturaAbertaId();

            assertEquals(401, amb.api.postJson(telemetria(aberta), "{}", Map.of()).status());
            assertEquals(401, amb.api.postJson(telemetria(aberta), "{}", chave("chave-errada")).status());
            assertEquals(401, amb.api.postJson(telemetria(aberta), "{}", chave("")).status());
        }
    }

    @Test
    void chaveValidaEmQualquerRotaDeContaDa403() throws Exception {
        try (AmbienteDeAutorizacao amb = new AmbienteDeAutorizacao()) {
            List<String> rotas = new java.util.ArrayList<>(List.of("/teste/admin/indicadores", "/auth/sessao"));
            for (Tipo tipo : Tipo.values()) {
                rotas.add("/teste/" + tipo.caminho() + "/" + tipo.id(1));
                rotas.add("/teste/" + tipo.caminho());
            }
            for (String rota : rotas) {
                Resposta r = amb.api.get(rota, chave(CHAVE));
                assertEquals(403, r.status(), rota);
                assertEquals("ACESSO_NEGADO", r.json().get("codigo").asText());
            }
            assertEquals(403, amb.api.postJson("/auth/logout", "", chave(CHAVE)).status());
            assertEquals(0, amb.recursos.acessos(), "a chave do simulador nao pode chegar a nenhum recurso");
        }
    }

    @Test
    void contasNaoUsamATelemetria() throws Exception {
        try (AmbienteDeAutorizacao amb = new AmbienteDeAutorizacao()) {
            long aberta = RecursosDeExemplo.capturaAbertaId();

            assertEquals(403, amb.chamar("POST", telemetria(aberta), amb.tokenA, "{}").status());
            assertEquals(403, amb.chamar("POST", telemetria(aberta), amb.tokenAdmin, "{}").status());
            // Token invalido sem chave continua sendo 401.
            assertEquals(401, amb.chamar("POST", telemetria(aberta), "token-invalido", "{}").status());
        }
    }

    @Test
    void chaveValidaPrevaleceQuandoHaTokenDeContaJuntoNaTelemetria() throws Exception {
        try (AmbienteDeAutorizacao amb = new AmbienteDeAutorizacao()) {
            Resposta r = amb.api.postJson(telemetria(RecursosDeExemplo.capturaAbertaId()), "{}",
                    Map.of(ChaveSimulador.CABECALHO, CHAVE, "Authorization", "Bearer " + amb.tokenA));

            assertEquals(200, r.status());
        }
    }

    @Test
    void semChaveConfiguradaATelemetriaDa401EAApiSobeNormalmente() throws Exception {
        try (AmbienteDeAutorizacao amb = new AmbienteDeAutorizacao(null)) {
            long aberta = RecursosDeExemplo.capturaAbertaId();

            assertEquals(200, amb.api.get("/health", Map.of()).status());
            assertEquals(401, amb.api.postJson(telemetria(aberta), "{}", chave("qualquer")).status());
            assertEquals(401, amb.api.postJson(telemetria(aberta), "{}", chave("")).status());
            assertEquals(200, amb.chamar("GET", "/auth/sessao", amb.tokenA, null).status());
        }
    }
}
