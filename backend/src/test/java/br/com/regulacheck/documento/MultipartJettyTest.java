package br.com.regulacheck.documento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Teste de reconhecimento (RC-08, T002) do suporte a {@code multipart/form-data} do Jetty 12 EE10 embutido: prova,
 * com um servlet minimo sob {@code /solicitacoes/*}, as suposicoes de {@code research.md} D2 antes de o resto da
 * historia depender delas. Mantido como teste de regressao: se uma atualizacao do Jetty mudar o comportamento, este
 * teste acusa antes dos testes de negocio.
 */
class MultipartJettyTest {

    private static final long MAX_ARQUIVO = 1_000;
    private static final long MAX_REQUISICAO = 1_500;
    private static final HttpClient CLIENTE = HttpClient.newHttpClient();

    @TempDir
    Path pastaTemporaria;

    private Server servidor;
    private int porta;

    /** Servlet que le a parte {@code arquivo} e devolve o tamanho, ou a classe da excecao lancada. */
    static final class Leitor extends HttpServlet {
        private static final long serialVersionUID = 1L;

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            try {
                Part parte = req.getPart("arquivo");
                Part tipo = req.getPart("tipoDocumental");
                String texto = tipo == null ? "-" : new String(tipo.getInputStream().readAllBytes(),
                        StandardCharsets.UTF_8);
                long lidos = parte == null ? -1 : parte.getInputStream().readAllBytes().length;
                escrever(resp, 200, "OK pathInfo=" + req.getPathInfo() + " bytes=" + lidos + " tipo=" + texto);
            } catch (Throwable falha) {
                StringBuilder cadeia = new StringBuilder();
                for (Throwable t = falha; t != null; t = t.getCause()) {
                    cadeia.append(" > ").append(t.getClass().getSimpleName()).append('[').append(t.getMessage())
                            .append(']');
                }
                escrever(resp, 400, "EXC" + cadeia);
            }
        }

        private static void escrever(HttpServletResponse resp, int status, String corpo) throws IOException {
            resp.setStatus(status);
            resp.setContentType("text/plain");
            resp.getWriter().write(corpo);
        }
    }

    @BeforeEach
    void subir() throws Exception {
        servidor = new Server();
        ServerConnector conector = new ServerConnector(servidor);
        conector.setHost("127.0.0.1");
        conector.setPort(0);
        servidor.addConnector(conector);
        ServletContextHandler contexto = new ServletContextHandler("/");
        ServletHolder holder = new ServletHolder(new Leitor());
        // Configuracao programatica, como Composicao fara: vale para a requisicao roteada por PathInfo.
        holder.getRegistration().setMultipartConfig(
                new MultipartConfigElement(pastaTemporaria.toString(), MAX_ARQUIVO, MAX_REQUISICAO, 0));
        contexto.addServlet(holder, "/solicitacoes/*");
        servidor.setHandler(contexto);
        servidor.start();
        porta = conector.getLocalPort();
    }

    @AfterEach
    void parar() throws Exception {
        servidor.stop();
    }

    @Test
    void multipartDentroDoLimiteEhLidoERoteadoPorPathInfo() throws Exception {
        MultipartTeste corpo = new MultipartTeste().texto("tipoDocumental", "ENCAMINHAMENTO")
                .arquivo("arquivo", "x.pdf", "application/pdf", new byte[500]);

        HttpResponse<String> r = enviar("/solicitacoes/12/documentos", corpo.contentType(), corpo.corpo());

        assertEquals(200, r.statusCode(), r.body());
        assertEquals("OK pathInfo=/12/documentos bytes=500 tipo=ENCAMINHAMENTO", r.body());
    }

    @Test
    void estouroDoLimiteDoArquivoEhExcecaoIdentificavel() throws Exception {
        MultipartTeste corpo = new MultipartTeste().arquivo("arquivo", "x.pdf", "application/pdf", new byte[1_200]);

        HttpResponse<String> r = enviar("/solicitacoes/12/documentos", corpo.contentType(), corpo.corpo());

        // Registra o tipo real da excecao: o controller precisa trata-lo para responder 413.
        
        assertEquals(400, r.statusCode());
        assertTrue(r.body().contains("max file size exceeded"), r.body());
    }

    @Test
    void estouroDoLimiteDaRequisicaoEhExcecaoIdentificavel() throws Exception {
        MultipartTeste corpo = new MultipartTeste().arquivo("arquivo", "x.pdf", "application/pdf", new byte[900])
                .arquivo("outro", "y.pdf", "application/pdf", new byte[900]);

        HttpResponse<String> r = enviar("/solicitacoes/12/documentos", corpo.contentType(), corpo.corpo());

        
        assertEquals(400, r.statusCode());
        assertTrue(r.body().contains("max length exceeded"), r.body());
    }

    @Test
    void corpoQueNaoEhMultipartFazGetPartLancarExcecao() throws Exception {
        HttpResponse<String> r = enviar("/solicitacoes/12/documentos", "application/json", "{}".getBytes());

        
        assertEquals(400, r.statusCode());
        assertTrue(r.body().contains("Not multipart Content-Type"), r.body());
    }

    @Test
    void partesTemporariasSaoApagadasAoFimDaRequisicao() throws Exception {
        MultipartTeste ok = new MultipartTeste().arquivo("arquivo", "x.pdf", "application/pdf", new byte[500]);
        enviar("/solicitacoes/12/documentos", ok.contentType(), ok.corpo());
        MultipartTeste estouro = new MultipartTeste().arquivo("arquivo", "x.pdf", "application/pdf", new byte[1_200]);
        enviar("/solicitacoes/12/documentos", estouro.contentType(), estouro.corpo());

        // O Jetty apaga as partes ao encerrar a requisicao; dar um instante para o ciclo de vida terminar.
        Thread.sleep(300);
        try (Stream<Path> restantes = Files.list(pastaTemporaria)) {
            assertEquals(0, restantes.count(), "nenhuma parte pode permanecer na pasta temporaria");
        }
    }

    private HttpResponse<String> enviar(String caminho, String tipo, byte[] corpo) throws Exception {
        HttpRequest requisicao = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + porta + caminho))
                .header("Content-Type", tipo).POST(HttpRequest.BodyPublishers.ofByteArray(corpo)).build();
        return CLIENTE.send(requisicao, HttpResponse.BodyHandlers.ofString());
    }
}
