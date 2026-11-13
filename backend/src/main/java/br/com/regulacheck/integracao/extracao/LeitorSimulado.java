package br.com.regulacheck.integracao.extracao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * <strong>Leitor SIMULADO</strong> (decisao do grupo de 12/11/2026): nao le o documento. Identifica o arquivo pelo
 * SHA-256 do conteudo e devolve a leitura predefinida em um catalogo; arquivo que nao esta no catalogo recebe
 * {@link SituacaoLeitura#LEITURA_VAZIA}, nunca campos inventados. Nao olha nome, extensao nem legenda (dados da
 * usuaria) e nao escreve conteudo em log. Toda saida respeita {@link ValidadorDeLeitura}; um catalogo fora do
 * contrato impede a criacao do leitor (a aplicacao nao sobe), em vez de falhar durante uma leitura.
 *
 * <p>O catalogo de producao ({@value #RECURSO_PADRAO}) so conhece as duas amostras sinteticas oficiais
 * ({@code samples/}); os testes passam um catalogo proprio ao construtor. Formato:
 * {@code {"leituras":[{"hash":"<sha256>","situacao":"LIDO","campos":[{"codigo":"NOME","situacao":"IDENTIFICADO",
 * "valor":"...","pagina":1,"trecho":"..."}]}]}}.
 */
public final class LeitorSimulado implements LeitorDeDocumento {

    /** Catalogo de producao no classpath. */
    public static final String RECURSO_PADRAO = "/extracao-simulada/catalogo.json";

    private static final Pattern HASH = Pattern.compile("^[0-9a-f]{64}$");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Map<String, LeituraDeDocumento> catalogo;

    /**
     * Leitor com o catalogo de producao.
     *
     * @throws IllegalStateException se o catalogo faltar ou estiver fora do contrato
     */
    public LeitorSimulado() {
        this(abrirPadrao());
    }

    /**
     * Leitor com um catalogo informado (usado nos testes).
     *
     * @param catalogoJson catalogo no formato descrito na classe; e fechado ao final
     * @throws IllegalStateException se o catalogo estiver fora do contrato
     */
    public LeitorSimulado(InputStream catalogoJson) {
        this.catalogo = Map.copyOf(carregar(catalogoJson));
    }

    @Override
    public LeituraDeDocumento ler(ArquivoParaLeitura arquivo) {
        LeituraDeDocumento predefinida = catalogo.get(sha256(arquivo.conteudo()));
        return predefinida != null ? predefinida : LeituraDeDocumento.vazia();
    }

    private static InputStream abrirPadrao() {
        InputStream entrada = LeitorSimulado.class.getResourceAsStream(RECURSO_PADRAO);
        if (entrada == null) {
            throw new IllegalStateException("Catalogo da leitura simulada ausente.");
        }
        return entrada;
    }

    private static Map<String, LeituraDeDocumento> carregar(InputStream entrada) {
        try (InputStream fluxo = entrada) {
            JsonNode raiz = MAPPER.readTree(fluxo);
            JsonNode leituras = raiz == null ? null : raiz.get("leituras");
            if (leituras == null || !leituras.isArray()) {
                throw new IllegalStateException("Catalogo da leitura simulada fora do contrato.");
            }
            Map<String, LeituraDeDocumento> mapa = new HashMap<>();
            for (JsonNode item : leituras) {
                String hash = item.path("hash").asText("");
                if (!HASH.matcher(hash).matches()) {
                    throw new IllegalStateException("Catalogo da leitura simulada com hash invalido.");
                }
                LeituraDeDocumento leitura = lerLeitura(item);
                if (ValidadorDeLeitura.verificar(leitura, null, 0).isPresent()) {
                    throw new IllegalStateException("Catalogo da leitura simulada com leitura fora do contrato.");
                }
                if (mapa.put(hash, leitura) != null) {
                    throw new IllegalStateException("Catalogo da leitura simulada com hash repetido.");
                }
            }
            return mapa;
        } catch (IOException | IllegalArgumentException falha) {
            throw new IllegalStateException("Catalogo da leitura simulada ilegivel ou fora do contrato.");
        }
    }

    private static LeituraDeDocumento lerLeitura(JsonNode item) {
        SituacaoLeitura situacao = SituacaoLeitura.valueOf(item.path("situacao").asText(""));
        List<CampoLido> campos = new ArrayList<>();
        for (JsonNode campo : item.path("campos")) {
            Integer pagina = campo.hasNonNull("pagina") ? campo.get("pagina").asInt() : null;
            String valor = campo.hasNonNull("valor") ? campo.get("valor").asText() : null;
            String trecho = campo.hasNonNull("trecho") ? campo.get("trecho").asText() : null;
            campos.add(new CampoLido(CodigoCampo.valueOf(campo.path("codigo").asText("")),
                    SituacaoCampo.valueOf(campo.path("situacao").asText("")), valor, pagina, trecho));
        }
        return new LeituraDeDocumento(situacao, campos);
    }

    /**
     * Calcula o SHA-256 em hexadecimal minusculo (o mesmo formato de {@code RC_DOCUMENTO.HASH_CONTEUDO}).
     *
     * @param conteudo bytes do arquivo
     * @return hash em hexadecimal
     */
    public static String sha256(byte[] conteudo) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(conteudo));
        } catch (NoSuchAlgorithmException falha) {
            throw new IllegalStateException("SHA-256 indisponivel na JVM.", falha);
        }
    }
}
