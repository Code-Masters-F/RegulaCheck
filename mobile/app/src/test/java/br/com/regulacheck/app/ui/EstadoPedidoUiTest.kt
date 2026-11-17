package br.com.regulacheck.app.ui

import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Estado do pedido → rótulo, mensagem e ação (data-model.md; textos de mobile/design/textos.md). */
class EstadoPedidoUiTest {

    @Test
    fun `valor desconhecido vira Estado nao reconhecido sem acao`() {
        val estado = EstadoPedido.daApi("ESTADO_FUTURO")
        val apresentacao = apresentarEstado(estado, 0)

        assertEquals(EstadoPedido.DESCONHECIDO, estado)
        assertEquals("Estado não reconhecido", apresentacao.rotulo)
        assertNull(apresentacao.acao)
        assertEquals(EstadoPedido.DESCONHECIDO, EstadoPedido.daApi("DESCONHECIDO"))
    }

    @Test
    fun `rascunho depende de haver documentos`() {
        assertEquals(AcaoPedido.ANEXAR_DOCUMENTOS, apresentarEstado(EstadoPedido.RASCUNHO, 0).acao)
        assertEquals(AcaoPedido.LER_DOCUMENTOS, apresentarEstado(EstadoPedido.RASCUNHO, 2).acao)
    }

    @Test
    fun `cada estado tem o rotulo e a acao do desenho`() {
        val esperado = mapOf(
            "PROCESSANDO" to ("Em verificação" to AcaoPedido.ATUALIZAR),
            "REQUER_CONFERENCIA" to ("Confira a leitura" to AcaoPedido.VER_O_QUE_CONFERIR),
            "COM_PENDENCIAS" to ("Com pendências" to AcaoPedido.VER_PENDENCIAS),
            "SEM_PENDENCIAS_IDENTIFICADAS" to ("Sem pendências identificadas" to AcaoPedido.VER_RESULTADO),
            "FALHA_ANALISE" to ("Falha na verificação" to AcaoPedido.TENTAR_DE_NOVO),
            "SUBMETIDA_SIMULACAO" to ("Simulação concluída" to null),
        )
        esperado.forEach { (valor, rotuloEAcao) ->
            val apresentacao = apresentarEstado(EstadoPedido.daApi(valor), 3)
            assertEquals(valor, rotuloEAcao.first, apresentacao.rotulo)
            assertEquals(valor, rotuloEAcao.second, apresentacao.acao)
        }
    }

    @Test
    fun `so Atualizar funciona nesta historia`() {
        assertEquals(listOf(AcaoPedido.ATUALIZAR), AcaoPedido.entries.filter { it.disponivel })
    }

    @Test
    fun `resultado geral usa os rotulos do estado`() {
        assertNull(rotuloResultadoGeral(null))
        assertEquals("Com pendências", rotuloResultadoGeral("COM_PENDENCIAS"))
        assertEquals("Estado não reconhecido", rotuloResultadoGeral("OUTRO"))
    }

    @Test
    fun `data da API aparece no fuso da interface`() {
        assertEquals("12/11/2026 às 10:06", formatarDataHora("2026-11-12T13:06:00Z", ZoneId.of("America/Bahia")))
        assertEquals("data-ruim", formatarDataHora("data-ruim"))
    }
}
