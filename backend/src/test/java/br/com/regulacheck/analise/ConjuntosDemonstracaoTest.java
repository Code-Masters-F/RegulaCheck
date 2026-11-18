package br.com.regulacheck.analise;

import static br.com.regulacheck.analise.CenarioAnalise.historico;
import static br.com.regulacheck.analise.CenarioAnalise.pedir;
import static br.com.regulacheck.analise.CenarioAvaliacao.confirmarTudo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.analise.CenarioAvaliacao.Pedido;
import br.com.regulacheck.auth.ServidorDeTeste.Resposta;
import br.com.regulacheck.documento.CenarioDocumento;
import br.com.regulacheck.extracao.CenarioExtracao;
import br.com.regulacheck.extracao.ConjuntosSinteticos;
import br.com.regulacheck.extracao.ConjuntosSinteticos.Arquivo;
import br.com.regulacheck.extracao.ConjuntosSinteticos.Conferencia;
import br.com.regulacheck.extracao.ConjuntosSinteticos.Conjunto;
import br.com.regulacheck.extracao.ConjuntosSinteticos.Formato;
import br.com.regulacheck.extracao.ConjuntosSinteticos.Recusa;
import br.com.regulacheck.extracao.ConjuntosSinteticos.Veredicto;
import br.com.regulacheck.integracao.extracao.LeitorSimulado;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/**
 * Regressao dos conjuntos sinteticos de demonstracao (RC-23): cada conjunto percorre a API real em memoria (envio,
 * leitura <strong>simulada</strong>, conferencia, analise) e o resultado e comparado com o gabarito escrito antes da
 * execucao, em {@link ConjuntosSinteticos}. Divergencia e um achado a registrar; o gabarito nao e ajustado para
 * igualar a saida. Sem banco, sem rede e sem credenciais.
 */
class ConjuntosDemonstracaoTest {

    private static final Path SAMPLES = Path.of("..", "samples");

    @TestFactory
    Stream<DynamicTest> cadaConjuntoCoincideComSeuGabarito() {
        return ConjuntosSinteticos.todos().stream()
                .map(conjunto -> DynamicTest.dynamicTest("conjunto " + conjunto.pasta(), () -> executar(conjunto)));
    }

    @Test
    void catalogoTemOsDozeConjuntosEAsOnzeSituacoesExigidas() {
        List<Conjunto> todos = ConjuntosSinteticos.todos();
        assertTrue(todos.size() >= 12, "ao menos 12 conjuntos: " + todos.size());
        Set<String> nomes = new java.util.HashSet<>();
        todos.forEach(c -> nomes.add(c.nome()));
        for (String situacao : List.of("completo", "sem-identificacao", "varias-pendencias", "foto-ilegivel",
                "foto-cortada", "outra-pessoa", "menor-com-autorizacao", "menor-sem-autorizacao",
                "maioridade-limite", "formato-invalido", "instrucao-no-documento")) {
            assertTrue(nomes.contains(situacao), "situacao exigida ausente: " + situacao);
        }
        assertTrue(todos.stream().anyMatch(c -> c.originalDe() != null), "ha versao corrigida");
        for (Conjunto c : todos) {
            if (c.originalDe() != null) {
                assertNotNull(ConjuntosSinteticos.porNumero(c.originalDe()), "original existente: " + c.pasta());
            }
        }
        assertTrue(todos.stream().flatMap(c -> c.arquivos().stream()).anyMatch(a -> a.formato() == Formato.PDF),
                "ha PDF textual");
        assertTrue(todos.stream().flatMap(c -> c.arquivos().stream()).anyMatch(a -> a.formato() == Formato.PNG),
                "ha imagem");
    }

    @Test
    void conjuntoComInstrucaoNoDocumentoTemOMesmoResultadoDoCompleto() {
        Conjunto completo = ConjuntosSinteticos.porNumero("01");
        Conjunto instrucao = ConjuntosSinteticos.porNumero("11");
        assertEquals(completo.regras(), instrucao.regras());
        assertEquals(completo.geral(), instrucao.geral());
    }

    @Test
    void todoArquivoTemAMarcaDeMaterialFicticioEOHashConfereComOCatalogoEOGabarito() throws IOException {
        String catalogo = Files.readString(Path.of("src", "main", "resources", "extracao-simulada", "catalogo.json"),
                StandardCharsets.UTF_8);
        for (Conjunto conjunto : ConjuntosSinteticos.todos()) {
            String gabarito = Files.readString(SAMPLES.resolve("conjuntos").resolve(conjunto.pasta())
                    .resolve("gabarito.md"), StandardCharsets.UTF_8);
            for (Arquivo arquivo : ConjuntosSinteticos.arquivosEfetivos(conjunto)) {
                assertTrue(contemMarca(arquivo), "marca de material ficticio ausente em " + arquivo.caminho());
                String hash = LeitorSimulado.sha256(arquivo.bytes());
                assertTrue(gabarito.contains(hash), "hash fora do gabarito do conjunto " + conjunto.pasta() + ": "
                        + arquivo.nome());
                if (arquivo.leitura() != null) {
                    assertTrue(catalogo.contains(hash), "hash fora do catalogo de leituras: " + arquivo.caminho());
                }
            }
        }
    }

    // ------------------------------------------------------------------ percurso

    private static void executar(Conjunto conjunto) throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            if (conjunto.recusadoNoEnvio()) {
                executarRecusas(c, conjunto);
            } else if (conjunto.originalDe() != null) {
                executarCorrigido(c, conjunto);
            } else {
                Pedido pedido = enviarELer(c, conjunto);
                JsonNode analise = analisar(c, conjunto, pedido);
                comparar(conjunto, analise);
            }
        }
    }

    private static void executarRecusas(CenarioDocumento c, Conjunto conjunto) throws Exception {
        String pedido = c.criarRascunho(c.authA);
        for (Recusa recusa : conjunto.recusas()) {
            Arquivo arquivo = conjunto.arquivos().stream().filter(a -> a.nome().equals(recusa.arquivo()))
                    .findFirst().orElseThrow();
            Resposta resposta = c.enviar(c.authA, pedido, arquivo.tipoInformado(), arquivo.nome(), arquivo.bytes(),
                    null);
            assertEquals(recusa.status(), resposta.status(), conjunto.pasta() + " " + arquivo.nome() + ": "
                    + resposta.corpo());
            String codigo = resposta.json().get("codigo").asText();
            assertTrue(recusa.codigosAceitos().contains(codigo), conjunto.pasta() + " " + arquivo.nome()
                    + ": codigo " + codigo + " fora de " + recusa.codigosAceitos());
        }
        // Nada foi gravado: sem documento nao ha o que ler.
        assertFalse(c.lerDocumentos(c.authA, pedido).status() == 201, conjunto.pasta() + ": nao deve haver leitura");
    }

    private static void executarCorrigido(CenarioDocumento c, Conjunto corrigido) throws Exception {
        Conjunto original = ConjuntosSinteticos.porNumero(corrigido.originalDe());
        Pedido pedido = enviarELer(c, original);
        JsonNode primeira = analisar(c, original, pedido);
        comparar(original, primeira);
        int revisaoOriginal = primeira.get("revisao").asInt();
        c.cenario.relogio.avancar(Duration.ofSeconds(30));

        List<Arquivo> arquivos = corrigido.arquivos();
        for (int i = 0; i < arquivos.size(); i++) {
            Arquivo arquivo = arquivos.get(i);
            int alvo = corrigido.substitui()[i];
            Resposta resposta = alvo < 0
                    ? c.enviar(c.authA, pedido.id(), arquivo.tipoInformado(), arquivo.nome(), arquivo.bytes(), null)
                    : c.substituir(c.authA, pedido.id(), pedido.documentos().get(alvo), arquivo.tipoInformado(),
                            arquivo.nome(), arquivo.bytes(), null);
            assertEquals(201, resposta.status(), corrigido.pasta() + " " + arquivo.nome() + ": " + resposta.corpo());
        }
        String leitura = c.lerDocumentos(c.authA, pedido.id()).json().get("id").asText();
        Pedido reenviado = new Pedido(pedido.id(), pedido.documentos(), leitura);
        JsonNode segunda = analisar(c, corrigido, reenviado);
        comparar(corrigido, segunda);

        assertTrue(segunda.get("revisao").asInt() > revisaoOriginal, corrigido.pasta() + ": nova revisao");
        JsonNode h = historico(c, c.authA, pedido.id()).json();
        assertEquals(2, h.get("analises").size(), corrigido.pasta() + ": historico preserva a analise original");
        assertEquals(segunda.get("id").asText(), h.get("vigenteId").asText(), corrigido.pasta() + ": vigente");
        JsonNode antiga = h.get("analises").get(1);
        assertEquals(primeira.get("id").asText(), antiga.get("id").asText());
        assertFalse(antiga.get("vigente").asBoolean());
        assertEquals(primeira.get("resultados"), antiga.get("resultados"),
                corrigido.pasta() + ": o resultado do original nao muda");
    }

    private static Pedido enviarELer(CenarioDocumento c, Conjunto conjunto) throws Exception {
        String pedido = c.criarRascunho(c.authA);
        List<String> documentos = new ArrayList<>();
        for (Arquivo arquivo : ConjuntosSinteticos.arquivosEfetivos(conjunto)) {
            Resposta resposta = c.enviar(c.authA, pedido, arquivo.tipoInformado(), arquivo.nome(), arquivo.bytes(),
                    null);
            assertEquals(201, resposta.status(), conjunto.pasta() + " " + arquivo.nome() + ": " + resposta.corpo());
            documentos.add(resposta.json().get("id").asText());
        }
        Resposta leitura = c.lerDocumentos(c.authA, pedido);
        assertEquals(201, leitura.status(), conjunto.pasta() + ": leitura " + leitura.corpo());
        return new Pedido(pedido, documentos, leitura.json().get("id").asText());
    }

    private static JsonNode analisar(CenarioDocumento c, Conjunto conjunto, Pedido pedido) throws Exception {
        if (conjunto.conferencia() == Conferencia.TUDO_CONFIRMADO) {
            confirmarTudo(c, pedido);
        }
        Resposta analise = pedir(c, c.authA, pedido.id());
        assertEquals(201, analise.status(), conjunto.pasta() + ": analise " + analise.corpo());
        return analise.json();
    }

    private static void comparar(Conjunto conjunto, JsonNode analise) {
        assertEquals(conjunto.geral().name(), analise.get("resultadoGeral").asText(),
                conjunto.pasta() + ": resultado geral");
        assertEquals(ConjuntosSinteticos.REGRAS.size(), analise.get("resultados").size(),
                conjunto.pasta() + ": todas as seis regras avaliadas");
        for (Map.Entry<String, Veredicto> regra : conjunto.regras().entrySet()) {
            assertEquals(regra.getValue().resultado().name(), CenarioAnalise.resultado(analise, regra.getKey()),
                    conjunto.pasta() + ": " + regra.getKey());
            if (regra.getValue().responsavel() != null) {
                assertEquals(regra.getValue().responsavel().name(),
                        CenarioAnalise.pendencia(analise, regra.getKey()).get("responsavel").asText(),
                        conjunto.pasta() + ": responsavel de " + regra.getKey());
            }
        }
    }

    private static boolean contemMarca(Arquivo arquivo) throws IOException {
        String marca = "SEM VALOR REAL";
        return switch (arquivo.formato()) {
            case PDF -> {
                try (PDDocument documento = Loader.loadPDF(arquivo.bytes())) {
                    yield new PDFTextStripper().getText(documento).contains(marca);
                }
            }
            case PNG -> arquivo.linhas().stream().anyMatch(linha -> linha.contains(marca));
            case TXT -> new String(arquivo.bytes(), StandardCharsets.UTF_8).contains(marca);
        };
    }
}
