package br.com.regulacheck.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/** Rótulos dos valores da API (spec 015 §2) e valores desconhecidos neutros (RC-19). */
class RotulosApiTest {

    @Test
    fun `valores conhecidos usam os rotulos da spec 015`() {
        assertEquals("Identificação ou cartão do SUS", RotulosApi.tipoDocumental("IDENTIFICACAO"))
        assertEquals("Não se aplica", RotulosApi.resultadoRegra("NAO_APLICAVEL"))
        assertEquals("Unidade emissora", RotulosApi.responsavel("UNIDADE_EMISSORA"))
        assertEquals("Quem resolve: unidade emissora", RotulosApi.quemResolve("UNIDADE_EMISSORA"))
        assertEquals("Quem resolve: você", RotulosApi.quemResolve("PACIENTE"))
        assertEquals("A leitura ainda não foi conferida", RotulosApi.motivo("TRANSCRICAO_NAO_CONFERIDA"))
    }

    @Test
    fun `valores desconhecidos ficam neutros e nunca negativos`() {
        assertEquals("Documento", RotulosApi.tipoDocumental("NOVO_TIPO"))
        assertEquals("Situação não reconhecida", RotulosApi.resultadoRegra("NOVO"))
        assertEquals("Não informado", RotulosApi.responsavel("NOVO"))
        assertEquals("Motivo não reconhecido", RotulosApi.motivo("NOVO"))
    }

    @Test
    fun `arquivo mostra formato, tamanho e paginas so em PDF`() {
        assertEquals("PDF · 184 KB · 2 páginas", RotulosApi.detalheArquivo("PDF", 188_416, 2))
        assertEquals("PDF · 127 KB · 1 página", RotulosApi.detalheArquivo("PDF", 130_048, 1))
        assertEquals("JPG · 1,2 MB", RotulosApi.detalheArquivo("JPEG", 1_258_291, 1))
        assertEquals("PNG · 86 KB", RotulosApi.detalheArquivo("PNG", 88_064, 1))
    }

    @Test
    fun `versao do protocolo sem o prefixo v`() {
        assertEquals("0.1", RotulosApi.versaoProtocolo("v0.1"))
        assertEquals("versao-x", RotulosApi.versaoProtocolo("versao-x"))
    }
}
