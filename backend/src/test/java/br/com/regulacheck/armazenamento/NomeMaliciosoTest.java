package br.com.regulacheck.armazenamento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.armazenamento.conferencia.ArquivoConferido;
import br.com.regulacheck.armazenamento.conferencia.ConferenciaDeArquivo;
import br.com.regulacheck.armazenamento.conferencia.LimitesArquivo;
import br.com.regulacheck.armazenamento.conferencia.ResultadoConferencia;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Nomes maliciosos viram texto de exibicao e nunca influenciam o local de gravacao (SC-002). */
class NomeMaliciosoTest {

    @TempDir
    Path base;

    private static final List<String> NOMES = List.of(
            "../../etc/passwd.pdf",
            "..\\..\\Windows\\win.ini.pdf",
            "/etc/shadow.pdf",
            "C:\\Windows\\System32\\config.pdf",
            "con.pdf",
            "NUL.pdf",
            "aux",
            "....",
            "laudo\r\nSet-Cookie: x=1.pdf",
            "laudo\u202Efdp.exe.pdf",
            "x".repeat(10_000) + ".pdf",
            "   ",
            "relatório médico – ação.pdf");

    @Test
    void nomeEhSempreSaneadoParaExibicao() {
        for (String nome : NOMES) {
            String exibicao = NomeExibicao.sanear(nome);
            assertFalse(exibicao.isBlank(), nome);
            assertFalse(exibicao.contains("/") || exibicao.contains("\\"), "separador em: " + exibicao);
            assertFalse(exibicao.contains("\r") || exibicao.contains("\n"), "CR/LF em: " + exibicao);
            assertFalse(exibicao.contains("\u202E"), "marcador bidirecional em: " + exibicao);
            assertTrue(exibicao.codePointCount(0, exibicao.length()) <= LimitesArquivo.MAXIMO_NOME_EXIBICAO);
        }
    }

    @Test
    void casosEspecificosDeSaneamento() {
        assertEquals("passwd.pdf", NomeExibicao.sanear("../../etc/passwd.pdf"));
        assertEquals("win.ini.pdf", NomeExibicao.sanear("..\\..\\Windows\\win.ini.pdf"));
        assertEquals("_con.pdf", NomeExibicao.sanear("con.pdf"));
        assertEquals(NomeExibicao.PADRAO, NomeExibicao.sanear("...."));
        assertEquals(NomeExibicao.PADRAO, NomeExibicao.sanear("   "));
        assertEquals(NomeExibicao.PADRAO, NomeExibicao.sanear(null));
        assertEquals("laudoSet-Cookie_ x=1.pdf", NomeExibicao.sanear("laudo\r\nSet-Cookie: x=1.pdf"));
        assertEquals("relatório médico – ação.pdf", NomeExibicao.sanear("relatório médico – ação.pdf"));
        String longo = NomeExibicao.sanear("x".repeat(10_000) + ".pdf");
        assertEquals(LimitesArquivo.MAXIMO_NOME_EXIBICAO, longo.length());
        assertTrue(longo.endsWith(".pdf"));
    }

    @Test
    void nenhumNomeFazGravarForaDaRaiz() throws IOException {
        Path raiz = Files.createDirectory(base.resolve("area-privada"));
        Path vizinho = base.resolve("fora");
        Files.createDirectory(vizinho);
        ConferenciaDeArquivo conferencia = ConferenciaDeArquivo.para(raiz);
        ArmazenamentoEmDisco armazenamento = new ArmazenamentoEmDisco(raiz);
        for (String nome : NOMES) {
            // Mesmo que a extensao fosse aceita, o nome so serve para exibicao.
            ResultadoConferencia r = conferencia.conferir(new ByteArrayInputStream(ArquivosDeTeste.pdf(1)),
                    nome.endsWith(".pdf") ? nome : nome + ".pdf");
            if (r instanceof ResultadoConferencia.Aceito aceito) {
                ArquivoConferido arquivo = aceito.arquivo();
                ArquivoGuardado guardado = armazenamento.guardar(arquivo);
                assertTrue(guardado.id().valor().matches("[0-9a-f]{32}"));
                // Pontos seguidos sao inofensivos como texto; o que nao pode haver sao separadores.
                assertFalse(guardado.nomeExibicao().contains("/") || guardado.nomeExibicao().contains("\\"), nome);
            }
        }
        try (Stream<Path> fora = Files.walk(vizinho)) {
            assertEquals(1, fora.count(), "nada pode ser criado fora da raiz");
        }
        try (Stream<Path> naRaiz = Files.list(raiz)) {
            naRaiz.filter(p -> !p.getFileName().toString().equals(".quarentena")).forEach(p -> assertTrue(
                    p.getFileName().toString().matches("[0-9a-f]{32}"),
                    "arquivo com nome inesperado: " + p.getFileName()));
        }
    }

    @Test
    void identificadorForaDoFormatoNaoViraCaminho() {
        for (String ruim : List.of("..", "../x", "..\\x", "a/b", "C:", "con", "", " ",
                "0123456789abcdef0123456789ABCDEF", "0123456789abcdef0123456789abcde")) {
            assertThrows(IllegalArgumentException.class, () -> IdInterno.de(ruim), ruim);
        }
        assertThrows(IllegalArgumentException.class, () -> IdInterno.de(null));
    }
}
