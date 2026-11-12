package br.com.regulacheck.common.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

/** Verifica que erros saem no formato padrao e sem detalhes internos. */
class ErroPadraoTest {

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

    private HttpResponse<String> chamar(String metodo, String caminho) throws Exception {
        HttpRequest requisicao = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + servidor.porta() + caminho))
                .method(metodo, HttpRequest.BodyPublishers.noBody()).build();
        return CLIENTE.send(requisicao, HttpResponse.BodyHandlers.ofString());
    }

    private void semDetalheInterno(String corpo) {
        assertFalse(corpo.contains("Exception"), corpo);
        assertFalse(corpo.contains("at br.com"), corpo);
        assertFalse(corpo.toLowerCase().contains("select "), corpo);
    }

    @Test
    void rotaInexistenteDevolve404NoFormatoPadrao() throws Exception {
        HttpResponse<String> resposta = chamar("GET", "/rota-inexistente");

        assertEquals(404, resposta.statusCode());
        assertTrue(resposta.body().contains("\"codigo\":\"NAO_ENCONTRADO\""), resposta.body());
        assertTrue(resposta.headers().firstValue("Content-Type").orElse("").startsWith("application/json"));
        semDetalheInterno(resposta.body());
    }

    @Test
    void metodoNaoPermitidoDevolve405NoFormatoPadrao() throws Exception {
        HttpResponse<String> resposta = chamar("POST", "/health");

        assertEquals(405, resposta.statusCode());
        assertTrue(resposta.body().contains("\"codigo\":\"METODO_NAO_PERMITIDO\""), resposta.body());
        assertEquals("GET", resposta.headers().firstValue("Allow").orElse(""));
        semDetalheInterno(resposta.body());
    }
}
