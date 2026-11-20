package br.com.regulacheck.documentacao;

import br.com.regulacheck.common.exception.RespostaErro;
import br.com.regulacheck.common.http.ServletJson;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Controller publico de {@code GET /docs} (RC-28): serve a documentacao interativa da API (Swagger UI) e o
 * contrato OpenAPI escrito a mao ({@code docs/api/openapi.yaml}, copiado para o jar no build).
 *
 * <p>Serve somente uma <strong>lista fixa</strong> de arquivos, carregados do classpath na construcao. O caminho
 * da URL so e usado como chave dessa lista e nunca compoe um caminho de arquivo, o que impede travessia de pasta.
 * Qualquer outro caminho responde 404 {@code NAO_ENCONTRADO}, identico ao da rota coringa, e qualquer metodo alem
 * de GET responde 405. A rota nao consulta banco nem expoe dado de paciente, segredo ou configuracao.
 *
 * <p>Nas respostas de sucesso, a politica de conteudo estrita do filtro global ({@code default-src 'none'}) e
 * trocada por {@link #POLITICA_DE_CONTEUDO}: o Swagger UI precisa executar seus proprios scripts, aplicar estilos
 * inline, mostrar icones embutidos ({@code data:}) e chamar a API na mesma origem ("Try it out"). Os demais
 * cabecalhos de seguranca do filtro ({@code nosniff}, {@code no-store}, {@code no-referrer}, HSTS) continuam
 * valendo, e as respostas de erro desta rota mantem a politica estrita.
 */
public class DocumentacaoController extends ServletJson {

    private static final long serialVersionUID = 1L;

    /** Politica de conteudo das respostas de sucesso de {@code /docs}; scripts so da propria API, sem inline. */
    public static final String POLITICA_DE_CONTEUDO = "default-src 'none'; script-src 'self'; "
            + "style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'; "
            + "frame-ancestors 'none'; base-uri 'none'; form-action 'none'";

    private static final String PASTA_PROPRIA = "/documentacao/";
    private static final String PROPRIEDADES_DA_WEBJAR = "/META-INF/maven/org.webjars/swagger-ui/pom.properties";
    private static final String PASTA_DA_WEBJAR = "/META-INF/resources/webjars/swagger-ui/";

    private static final String HTML = "text/html;charset=utf-8";
    private static final String JAVASCRIPT = "text/javascript;charset=utf-8";
    private static final String CSS = "text/css;charset=utf-8";
    private static final String YAML = "application/yaml;charset=utf-8";
    private static final String PNG = "image/png";

    /** Arquivo servido: conteudo imutavel e tipo declarado (o {@code nosniff} exige o tipo correto). */
    private record Arquivo(byte[] conteudo, String tipo) {
    }

    /** Lista fixa: caminho apos {@code /docs} (vazio para {@code /docs}) para o arquivo. Somente leitura. */
    private final transient Map<String, Arquivo> arquivos;

    /**
     * Carrega a pagina, o inicializador, o contrato e os arquivos do Swagger UI da versao empacotada.
     *
     * @throws IllegalStateException se algum arquivo faltar no pacote; a API nao sobe com a documentacao quebrada
     */
    public DocumentacaoController() {
        super("GET");
        String webjar = PASTA_DA_WEBJAR + versaoDaWebjar() + "/";
        Arquivo pagina = carregar(PASTA_PROPRIA + "index.html", HTML);
        Map<String, Arquivo> mapa = new HashMap<>();
        mapa.put("", pagina);
        mapa.put("/", pagina);
        mapa.put("/index.html", pagina);
        mapa.put("/inicializar.js", carregar(PASTA_PROPRIA + "inicializar.js", JAVASCRIPT));
        mapa.put("/openapi.yaml", carregar(PASTA_PROPRIA + "openapi.yaml", YAML));
        mapa.put("/swagger-ui-bundle.js", carregar(webjar + "swagger-ui-bundle.js", JAVASCRIPT));
        mapa.put("/swagger-ui.css", carregar(webjar + "swagger-ui.css", CSS));
        mapa.put("/index.css", carregar(webjar + "index.css", CSS));
        mapa.put("/favicon-32x32.png", carregar(webjar + "favicon-32x32.png", PNG));
        mapa.put("/favicon-16x16.png", carregar(webjar + "favicon-16x16.png", PNG));
        this.arquivos = Map.copyOf(mapa);
    }

    @Override
    protected void doGet(HttpServletRequest requisicao, HttpServletResponse resposta) throws IOException {
        String caminho = requisicao.getPathInfo() == null ? "" : requisicao.getPathInfo();
        Arquivo arquivo = arquivos.get(caminho);
        if (arquivo == null) {
            RespostaErro.escrever(resposta, HttpServletResponse.SC_NOT_FOUND, "NAO_ENCONTRADO",
                    "Recurso nao encontrado.");
            return;
        }
        resposta.setStatus(HttpServletResponse.SC_OK);
        resposta.setHeader("Content-Security-Policy", POLITICA_DE_CONTEUDO);
        resposta.setContentType(arquivo.tipo());
        resposta.setContentLength(arquivo.conteudo().length);
        resposta.getOutputStream().write(arquivo.conteudo());
    }

    /** A versao vem do {@code pom.properties} da webjar, para que ela fique escrita so no {@code pom.xml}. */
    private static String versaoDaWebjar() {
        Properties propriedades = new Properties();
        try (InputStream entrada = abrir(PROPRIEDADES_DA_WEBJAR)) {
            propriedades.load(entrada);
        } catch (IOException falha) {
            throw new UncheckedIOException("Documentacao da API ilegivel no pacote.", falha);
        }
        String versao = propriedades.getProperty("version", "");
        if (!versao.matches("[0-9]+(\\.[0-9]+)*")) {
            throw new IllegalStateException("Versao do Swagger UI ausente no pacote.");
        }
        return versao;
    }

    private static Arquivo carregar(String recurso, String tipo) {
        try (InputStream entrada = abrir(recurso)) {
            return new Arquivo(entrada.readAllBytes(), tipo);
        } catch (IOException falha) {
            throw new UncheckedIOException("Documentacao da API ilegivel no pacote.", falha);
        }
    }

    private static InputStream abrir(String recurso) {
        InputStream entrada = DocumentacaoController.class.getResourceAsStream(recurso);
        if (entrada == null) {
            // Defeito de empacotamento: falha na subida em vez de uma pagina quebrada em silencio.
            throw new IllegalStateException("Documentacao da API ausente no pacote.");
        }
        return entrada;
    }
}
