package br.com.regulacheck.saude;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.config.Configuracao;
import br.com.regulacheck.config.Servidor;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verifica o health tecnico com o servidor real em porta livre, sem banco nem variaveis de ambiente.
 */
class SaudeEndpointTest {

    private static Servidor servidor;
    private static final HttpClient CLIENTE = HttpClient.newHttpClient();

    @BeforeAll
    static void iniciar() throws Exception {
        servidor = Servidor.criar(new Configuracao("127.0.0.1", 0));
        servidor.iniciar();
    }

    @AfterAll
    static void parar() throws Exception {
        servidor.parar();
    }

    @Test
    void healthResponde200ComStatusUp() throws Exception {
        HttpResponse<String> resposta = CLIENTE.send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + servidor.porta() + "/health")).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(200, resposta.statusCode());
        assertEquals("{\"status\":\"UP\"}", resposta.body());
        assertTrue(resposta.headers().firstValue("Content-Type").orElse("").startsWith("application/json"));
    }
}
