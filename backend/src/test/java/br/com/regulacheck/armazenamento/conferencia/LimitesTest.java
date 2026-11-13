package br.com.regulacheck.armazenamento.conferencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.regulacheck.armazenamento.ArquivosDeTeste;
import br.com.regulacheck.common.exception.ExcecaoNegocio;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class LimitesTest {

    @Test
    void limitesDoMvpSaoOsDocumentados() {
        assertEquals(3, LimitesArquivo.MAXIMO_ARQUIVOS);
        assertEquals(5_242_880L, LimitesArquivo.MAXIMO_BYTES);
        assertEquals(5, LimitesArquivo.MAXIMO_PAGINAS_PDF);
    }

    @Test
    void tresArquivosPassamEQuatroNao() {
        LimitesArquivo.exigirQuantidade(0);
        LimitesArquivo.exigirQuantidade(3);
        ExcecaoNegocio erro = assertThrows(ExcecaoNegocio.class, () -> LimitesArquivo.exigirQuantidade(4));
        assertEquals(400, erro.status());
        assertEquals("REQUISICAO_INVALIDA", erro.codigo());
        assertEquals("arquivos", erro.detalhes().get(0).campo());
    }

    @Test
    void assinaturaDefineOTipoReal() {
        assertEquals(TipoArquivo.PDF, TipoArquivo.porAssinatura(ArquivosDeTeste.pdf(1)).orElseThrow());
        assertEquals(TipoArquivo.PNG, TipoArquivo.porAssinatura(ArquivosDeTeste.png()).orElseThrow());
        assertEquals(TipoArquivo.JPEG, TipoArquivo.porAssinatura(ArquivosDeTeste.jpeg()).orElseThrow());
        assertTrue(TipoArquivo.porAssinatura(ArquivosDeTeste.texto()).isEmpty());
        assertTrue(TipoArquivo.porAssinatura(ArquivosDeTeste.executavel()).isEmpty());
        assertTrue(TipoArquivo.porAssinatura(new byte[0]).isEmpty());
        assertTrue(TipoArquivo.porAssinatura(Arrays.copyOf(ArquivosDeTeste.png(), 4)).isEmpty());
    }

    @Test
    void extensaoNaoDiferenciaCaixaMasExigeCorrespondencia() {
        assertTrue(TipoArquivo.PDF.aceitaExtensao("Laudo.PDF"));
        assertTrue(TipoArquivo.JPEG.aceitaExtensao("foto.jpg"));
        assertTrue(TipoArquivo.JPEG.aceitaExtensao("foto.JPEG"));
        assertTrue(TipoArquivo.PNG.aceitaExtensao("foto.png"));
        assertFalse(TipoArquivo.PNG.aceitaExtensao("foto.jpg"));
        assertFalse(TipoArquivo.PDF.aceitaExtensao("laudo.pdf.exe"));
        assertFalse(TipoArquivo.PDF.aceitaExtensao("laudo"));
        assertFalse(TipoArquivo.PDF.aceitaExtensao("laudo."));
        assertFalse(TipoArquivo.PDF.aceitaExtensao(null));
    }
}
