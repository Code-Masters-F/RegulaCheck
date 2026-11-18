package br.com.regulacheck.app.ui

import br.com.regulacheck.app.api.ErroApi
import br.com.regulacheck.app.api.apiDeTeste
import br.com.regulacheck.app.api.erroApi
import br.com.regulacheck.app.api.indicadores
import br.com.regulacheck.app.api.json
import br.com.regulacheck.app.ui.indicadores.IndicadoresViewModel
import br.com.regulacheck.app.ui.navegacao.EstadoTela
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** E4.2 (RC-20): carregando → sucesso; falha com "Tentar de novo" sem repetição automática. */
@OptIn(ExperimentalCoroutinesApi::class)
class IndicadoresViewModelTest {

    private lateinit var servidor: MockWebServer

    @Before
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        servidor = MockWebServer().apply { start() }
    }

    @After
    fun encerrar() {
        servidor.shutdown()
        Dispatchers.resetMain()
    }

    private fun esperar(vm: IndicadoresViewModel, condicao: (EstadoTela<*>) -> Boolean) =
        runBlocking { withTimeout(5_000) { vm.estado.first(condicao) } }

    @Test
    fun `comeca carregando e mostra os agregados da API`() {
        servidor.enqueue(json(200, indicadores()))
        val vm = IndicadoresViewModel(apiDeTeste(servidor))
        assertEquals(EstadoTela.Carregando, vm.estado.value)

        vm.carregar()
        val estado = esperar(vm) { it is EstadoTela.Sucesso }

        assertEquals(7L, (estado as EstadoTela.Sucesso).dados.totalPedidos)
        assertEquals(1, servidor.requestCount)
    }

    @Test
    fun `falha nao repete sozinha e tentar de novo faz uma nova consulta`() {
        servidor.enqueue(erroApi(503, "DEPENDENCIA_INDISPONIVEL"))
        servidor.enqueue(json(200, indicadores()))
        val vm = IndicadoresViewModel(apiDeTeste(servidor))

        vm.carregar()
        val falha = esperar(vm) { it is EstadoTela.Falha }
        assertTrue((falha as EstadoTela.Falha).erro is ErroApi.Indisponivel)
        servidor.takeRequest()
        assertNull("nenhuma repeticao automatica", servidor.takeRequest(300, TimeUnit.MILLISECONDS))

        vm.carregar()
        esperar(vm) { it is EstadoTela.Sucesso }
        assertEquals(2, servidor.requestCount)
    }

    @Test
    fun `403 fica como sem permissao`() {
        servidor.enqueue(erroApi(403, "ACESSO_NEGADO"))
        val vm = IndicadoresViewModel(apiDeTeste(servidor))

        vm.carregar()

        assertEquals(EstadoTela.Falha(ErroApi.SemPermissao), esperar(vm) { it is EstadoTela.Falha })
    }
}
