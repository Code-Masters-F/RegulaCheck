package br.com.regulacheck.common.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.auth.CenarioAuth;
import br.com.regulacheck.auth.ServidorDeTeste;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.common.Json;
import br.com.regulacheck.integracao.ia.TextoSimulado;
import br.com.regulacheck.usuario.entity.Perfil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

/** SC-007: texto de resumo simulado com marcacao executavel sai como texto puro. */
class TextoPuroTest {

    private static final List<String> HOSTIS = List.of(
            "<script>alert(1)</script>",
            "<img src=x onerror=alert(1)>",
            "Tom & Jerry's \"citacao\" <b>negrito</b>",
            "</pre><iframe src=//exemplo.invalid></iframe>");

    /** Rota de exemplo (so de teste) que devolve o resumo simulado com o texto do parametro {@code i}. */
    private static final class ResumoServlet extends ServletJson {
        private static final long serialVersionUID = 1L;

        ResumoServlet() {
            super("GET");
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            int indice = Integer.parseInt(req.getParameter("i"));
            responderJson(resp, 200, TextoSimulado.de(HOSTIS.get(indice)));
        }
    }

    @Test
    void respostaNuncaTraMarcacaoLiteralMasOClienteRecuperaOTextoOriginal() throws Exception {
        CenarioAuth cenario = new CenarioAuth();
        try (ServidorDeTeste api = ServidorDeTeste.iniciarComRotas(
                new br.com.regulacheck.config.Configuracao("127.0.0.1", 0), cenario.dependencias(),
                rotas -> rotas.registrar("/teste/resumo", new ResumoServlet(), Perfil.PACIENTE))) {
            String token = api.entrar(CenarioAuth.EMAIL_A, CenarioAuth.SENHA);
            for (int i = 0; i < HOSTIS.size(); i++) {
                Resposta r = api.get("/teste/resumo?i=" + i, ServidorDeTeste.bearer(token));
                assertEquals(200, r.status());
                for (String perigoso : new String[] {"<", ">", "&", "'"}) {
                    assertFalse(r.corpo().contains(perigoso), "marcacao literal '" + perigoso + "' em: " + r.corpo());
                }
                assertEquals(HOSTIS.get(i), r.json().get("texto").asText(), "o cliente deve recuperar o original");
                assertTrue(r.json().get("simulado").asBoolean());
                assertEquals("texto_puro", r.json().get("formato").asText());
                assertEquals("nosniff", r.cabecalho("X-Content-Type-Options"));
                assertTrue(r.cabecalho("Content-Type").startsWith("application/json"));
            }
        }
    }

    @Test
    void escapeVaiAteForaDoCaminhoHttp() {
        String json = Json.escrever(TextoSimulado.de("<script>x</script> & 'a'"));
        assertFalse(json.contains("<") || json.contains(">") || json.contains("&") || json.contains("'"), json);
        assertTrue(json.toLowerCase().contains("\\u003cscript\\u003e"));
    }

    @Test
    void textoNormalComAcentosNaoEEscapadoSemNecessidade() {
        String json = Json.escrever(TextoSimulado.de("Atenção: confira o CNS"));
        assertTrue(json.contains("Atenção: confira o CNS"), json);
    }
}
