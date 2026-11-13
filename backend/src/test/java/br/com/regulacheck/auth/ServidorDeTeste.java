package br.com.regulacheck.auth;

import br.com.regulacheck.config.Configuracao;
import br.com.regulacheck.config.Dependencias;
import br.com.regulacheck.config.Servidor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

/** Sobe a API real (Jetty, porta livre) com dependencias de teste e oferece chamadas HTTP simples. */
public final class ServidorDeTeste implements AutoCloseable {

    /** Resposta HTTP resumida. */
    public record Resposta(int status, String corpo, HttpResponse<String> http) {

        public JsonNode json() {
            try {
                return new ObjectMapper().readTree(corpo);
            } catch (Exception e) {
                throw new IllegalStateException("Corpo nao e JSON: " + corpo, e);
            }
        }

        public String cabecalho(String nome) {
            return http.headers().firstValue(nome).orElse("");
        }
    }

    private static final HttpClient CLIENTE = HttpClient.newHttpClient();

    private final Servidor servidor;

    private ServidorDeTeste(Servidor servidor) {
        this.servidor = servidor;
    }

    /** Inicia a API com o cenario de teste (repositorios em memoria, relogio fixo). */
    public static ServidorDeTeste iniciar(Dependencias dependencias) throws Exception {
        Servidor servidor = Servidor.criar(new Configuracao("127.0.0.1", 0), dependencias);
        servidor.iniciar();
        return new ServidorDeTeste(servidor);
    }

    /** Inicia a API de teste com chave do simulador e rotas de exemplo registradas pelo mecanismo real. */
    public static ServidorDeTeste iniciarComRotas(Configuracao configuracao, Dependencias dependencias,
            java.util.function.Consumer<br.com.regulacheck.auth.autorizacao.RotasPrivadas> rotas) throws Exception {
        Servidor servidor = Servidor.criar(configuracao, dependencias, rotas);
        servidor.iniciar();
        return new ServidorDeTeste(servidor);
    }

    /** Inicia a API com as dependencias reais e a configuracao informada (usado para testar o 503 sem banco). */
    public static ServidorDeTeste iniciarReal(Configuracao configuracao) throws Exception {
        Servidor servidor = Servidor.criar(configuracao);
        servidor.iniciar();
        return new ServidorDeTeste(servidor);
    }

    /** Resposta HTTP com corpo binario (download de arquivo). */
    public record RespostaBinaria(int status, byte[] corpo, HttpResponse<byte[]> http) {

        public String cabecalho(String nome) {
            return http.headers().firstValue(nome).orElse("");
        }
    }

    /** GET que preserva os bytes do corpo (para conferir download de arquivo byte a byte). */
    public RespostaBinaria getBinario(String caminho, Map<String, String> cabecalhos) throws Exception {
        HttpResponse<byte[]> r = CLIENTE.send(builder(caminho, cabecalhos).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());
        return new RespostaBinaria(r.statusCode(), r.body(), r);
    }

    public Resposta get(String caminho, Map<String, String> cabecalhos) throws Exception {
        return enviar(builder(caminho, cabecalhos).GET().build());
    }

    /** Envia um metodo qualquer, com corpo JSON opcional (use {@code null} para nao enviar corpo). */
    public Resposta enviar(String metodo, String caminho, String json, Map<String, String> cabecalhos)
            throws Exception {
        HttpRequest.Builder b = builder(caminho, cabecalhos);
        if (json != null) {
            b.header("Content-Type", "application/json");
        }
        return enviar(b.method(metodo, json == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(json)).build());
    }

    public Resposta postJson(String caminho, String json, Map<String, String> cabecalhos) throws Exception {
        return enviar(builder(caminho, cabecalhos)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json)).build());
    }

    public Resposta post(String caminho, String corpo, String contentType, Map<String, String> cabecalhos)
            throws Exception {
        HttpRequest.Builder b = builder(caminho, cabecalhos);
        if (contentType != null) {
            b.header("Content-Type", contentType);
        }
        return enviar(b.POST(HttpRequest.BodyPublishers.ofString(corpo)).build());
    }

    /** Faz login e devolve o token (falha o teste se nao for 200). */
    public String entrar(String email, String senha) throws Exception {
        Resposta r = postJson("/auth/login", "{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}", Map.of());
        if (r.status() != 200) {
            throw new IllegalStateException("Login deveria ser 200, foi " + r.status());
        }
        return r.json().get("token").asText();
    }

    /** @return nomes dos campos de um objeto JSON, na ordem em que aparecem */
    public static java.util.List<String> campos(JsonNode objeto) {
        java.util.List<String> nomes = new java.util.ArrayList<>();
        objeto.fieldNames().forEachRemaining(nomes::add);
        return nomes;
    }

    public static Map<String, String> bearer(String token) {
        return Map.of("Authorization", "Bearer " + token);
    }

    private HttpRequest.Builder builder(String caminho, Map<String, String> cabecalhos) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + servidor.porta() + caminho));
        cabecalhos.forEach(b::header);
        return b;
    }

    private Resposta enviar(HttpRequest requisicao) throws Exception {
        HttpResponse<String> r = CLIENTE.send(requisicao, HttpResponse.BodyHandlers.ofString());
        return new Resposta(r.statusCode(), r.body(), r);
    }

    @Override
    public void close() throws Exception {
        servidor.parar();
    }
}
