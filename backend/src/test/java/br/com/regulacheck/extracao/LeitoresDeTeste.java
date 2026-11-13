package br.com.regulacheck.extracao;

import br.com.regulacheck.integracao.extracao.LeitorSimulado;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Fabrica leitores simulados para os testes (catalogo de producao + catalogo de teste). */
public final class LeitoresDeTeste {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Path CATALOGO_PRODUCAO = Path.of("src", "main", "resources", "extracao-simulada",
            "catalogo.json");
    private static final Path CATALOGO_TESTES = Path.of("src", "test", "resources", "extracao-simulada",
            "catalogo-testes.json");

    private LeitoresDeTeste() {
    }

    /** @return leitor simulado que conhece as duas amostras oficiais e as cinco extras de teste */
    public static LeitorSimulado simuladoComTodasAsAmostras() {
        try {
            ObjectNode raiz = MAPPER.createObjectNode();
            ArrayNode leituras = raiz.putArray("leituras");
            for (Path catalogo : new Path[] {CATALOGO_PRODUCAO, CATALOGO_TESTES}) {
                JsonNode lido = MAPPER.readTree(Files.readAllBytes(catalogo));
                lido.get("leituras").forEach(leituras::add);
            }
            return new LeitorSimulado(new ByteArrayInputStream(MAPPER.writeValueAsBytes(raiz)));
        } catch (IOException falha) {
            throw new UncheckedIOException(falha);
        }
    }

    /**
     * @param json catalogo em texto
     * @return leitor simulado com o catalogo informado
     */
    public static LeitorSimulado simuladoCom(String json) {
        InputStream entrada = new ByteArrayInputStream(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return new LeitorSimulado(entrada);
    }
}
