package br.com.regulacheck.armazenamento;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import br.com.regulacheck.armazenamento.conferencia.ArquivoConferido;
import br.com.regulacheck.armazenamento.conferencia.ConferenciaDeArquivo;
import br.com.regulacheck.armazenamento.conferencia.ResultadoConferencia;
import br.com.regulacheck.armazenamento.conferencia.TipoArquivo;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ArmazenamentoEmDiscoTest {

    @TempDir
    Path base;

    Path raiz;
    ArmazenamentoEmDisco armazenamento;
    ConferenciaDeArquivo conferencia;

    @BeforeEach
    void preparar() throws IOException {
        raiz = base.resolve("area-privada");
        armazenamento = new ArmazenamentoEmDisco(raiz);
        conferencia = ConferenciaDeArquivo.para(raiz);
    }

    private ArquivoConferido aceitar(byte[] conteudo, String nome) {
        return assertInstanceOf(ResultadoConferencia.Aceito.class,
                conferencia.conferir(new ByteArrayInputStream(conteudo), nome)).arquivo();
    }

    private long arquivosSoltos() throws IOException {
        try (Stream<Path> todos = Files.list(raiz)) {
            return todos.filter(p -> !p.getFileName().toString().equals(".quarentena")).count();
        }
    }

    @Test
    void guardarEAbrirDevolvemOMesmoConteudoComNomeInterno() throws Exception {
        byte[] conteudo = ArquivosDeTeste.pdf(2);
        ArquivoGuardado guardado = armazenamento.guardar(aceitar(conteudo, "meu laudo.pdf"));
        assertEquals(TipoArquivo.PDF, guardado.tipo());
        assertEquals(2, guardado.paginas());
        assertEquals(conteudo.length, guardado.tamanhoBytes());
        assertEquals("meu laudo.pdf", guardado.nomeExibicao());
        assertTrue(Files.isRegularFile(raiz.resolve(guardado.id().valor())), "nome final = identificador interno");
        try (InputStream lido = armazenamento.abrir(guardado.id())) {
            assertArrayEquals(conteudo, lido.readAllBytes());
        }
        assertEquals(1, arquivosSoltos(), "so o arquivo final; sem temporario nem copia com o nome original");
    }

    @Test
    void quarentenaFicaLimpaDepoisDeGuardar() throws Exception {
        armazenamento.guardar(aceitar(ArquivosDeTeste.png(), "foto.png"));
        try (Stream<Path> q = Files.list(raiz.resolve(".quarentena"))) {
            assertEquals(0, q.count());
        }
    }

    @Test
    void removerApagaEEIdempotente() throws Exception {
        ArquivoGuardado guardado = armazenamento.guardar(aceitar(ArquivosDeTeste.jpeg(), "foto.jpg"));
        armazenamento.remover(guardado.id());
        armazenamento.remover(guardado.id());
        assertEquals(0, arquivosSoltos());
        assertThrows(ArquivoAusenteException.class, () -> armazenamento.abrir(guardado.id()));
    }

    @Test
    void abrirIdInexistenteFalhaSemCaminhoNaMensagem() throws Exception {
        armazenamento.guardar(aceitar(ArquivosDeTeste.png(), "a.png"));
        ArquivoAusenteException erro = assertThrows(ArquivoAusenteException.class,
                () -> armazenamento.abrir(IdInterno.novo()));
        assertFalse(erro.getMessage().contains(raiz.toString()));
        assertFalse(erro.getMessage().contains("\\"));
    }

    @Test
    void linkSimbolicoParaForaDaRaizNaoEhSeguido() throws Exception {
        Path segredo = base.resolve("segredo.txt");
        Files.writeString(segredo, "conteudo fora da area privada");
        armazenamento.guardar(aceitar(ArquivosDeTeste.png(), "a.png"));
        IdInterno id = IdInterno.novo();
        try {
            Files.createSymbolicLink(raiz.resolve(id.valor()), segredo);
        } catch (IOException | UnsupportedOperationException | SecurityException e) {
            assumeTrue(false, "sistema nao permite criar link simbolico neste ambiente: " + e.getClass().getSimpleName());
        }
        assertThrows(ArquivoAusenteException.class, () -> armazenamento.abrir(id));
    }

    @Test
    void falhaNaGravacaoNaoDeixaArquivoParcialNemTemporario() throws Exception {
        Files.createDirectories(raiz);
        ArquivoConferido fantasma = new ArquivoConferido(base.resolve("nao-existe.tmp"), TipoArquivo.PDF, 10, 1, "x.pdf");
        ExcecaoNegocio erro = assertThrows(ExcecaoNegocio.class, () -> armazenamento.guardar(fantasma));
        assertEquals(503, erro.status());
        assertEquals("DEPENDENCIA_INDISPONIVEL", erro.codigo());
        assertFalse(erro.getMessage().contains(raiz.toString()));
        assertEquals(0, arquivosSoltos());
    }

    @Test
    void semAreaConfiguradaGuardarFalhaComErroClaro() throws Exception {
        ArmazenamentoEmDisco semRaiz = new ArmazenamentoEmDisco(null);
        ArquivoConferido arquivo = aceitar(ArquivosDeTeste.pdf(1), "a.pdf");
        ExcecaoNegocio erro = assertThrows(ExcecaoNegocio.class, () -> semRaiz.guardar(arquivo));
        assertEquals(503, erro.status());
        assertThrows(ArquivoAusenteException.class, () -> semRaiz.abrir(IdInterno.novo()));
        semRaiz.remover(IdInterno.novo());
        assertTrue(Files.notExists(arquivo.caminhoTemporario()), "quarentena descartada mesmo na falha");
    }

    @Test
    void doisArquivosComMesmoNomeOriginalGanhamIdentificadoresDiferentes() throws Exception {
        ArquivoGuardado a = armazenamento.guardar(aceitar(ArquivosDeTeste.pdf(1), "igual.pdf"));
        ArquivoGuardado b = armazenamento.guardar(aceitar(ArquivosDeTeste.pdf(1), "igual.pdf"));
        assertFalse(a.id().equals(b.id()));
        assertEquals(2, arquivosSoltos());
    }
}
