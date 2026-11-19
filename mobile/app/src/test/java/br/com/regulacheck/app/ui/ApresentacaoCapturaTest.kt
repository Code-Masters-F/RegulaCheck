package br.com.regulacheck.app.ui

import br.com.regulacheck.app.api.DISTANCIA_DENTRO
import br.com.regulacheck.app.api.DISTANCIA_PERTO
import br.com.regulacheck.app.api.DISTANCIA_SEM_LEITURA
import br.com.regulacheck.app.api.JsonApi
import br.com.regulacheck.app.api.LUMINOSIDADE_BAIXA
import br.com.regulacheck.app.api.LUMINOSIDADE_DENTRO
import br.com.regulacheck.app.api.LUMINOSIDADE_SEM_LEITURA
import br.com.regulacheck.app.api.Telemetria
import br.com.regulacheck.app.api.sensor
import br.com.regulacheck.app.api.telemetria
import br.com.regulacheck.app.ui.captura.ApresentacaoCaptura
import br.com.regulacheck.app.ui.captura.MemoriaCaptura
import br.com.regulacheck.app.ui.captura.SituacaoSensorUi
import br.com.regulacheck.app.ui.captura.apresentarTelemetria
import br.com.regulacheck.app.ui.captura.formatarIdade
import br.com.regulacheck.app.ui.captura.formatarValor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Estados da E4.1 (quadros o3kkGI, fOrmB, NyDDm, DGf2T, Cna25, kaHJz) a partir de respostas do
 * contrato da spec 016, com os textos de `mobile/design/textos.md` (RC-22, spec 023).
 */
class ApresentacaoCapturaTest {

    private fun ler(corpo: String): Telemetria = JsonApi.decodeFromString(Telemetria.serializer(), corpo)

    /** Aplica as consultas em sequência, como na mesma visita; devolve a última apresentação. */
    private fun visita(vararg corpos: String): ApresentacaoCaptura {
        var memoria = MemoriaCaptura()
        var ultima: ApresentacaoCaptura? = null
        corpos.forEach { corpo ->
            val r = apresentarTelemetria(ler(corpo), memoria)
            memoria = r.memoria
            ultima = r.apresentacao
        }
        return ultima!!
    }

    @Test
    fun `normal mostra os dois sensores adequados com valor unidade e idade`() {
        val a = visita(telemetria(LUMINOSIDADE_DENTRO, DISTANCIA_DENTRO))

        val (luz, distancia) = a.sensores
        assertEquals("Luminosidade", luz.nome)
        assertEquals("410 lux", luz.valor)
        assertEquals(SituacaoSensorUi.ADEQUADA, luz.situacao)
        assertEquals("Adequada", luz.rotulo)
        assertNull(luz.orientacao)
        assertEquals("Leitura há 5 s · sensor simulado", luz.linhaLeitura)
        assertEquals("Distância", distancia.nome)
        assertEquals("31,5 cm", distancia.valor)
        assertFalse(a.indisponivel)
        assertFalse(a.mesmoAssim)
    }

    @Test
    fun `luminosidade baixa mostra a orientacao da API e nao muda a distancia`() {
        val a = visita(telemetria(LUMINOSIDADE_BAIXA, DISTANCIA_DENTRO))

        assertEquals(SituacaoSensorUi.BAIXA, a.sensores[0].situacao)
        assertEquals("Baixa", a.sensores[0].rotulo)
        assertEquals("Ilumine melhor o documento antes de capturar.", a.sensores[0].orientacao)
        assertEquals(SituacaoSensorUi.ADEQUADA, a.sensores[1].situacao)
        assertFalse("fora da faixa não vira 'mesmo assim' (quadro fOrmB)", a.mesmoAssim)
    }

    @Test
    fun `luminosidade acima da faixa e distancia fora para os dois lados`() {
        val alta = visita(telemetria(sensor("LUMINOSIDADE", "ACIMA_DA_FAIXA", 1500.0, codigo = "REDUZIR_ILUMINACAO_OU_REFLEXO", mensagem = "Reduza o reflexo."), DISTANCIA_PERTO))
        val longe = visita(telemetria(LUMINOSIDADE_DENTRO, sensor("DISTANCIA", "ACIMA_DA_FAIXA", 60.0, codigo = "APROXIMAR_DOCUMENTO", mensagem = "Aproxime o celular do documento.")))

        assertEquals("Alta", alta.sensores[0].rotulo)
        assertEquals("1.500 lux", alta.sensores[0].valor)
        assertEquals(SituacaoSensorUi.FORA_DO_IDEAL, alta.sensores[1].situacao)
        assertEquals("Fora do ideal", alta.sensores[1].rotulo)
        assertEquals("Afaste o celular do documento.", alta.sensores[1].orientacao)
        assertEquals("Fora do ideal", longe.sensores[1].rotulo)
        assertEquals("Aproxime o celular do documento.", longe.sensores[1].orientacao)
    }

    @Test
    fun `volta a faixa mostra adequada de novo com o valor anterior e mantem enquanto dentro`() {
        val dentroDepois = sensor("LUMINOSIDADE", "DENTRO_DA_FAIXA", 410.0, medidaEm = "2026-11-18T14:00:26Z")
        val recuperada = visita(telemetria(LUMINOSIDADE_BAIXA, DISTANCIA_DENTRO), telemetria(dentroDepois, DISTANCIA_DENTRO))
        val continua = visita(
            telemetria(LUMINOSIDADE_BAIXA, DISTANCIA_DENTRO),
            telemetria(dentroDepois, DISTANCIA_DENTRO),
            telemetria(LUMINOSIDADE_DENTRO, DISTANCIA_DENTRO),
        )

        val luz = recuperada.sensores[0]
        assertEquals(SituacaoSensorUi.ADEQUADA_DE_NOVO, luz.situacao)
        assertEquals("Adequada de novo", luz.rotulo)
        assertEquals("Condições adequadas de novo.", luz.orientacao)
        assertEquals("Leitura há 4 s · antes: 60 lux · sensor simulado", luz.linhaLeitura)
        assertEquals("a distância nunca saiu: continua só adequada", SituacaoSensorUi.ADEQUADA, recuperada.sensores[1].situacao)
        assertEquals(SituacaoSensorUi.ADEQUADA_DE_NOVO, continua.sensores[0].situacao)
    }

    @Test
    fun `sair da faixa de novo apaga a recuperacao e a proxima volta usa o novo valor`() {
        val a = visita(
            telemetria(LUMINOSIDADE_BAIXA, DISTANCIA_DENTRO),
            telemetria(LUMINOSIDADE_DENTRO, DISTANCIA_DENTRO),
            telemetria(sensor("LUMINOSIDADE", "ABAIXO_DA_FAIXA", 90.0), DISTANCIA_DENTRO),
        )
        val b = visita(
            telemetria(LUMINOSIDADE_BAIXA, DISTANCIA_DENTRO),
            telemetria(LUMINOSIDADE_DENTRO, DISTANCIA_DENTRO),
            telemetria(sensor("LUMINOSIDADE", "ABAIXO_DA_FAIXA", 90.0), DISTANCIA_DENTRO),
            telemetria(LUMINOSIDADE_DENTRO, DISTANCIA_DENTRO),
        )

        assertEquals(SituacaoSensorUi.BAIXA, a.sensores[0].situacao)
        assertEquals("Leitura há 5 s · antes: 90 lux · sensor simulado", b.sensores[0].linhaLeitura)
    }

    @Test
    fun `primeira consulta dentro da faixa nao e recuperada`() {
        val a = visita(telemetria(LUMINOSIDADE_DENTRO, DISTANCIA_DENTRO), telemetria(LUMINOSIDADE_DENTRO, DISTANCIA_DENTRO))

        assertTrue(a.sensores.all { it.situacao == SituacaoSensorUi.ADEQUADA })
    }

    @Test
    fun `fora depois desatualizada depois dentro conta como recuperada`() {
        val desatualizada = sensor("LUMINOSIDADE", "DESATUALIZADA", 60.0, medidaEm = "2026-11-18T13:57:30Z", codigo = "VERIFICAR_ESTACAO", mensagem = "Verifique se a estação de captura está ligada.")
        val a = visita(
            telemetria(LUMINOSIDADE_BAIXA, DISTANCIA_DENTRO),
            telemetria(desatualizada, DISTANCIA_DENTRO),
            telemetria(LUMINOSIDADE_DENTRO, DISTANCIA_DENTRO),
        )

        assertEquals(SituacaoSensorUi.ADEQUADA_DE_NOVO, a.sensores[0].situacao)
    }

    @Test
    fun `recuperada depois desatualizada depois dentro volta a ser so adequada`() {
        // research D5: a recuperação já foi mostrada; a interrupção encerra a marca "de novo".
        val desatualizada = sensor("LUMINOSIDADE", "DESATUALIZADA", 410.0, medidaEm = "2026-11-18T13:57:30Z", codigo = "VERIFICAR_ESTACAO", mensagem = "Verifique se a estação de captura está ligada.")
        val a = visita(
            telemetria(LUMINOSIDADE_BAIXA, DISTANCIA_DENTRO),
            telemetria(LUMINOSIDADE_DENTRO, DISTANCIA_DENTRO),
            telemetria(desatualizada, DISTANCIA_DENTRO),
            telemetria(LUMINOSIDADE_DENTRO, DISTANCIA_DENTRO),
        )
        val b = visita(
            telemetria(LUMINOSIDADE_BAIXA, DISTANCIA_DENTRO),
            telemetria(LUMINOSIDADE_DENTRO, DISTANCIA_DENTRO),
            telemetria(LUMINOSIDADE_SEM_LEITURA, DISTANCIA_SEM_LEITURA),
            telemetria(LUMINOSIDADE_DENTRO, DISTANCIA_DENTRO),
        )

        assertEquals(SituacaoSensorUi.ADEQUADA, a.sensores[0].situacao)
        assertEquals(SituacaoSensorUi.ADEQUADA, b.sensores[0].situacao)
    }

    @Test
    fun `desatualizada mostra a idade da ultima leitura e pede mesmo assim`() {
        val antiga = "2026-11-18T13:57:30Z"
        val a = visita(
            telemetria(
                sensor("LUMINOSIDADE", "DESATUALIZADA", 410.0, medidaEm = antiga, codigo = "VERIFICAR_ESTACAO", mensagem = "Verifique se a estação de captura está ligada."),
                sensor("DISTANCIA", "DESATUALIZADA", 31.5, medidaEm = antiga, codigo = "VERIFICAR_ESTACAO", mensagem = "Verifique se a estação de captura está ligada."),
            ),
        )

        assertEquals("Desatualizada", a.sensores[0].rotulo)
        assertEquals("410 lux", a.sensores[0].valor)
        assertEquals("Última leitura há 3 min · sensor simulado", a.sensores[0].linhaLeitura)
        assertEquals("Verifique se a estação de captura está ligada.", a.sensores[0].orientacao)
        assertTrue(a.mesmoAssim)
        assertFalse(a.indisponivel)
    }

    @Test
    fun `os dois sem leitura sem sessao viram indisponivel`() {
        val a = visita(telemetria(LUMINOSIDADE_SEM_LEITURA, DISTANCIA_SEM_LEITURA, comSessao = false))

        assertTrue(a.indisponivel)
        assertTrue(a.mesmoAssim)
        assertTrue(a.sensores.all { it.situacao == SituacaoSensorUi.INDISPONIVEL && it.valor == "—" })
        assertEquals("Sensores indisponíveis agora", a.sensores[0].rotulo)
        assertEquals("Você pode anexar e salvar seus documentos normalmente.", a.sensores[0].orientacao)
        assertNull(a.sensores[1].orientacao)
        assertEquals("Sem leitura disponível · sensor simulado", a.sensores[1].linhaLeitura)
    }

    @Test
    fun `so um sensor sem leitura mostra sem leitura e a orientacao da API`() {
        val a = visita(telemetria(LUMINOSIDADE_DENTRO, DISTANCIA_SEM_LEITURA))

        assertFalse(a.indisponivel)
        assertEquals(SituacaoSensorUi.SEM_LEITURA, a.sensores[1].situacao)
        assertEquals("Sem leitura", a.sensores[1].rotulo)
        assertEquals("—", a.sensores[1].valor)
        assertEquals("Sem leitura do sensor. Você pode capturar e enviar o documento normalmente.", a.sensores[1].orientacao)
        assertTrue(a.mesmoAssim)
    }

    @Test
    fun `situacao desconhecida e orientacao vazia nao quebram a tela`() {
        val a = visita(
            telemetria(
                sensor("LUMINOSIDADE", "NOVA_SITUACAO", 410.0, codigo = "CAPTURA_SEGUE_SEM_SENSOR", mensagem = ""),
                sensor("DISTANCIA", "ABAIXO_DA_FAIXA", 8.0, codigo = "AFASTAR_DOCUMENTO", mensagem = " "),
            ),
        )

        assertEquals(SituacaoSensorUi.SEM_LEITURA, a.sensores[0].situacao)
        assertEquals(Textos.PODE_ANEXAR_NORMALMENTE, a.sensores[0].orientacao)
        assertEquals("Afaste o celular do documento.", a.sensores[1].orientacao)
    }

    @Test
    fun `idade usa o relogio do servidor e nunca e negativa`() {
        val futura = sensor("LUMINOSIDADE", "DENTRO_DA_FAIXA", 410.0, medidaEm = "2026-11-18T14:00:40Z")
        val a = visita(telemetria(futura, DISTANCIA_DENTRO, consultadaEm = "2026-11-18T14:00:30.500Z"))

        assertEquals("Leitura há 0 s · sensor simulado", a.sensores[0].linhaLeitura)
    }

    @Test
    fun `formatos de valor e idade`() {
        assertEquals("200.000 lux", formatarValor(200000.0, "LUX"))
        assertEquals("31,5 cm", formatarValor(31.5, "CM"))
        assertEquals("350 lux", formatarValor(350.0, "LUX"))
        assertEquals("59 s", formatarIdade(59))
        assertEquals("1 min", formatarIdade(60))
        assertEquals("3 min", formatarIdade(200))
        assertEquals("1 h", formatarIdade(3600))
    }
}
