package br.com.regulacheck.common.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import br.com.regulacheck.auth.CenarioAuth;
import br.com.regulacheck.auth.ServidorDeTeste;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** FR-027: entradas ruins viram 400/415 padrao, sem eco do corpo nem detalhe interno (comportamento da RC-04). */
class ValidacaoDeEntradaTest {

    private static void semDetalheInterno(Resposta r, String... naoEcoar) {
        for (String proibido : new String[] {"Exception", "\tat ", "br.com.regulacheck", "jackson", "SELECT"}) {
            assertFalse(r.corpo().contains(proibido), "detalhe interno: " + proibido);
        }
        for (String valor : naoEcoar) {
            assertFalse(r.corpo().contains(valor), "eco do corpo recebido");
        }
    }

    @Test
    void corpoMalformadoVazioEGrandeDaoRequisicaoInvalidaSemEco() throws Exception {
        CenarioAuth cenario = new CenarioAuth();
        try (ServidorDeTeste api = ServidorDeTeste.iniciar(cenario.dependencias())) {
            Resposta malformado = api.postJson("/auth/login", "{\"email\": \"a@x.invalid\", \"senha\": SENHA-NO-CORPO-1", Map.of());
            assertEquals(400, malformado.status());
            assertEquals("REQUISICAO_INVALIDA", malformado.json().get("codigo").asText());
            semDetalheInterno(malformado, "SENHA-NO-CORPO-1");

            Resposta vazio = api.postJson("/auth/login", "   ", Map.of());
            assertEquals(400, vazio.status());

            String grande = "{\"email\":\"a@x.invalid\",\"senha\":\"" + "SENHA-GRANDE".repeat(1000) + "\"}";
            Resposta acimaDoLimite = api.postJson("/auth/login", grande, Map.of());
            assertEquals(400, acimaDoLimite.status(), "corpo JSON grande segue 400 (contrato da RC-04); 413 e so de arquivo");
            semDetalheInterno(acimaDoLimite, "SENHA-GRANDE");
        }
    }

    @Test
    void tipoDeConteudoErradoDa415SemEco() throws Exception {
        CenarioAuth cenario = new CenarioAuth();
        try (ServidorDeTeste api = ServidorDeTeste.iniciar(cenario.dependencias())) {
            Resposta r = api.post("/auth/login", "email=a&senha=SENHA-FORM-1", "application/x-www-form-urlencoded",
                    Map.of());
            assertEquals(415, r.status());
            assertEquals("TIPO_NAO_SUPORTADO", r.json().get("codigo").asText());
            semDetalheInterno(r, "SENHA-FORM-1");
            Resposta semTipo = api.post("/auth/login", "{}", null, Map.of());
            assertEquals(415, semTipo.status());
        }
    }
}
