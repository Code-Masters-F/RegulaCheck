package br.com.regulacheck.documentacao;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.config.Configuracao;
import br.com.regulacheck.config.Servidor;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * RC-28: a documentacao interativa em {@code /docs} com o servidor real em porta livre, sem banco e sem sessao.
 * Confere a lista fixa de arquivos, o contrato identico ao versionado, os 404/405 padrao e que a politica de
 * conteudo mais permissiva fica restrita as respostas de sucesso de {@code /docs}.
 */
class DocumentacaoEndpointTest {

    private static final String POLITICA_ESTRITA = "default-src 'none'; frame-ancestors 'none'";
    private static final HttpClient CLIENTE = HttpClient.newHttpClient();

    private static Servidor servidor;

    @BeforeAll
    static void iniciar() throws Exception {
        servidor = Servidor.criar(new Configuracao("127.0.0.1", 0));
        servidor.iniciar();
    }

    @AfterAll
    static void parar() throws Exception {
        servidor.parar();
    }

    private static HttpResponse<byte[]> enviar(String metodo, String caminho) throws Exception {
        HttpRequest requisicao = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + servidor.porta() + caminho))
                .method(metodo, HttpRequest.BodyPublishers.noBody()).build();
        return CLIENTE.send(requisicao, HttpResponse.BodyHandlers.ofByteArray());
    }

    private static String cabecalho(HttpResponse<?> resposta, String nome) {
        return resposta.headers().firstValue(nome).orElse("");
    }

    private static String texto(HttpResponse<byte[]> resposta) {
        return new String(resposta.body(), StandardCharsets.UTF_8);
    }

    @Test
    void paginaAbreSemSessaoNosTresEnderecos() throws Exception {
        for (String caminho : new String[] {"/docs", "/docs/", "/docs/index.html"}) {
            HttpResponse<byte[]> r = enviar("GET", caminho);
            assertEquals(200, r.statusCode(), caminho);
            assertTrue(cabecalho(r, "Content-Type").startsWith("text/html"), caminho);
            assertTrue(texto(r).contains("id=\"swagger-ui\""), caminho);
            assertTrue(texto(r).contains("/docs/inicializar.js"), caminho);
            assertEquals(DocumentacaoController.POLITICA_DE_CONTEUDO, cabecalho(r, "Content-Security-Policy"), caminho);
        }
    }

    @Test
    void contratoServidoEIdenticoAoVersionadoEmDocsApi() throws Exception {
        HttpResponse<byte[]> r = enviar("GET", "/docs/openapi.yaml");

        assertEquals(200, r.statusCode());
        assertTrue(cabecalho(r, "Content-Type").startsWith("application/yaml"));
        assertArrayEquals(Files.readAllBytes(Path.of("..", "docs", "api", "openapi.yaml")), r.body());
    }

    @Test
    void arquivosDaPaginaSaemComOTipoCorretoEOsCabecalhosDeSeguranca() throws Exception {
        String[][] esperados = {
            {"/docs/inicializar.js", "text/javascript"},
            {"/docs/swagger-ui-bundle.js", "text/javascript"},
            {"/docs/swagger-ui.css", "text/css"},
            {"/docs/index.css", "text/css"},
            {"/docs/favicon-32x32.png", "image/png"},
            {"/docs/favicon-16x16.png", "image/png"},
        };
        for (String[] esperado : esperados) {
            HttpResponse<byte[]> r = enviar("GET", esperado[0]);
            assertEquals(200, r.statusCode(), esperado[0]);
            assertTrue(cabecalho(r, "Content-Type").startsWith(esperado[1]), esperado[0]);
            assertTrue(r.body().length > 0, esperado[0]);
            assertEquals("nosniff", cabecalho(r, "X-Content-Type-Options"), esperado[0]);
            assertEquals("no-store", cabecalho(r, "Cache-Control"), esperado[0]);
            assertEquals("no-referrer", cabecalho(r, "Referrer-Policy"), esperado[0]);
        }
    }

    @Test
    void inicializadorNaoUsaValidadorExternoNemGuardaAutorizacao() throws Exception {
        String inicializador = texto(enviar("GET", "/docs/inicializar.js"));

        assertTrue(inicializador.contains("url: \"/docs/openapi.yaml\""));
        assertTrue(inicializador.contains("validatorUrl: null"));
        assertTrue(inicializador.contains("persistAuthorization: false"));
        assertFalse(inicializador.contains("http"), "o inicializador nao aponta para outro endereco");
    }

    @Test
    void caminhoForaDaListaRespondeNaoEncontradoComPoliticaEstrita() throws Exception {
        // swagger-ui.js e swagger-initializer.js existem na webjar, mas nao estao na lista fixa.
        for (String caminho : new String[] {"/docs/swagger-ui.js", "/docs/swagger-initializer.js",
                "/docs/openapi.yml", "/docs/documentacao/openapi.yaml", "/docs/META-INF/MANIFEST.MF",
                "/docs/index.html/", "/docsx"}) {
            HttpResponse<byte[]> r = enviar("GET", caminho);
            assertEquals(404, r.statusCode(), caminho);
            assertTrue(texto(r).contains("\"codigo\":\"NAO_ENCONTRADO\""), caminho);
            assertEquals(POLITICA_ESTRITA, cabecalho(r, "Content-Security-Policy"), caminho);
        }
    }

    @Test
    void travessiaDePastaNaoEntregaArquivoDoServidor() throws Exception {
        for (String caminho : new String[] {"/docs/../pom.xml", "/docs/%2e%2e/pom.xml", "/docs/..%2Fpom.xml"}) {
            HttpResponse<byte[]> r = enviar("GET", caminho);
            assertNotEquals(200, r.statusCode(), caminho);
            assertFalse(texto(r).contains("<project"), caminho);
        }
    }

    @Test
    void metodoDiferenteDeGetRespondeMetodoNaoPermitido() throws Exception {
        for (String metodo : new String[] {"POST", "PUT", "DELETE"}) {
            HttpResponse<byte[]> r = enviar(metodo, "/docs/openapi.yaml");
            assertEquals(405, r.statusCode(), metodo);
            assertEquals("GET", cabecalho(r, "Allow"), metodo);
            assertTrue(texto(r).contains("\"codigo\":\"METODO_NAO_PERMITIDO\""), metodo);
        }
    }

    @Test
    void demaisRotasMantemAPoliticaEstrita() throws Exception {
        assertEquals(POLITICA_ESTRITA, cabecalho(enviar("GET", "/health"), "Content-Security-Policy"));
        assertEquals(POLITICA_ESTRITA, cabecalho(enviar("GET", "/rota-inexistente"), "Content-Security-Policy"));
    }
}
