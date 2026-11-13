package br.com.regulacheck.extracao;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.documento.CenarioDocumento;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** A leitura e SIMULADA (decisao de 12/11/2026): nenhuma leitura real de arquivo e toda resposta se identifica. */
class SemLeituraRealTest {

    private static final Path FONTES = Path.of("src", "main", "java", "br", "com", "regulacheck");

    @Test
    void nenhumaClasseDeLeituraOuExtracaoUsaBibliotecaDeLeituraDeArquivo() throws IOException {
        for (String pacote : List.of("integracao/extracao", "extracao")) {
            try (Stream<Path> arquivos = Files.walk(FONTES.resolve(pacote))) {
                for (Path fonte : arquivos.filter(p -> p.toString().endsWith(".java")).toList()) {
                    String texto = Files.readString(fonte, StandardCharsets.UTF_8);
                    for (String proibido : List.of("org.apache.pdfbox", "javax.imageio", "java.awt", "tesseract",
                            "java.net.http", "java.net.URL")) {
                        assertFalse(texto.contains("import " + proibido) || texto.contains(proibido + "."),
                                fonte + " nao pode usar " + proibido);
                    }
                }
            }
        }
    }

    @Test
    void leitorSimuladoDeclaraSerSimuladoNoJavadocENaoOuveNomeNemExtensao() throws IOException {
        String texto = Files.readString(FONTES.resolve("integracao/extracao/LeitorSimulado.java"),
                StandardCharsets.UTF_8);
        assertTrue(texto.contains("SIMULADO"));
        assertFalse(texto.contains("nomeArquivo"));
        assertFalse(texto.contains("getName"));
    }

    @Test
    void todaRespostaDeLeituraTrazSimuladaTrueEOAvisoFixo() throws Exception {
        try (CenarioDocumento c = CenarioExtracao.iniciarComSimulado()) {
            String pedido = CenarioExtracao.pedidoCom(c, c.authA, AmostrasSinteticas.ENCAMINHAMENTO);
            var criada = c.lerDocumentos(c.authA, pedido).json();
            var repetida = c.lerDocumentos(c.authA, pedido).json();
            var consulta = c.consultarLeitura(c.authA, pedido).json();
            for (var corpo : List.of(criada, repetida, consulta)) {
                assertTrue(corpo.get("simulada").asBoolean());
                assertTrue(corpo.get("aviso").asText().startsWith("Leitura simulada"));
            }
        }
    }
}
