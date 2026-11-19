package br.com.regulacheck.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** E4.2 (RC-20, ajuste de 19/11/2026): parcelas de uma regra em "Regras com mais pendências". */
class TextosIndicadoresTest {

    @Test
    fun `mostra so as parcelas maiores que zero, no singular e no plural`() {
        assertEquals("5 não verificáveis", Textos.parcelasPendencias(pendentes = 0, naoVerificaveis = 5))
        assertEquals("1 pendente", Textos.parcelasPendencias(pendentes = 1, naoVerificaveis = 0))
        assertEquals("2 pendentes · 1 não verificável", Textos.parcelasPendencias(pendentes = 2, naoVerificaveis = 1))
    }

    @Test
    fun `sem parcelas na resposta nao mostra detalhe`() {
        assertNull(Textos.parcelasPendencias(pendentes = 0, naoVerificaveis = 0))
    }
}
