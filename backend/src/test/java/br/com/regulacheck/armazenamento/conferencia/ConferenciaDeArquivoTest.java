package br.com.regulacheck.armazenamento.conferencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.armazenamento.ArquivosDeTeste;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Conferencia de arquivos com um conjunto 100% sintetico (SC-001). */
class ConferenciaDeArquivoTest {

    @TempDir
    Path raiz;

    ConferenciaDeArquivo conferencia;

    @BeforeEach
    void preparar() throws IOException {
        conferencia = ConferenciaDeArquivo.para(raiz);
    }

    private ResultadoConferencia conferir(byte[] conteudo, String nome) {
        return conferencia.conferir(new ByteArrayInputStream(conteudo), nome);
    }

    private void assertRecusado(MotivoRecusa esperado, ResultadoConferencia resultado) {
        ResultadoConferencia.Recusado recusado = assertInstanceOf(ResultadoConferencia.Recusado.class, resultado);
        assertEquals(esperado, recusado.motivo());
        assertTrue(arquivosNaRaiz() == 0, "nada pode ficar gravado apos recusa");
    }

    private ArquivoConferido assertAceito(ResultadoConferencia resultado) {
        return assertInstanceOf(ResultadoConferencia.Aceito.class, resultado).arquivo();
    }

    private long arquivosNaRaiz() {
        try (Stream<Path> todos = Files.walk(raiz)) {
            return todos.filter(Files::isRegularFile).count();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void pdfDeUmaECincoPaginasSaoAceitos() {
        ArquivoConferido um = assertAceito(conferir(ArquivosDeTeste.pdf(1), "laudo.pdf"));
        assertEquals(TipoArquivo.PDF, um.tipo());
        assertEquals(1, um.paginas());
        ArquivoConferido cinco = assertAceito(conferir(ArquivosDeTeste.pdf(5), "Laudo.PDF"));
        assertEquals(5, cinco.paginas());
    }

    @Test
    void jpegEPngValidosSaoAceitos() {
        assertEquals(TipoArquivo.JPEG, assertAceito(conferir(ArquivosDeTeste.jpeg(), "foto.jpg")).tipo());
        assertEquals(TipoArquivo.JPEG, assertAceito(conferir(ArquivosDeTeste.jpeg(), "foto.jpeg")).tipo());
        ArquivoConferido png = assertAceito(conferir(ArquivosDeTeste.png(), "foto.png"));
        assertEquals(TipoArquivo.PNG, png.tipo());
        assertEquals(1, png.paginas());
    }

    @Test
    void textoComExtensaoPdfEhRecusadoPorTipo() {
        assertRecusado(MotivoRecusa.TIPO_NAO_SUPORTADO, conferir(ArquivosDeTeste.texto(), "laudo.pdf"));
    }

    @Test
    void executavelComExtensaoPdfEhRecusadoPorTipo() {
        assertRecusado(MotivoRecusa.TIPO_NAO_SUPORTADO, conferir(ArquivosDeTeste.executavel(), "laudo.pdf"));
    }

    @Test
    void pngComExtensaoJpgEhRecusadoPorExtensao() {
        assertRecusado(MotivoRecusa.EXTENSAO_INCOMPATIVEL, conferir(ArquivosDeTeste.png(), "foto.jpg"));
    }

    @Test
    void duplaExtensaoEnganosaEhRecusada() {
        assertRecusado(MotivoRecusa.EXTENSAO_INCOMPATIVEL, conferir(ArquivosDeTeste.pdf(1), "laudo.pdf.exe"));
        assertRecusado(MotivoRecusa.EXTENSAO_INCOMPATIVEL, conferir(ArquivosDeTeste.png(), "foto.png.pdf"));
    }

    @Test
    void semExtensaoOuNomeNuloEhRecusado() {
        assertRecusado(MotivoRecusa.EXTENSAO_INCOMPATIVEL, conferir(ArquivosDeTeste.pdf(1), "laudo"));
        assertRecusado(MotivoRecusa.EXTENSAO_INCOMPATIVEL, conferir(ArquivosDeTeste.pdf(1), null));
    }

    @Test
    void cinco_MbExatosPassamEUmByteAMaisEhRecusado() {
        int limite = (int) LimitesArquivo.MAXIMO_BYTES;
        ArquivoConferido exato = assertAceito(conferir(ArquivosDeTeste.pdfComTamanho(limite), "grande.pdf"));
        assertEquals(limite, exato.tamanhoBytes());
        exato.descartar();
        assertRecusado(MotivoRecusa.TAMANHO_EXCEDIDO, conferir(ArquivosDeTeste.pdfComTamanho(limite + 1), "grande.pdf"));
    }

    @Test
    void leituraParaNoLimiteSemConfiarEmFluxoSemFim() {
        AtomicLong lidos = new AtomicLong();
        InputStream infinito = ArquivosDeTeste.fluxoInfinitoPdf();
        InputStream contado = new InputStream() {
            @Override
            public int read() throws IOException {
                lidos.incrementAndGet();
                return infinito.read();
            }
        };
        assertRecusado(MotivoRecusa.TAMANHO_EXCEDIDO, conferencia.conferir(contado, "sem-fim.pdf"));
        assertTrue(lidos.get() <= LimitesArquivo.MAXIMO_BYTES + 1, "leu alem do limite: " + lidos.get());
    }

    @Test
    void pdfComMaisDeCincoPaginasEhRecusado() {
        assertRecusado(MotivoRecusa.EXCESSO_DE_PAGINAS, conferir(ArquivosDeTeste.pdf(6), "longo.pdf"));
    }

    @Test
    void pdfProtegidoPorSenhaEhRecusadoComMotivoProprio() {
        assertRecusado(MotivoRecusa.PDF_PROTEGIDO, conferir(ArquivosDeTeste.pdfProtegido(), "protegido.pdf"));
    }

    @Test
    void pdfTruncadoArquivoVazioEImagemTruncadaSaoIlegiveis() {
        assertRecusado(MotivoRecusa.ARQUIVO_ILEGIVEL, conferir(ArquivosDeTeste.pdfTruncado(), "cortado.pdf"));
        assertRecusado(MotivoRecusa.ARQUIVO_ILEGIVEL, conferir(new byte[0], "vazio.pdf"));
        assertRecusado(MotivoRecusa.ARQUIVO_ILEGIVEL, conferir(ArquivosDeTeste.pngTruncado(), "cortada.png"));
    }

    @Test
    void imagemAcimaDoLimiteDePixelsEhRecusadaSemDecodificar() {
        assertRecusado(MotivoRecusa.ARQUIVO_ILEGIVEL, conferir(ArquivosDeTeste.pngDimensaoExtrema(), "bomba.png"));
    }

    @Test
    void contentTypeDeclaradoNaoInfluencia() {
        // A API de conferencia nem recebe o Content-Type: so conteudo e nome decidem.
        assertRecusado(MotivoRecusa.TIPO_NAO_SUPORTADO, conferir(ArquivosDeTeste.texto(), "foto.png"));
    }

    @Test
    void pdfComJavaScriptEAnexoEhAceitoSemExecutarNada() {
        ArquivoConferido arquivo = assertAceito(conferir(ArquivosDeTeste.pdfComConteudoAtivo(), "ativo.pdf"));
        assertEquals(TipoArquivo.PDF, arquivo.tipo());
        assertEquals(1, arquivo.paginas());
        // Nenhum arquivo extra foi extraido do anexo embutido: so o temporario da quarentena existe.
        assertEquals(1, arquivosNaRaiz());
    }

    @Test
    void recusaNaoDeixaNadaNaQuarentena() {
        conferir(ArquivosDeTeste.texto(), "a.pdf");
        conferir(ArquivosDeTeste.png(), "a.jpg");
        conferir(ArquivosDeTeste.pdf(9), "a.pdf");
        assertEquals(0, arquivosNaRaiz());
    }

    @Test
    void funcionaSemAreaDeArquivosConfigurada() throws IOException {
        ConferenciaDeArquivo semRaiz = ConferenciaDeArquivo.para(null);
        ResultadoConferencia ok = semRaiz.conferir(new ByteArrayInputStream(ArquivosDeTeste.pdf(1)), "a.pdf");
        ArquivoConferido arquivo = assertInstanceOf(ResultadoConferencia.Aceito.class, ok).arquivo();
        assertTrue(Files.exists(arquivo.caminhoTemporario()));
        arquivo.descartar();
        assertTrue(Files.notExists(arquivo.caminhoTemporario()));
    }

    @Test
    void nomeDeExibicaoEhSaneadoNoResultado() {
        ArquivoConferido arquivo = assertAceito(conferir(ArquivosDeTeste.pdf(1), "..\\..\\evil\r\nnome.pdf"));
        assertEquals("evilnome.pdf", arquivo.nomeExibicao());
    }
}
