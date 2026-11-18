package br.com.regulacheck.app.ui

import br.com.regulacheck.app.api.DISTANCIA_DENTRO
import br.com.regulacheck.app.api.ErroApi
import br.com.regulacheck.app.api.LUMINOSIDADE_BAIXA
import br.com.regulacheck.app.api.LUMINOSIDADE_DENTRO
import br.com.regulacheck.app.api.apiDeTeste
import br.com.regulacheck.app.api.erroApi
import br.com.regulacheck.app.api.json
import br.com.regulacheck.app.api.sessaoCaptura
import br.com.regulacheck.app.api.telemetria
import br.com.regulacheck.app.ui.captura.CondicoesCapturaViewModel
import br.com.regulacheck.app.ui.captura.EstadoCaptura
import br.com.regulacheck.app.ui.captura.SituacaoSensorUi
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Laço da E4.1 (RC-22, spec 023, research D3/D6) contra um servidor HTTP local: sessão pedida uma
 * vez, consultas sequenciais que param ao cancelar, "indisponível" sem travar e erros que encerram.
 * A rede é real; os tempos são reais e curtos.
 */
class CondicoesCapturaViewModelTest {

    private lateinit var servidor: MockWebServer
    private val escopo = CoroutineScope(Dispatchers.Default)

    @Before
    fun preparar() {
        servidor = MockWebServer()
        servidor.start()
    }

    @After
    fun encerrar() {
        servidor.shutdown()
    }

    /**
     * Servidor que responde `POST …/sessoes-captura` com [sessao] e cada `GET …/leituras` com o
     * próximo item de [leituras] (repete o último), demorando [demoraMs] e medindo a sobreposição.
     */
    private inner class Estacao(
        private val sessao: MockResponse = json(201, sessaoCaptura()),
        private val leituras: List<() -> MockResponse> = listOf({ json(200, telemetria(LUMINOSIDADE_DENTRO, DISTANCIA_DENTRO)) }),
        private val demoraMs: Long = 0,
    ) : Dispatcher() {
        val sessoes = AtomicInteger()
        val consultas = AtomicInteger()
        private val emAndamento = AtomicInteger()
        val maximoSimultaneo = AtomicInteger()

        override fun dispatch(request: RecordedRequest): MockResponse {
            if (request.method == "POST" && request.path == "/solicitacoes/12/sessoes-captura") {
                sessoes.incrementAndGet()
                return sessao
            }
            if (request.method != "GET" || request.path != "/solicitacoes/12/leituras") return MockResponse().setResponseCode(500)
            val indice = consultas.getAndIncrement()
            val agora = emAndamento.incrementAndGet()
            maximoSimultaneo.accumulateAndGet(agora) { a, b -> maxOf(a, b) }
            try {
                if (demoraMs > 0) Thread.sleep(demoraMs)
                return leituras[minOf(indice, leituras.lastIndex)]()
            } finally {
                emAndamento.decrementAndGet()
            }
        }
    }

    private fun viewModel(estacao: Estacao, intervaloMs: Long = 20, aoSessaoExpirar: () -> Unit = {}) =
        CondicoesCapturaViewModel(apiDeTeste(servidor, aoSessaoExpirar = aoSessaoExpirar), "12", intervaloMs).also {
            servidor.dispatcher = estacao
        }

    private fun acompanhar(vm: CondicoesCapturaViewModel): Job = escopo.launch { vm.acompanhar() }

    private fun esperar(vm: CondicoesCapturaViewModel, condicao: (EstadoCaptura) -> Boolean): EstadoCaptura =
        runBlocking { withTimeout(5_000) { vm.estado.first(condicao) } }

    private fun parar(job: Job) = runBlocking { job.cancelAndJoin() }

    private fun esperarConsultas(estacao: Estacao, minimo: Int) =
        runBlocking { withTimeout(5_000) { while (estacao.consultas.get() < minimo) delay(20) } }

    @Test
    fun `sessao 201 e depois a consulta mostra os sensores`() {
        val estacao = Estacao()
        val vm = viewModel(estacao, intervaloMs = 60_000)
        val job = acompanhar(vm)

        val estado = esperar(vm) { it is EstadoCaptura.Exibindo } as EstadoCaptura.Exibindo
        parar(job)

        assertEquals(SituacaoSensorUi.ADEQUADA, estado.apresentacao.sensores[0].situacao)
        assertEquals(1, estacao.sessoes.get())
        assertEquals(1, estacao.consultas.get())
    }

    @Test
    fun `sessao 200 ja aberta segue igual`() {
        val estacao = Estacao(sessao = json(200, sessaoCaptura(criada = false)))
        val vm = viewModel(estacao, intervaloMs = 60_000)
        val job = acompanhar(vm)

        esperar(vm) { it is EstadoCaptura.Exibindo }
        parar(job)

        assertEquals(1, estacao.consultas.get())
    }

    @Test
    fun `leitura fora e depois dentro da faixa gera recuperada sem toque`() {
        val estacao = Estacao(
            leituras = listOf(
                { json(200, telemetria(LUMINOSIDADE_BAIXA, DISTANCIA_DENTRO)) },
                { json(200, telemetria(LUMINOSIDADE_DENTRO, DISTANCIA_DENTRO)) },
            ),
        )
        val vm = viewModel(estacao)
        val job = acompanhar(vm)

        esperar(vm) { (it as? EstadoCaptura.Exibindo)?.apresentacao?.sensores?.get(0)?.situacao == SituacaoSensorUi.BAIXA }
        val recuperada = esperar(vm) {
            (it as? EstadoCaptura.Exibindo)?.apresentacao?.sensores?.get(0)?.situacao == SituacaoSensorUi.ADEQUADA_DE_NOVO
        } as EstadoCaptura.Exibindo
        parar(job)

        assertEquals(Textos.CONDICOES_ADEQUADAS_DE_NOVO, recuperada.apresentacao.sensores[0].orientacao)
        assertEquals("a sessão é pedida uma vez por visita", 1, estacao.sessoes.get())
    }

    @Test
    fun `consultas nunca se sobrepoem e param ao cancelar`() {
        val estacao = Estacao(demoraMs = 200)
        val vm = viewModel(estacao, intervaloMs = 10)
        val job = acompanhar(vm)

        // Espera pelo número de consultas, não por um tempo fixo: com a máquina lenta, o laço
        // correto só demora mais para chegar lá.
        esperarConsultas(estacao, 3)
        parar(job)
        val aoParar = estacao.consultas.get()
        runBlocking { delay(500) }

        assertEquals(1, estacao.maximoSimultaneo.get())
        assertEquals("nenhuma consulta depois de sair da tela", aoParar, estacao.consultas.get())
    }

    @Test
    fun `segundo laco ao mesmo tempo nao faz nada`() {
        val estacao = Estacao(demoraMs = 150)
        val vm = viewModel(estacao, intervaloMs = 10)
        val primeiro = acompanhar(vm)
        val segundo = acompanhar(vm)

        runBlocking { delay(700) }
        parar(primeiro)
        parar(segundo)

        assertEquals(1, estacao.maximoSimultaneo.get())
        assertEquals(1, estacao.sessoes.get())
    }

    @Test
    fun `sem conexao fica indisponivel com seguir disponivel e continua consultando`() {
        val queda = { MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST) }
        val estacao = Estacao(sessao = queda(), leituras = listOf(queda))
        val vm = viewModel(estacao)
        val job = acompanhar(vm)

        val estado = esperar(vm) { it is EstadoCaptura.Exibindo } as EstadoCaptura.Exibindo
        esperarConsultas(estacao, 3)
        parar(job)

        assertTrue(estado.apresentacao.indisponivel)
        assertTrue("a ação principal passa a 'mesmo assim'", estado.apresentacao.mesmoAssim)
        assertTrue("não encerra a tela", vm.estado.value is EstadoCaptura.Exibindo)
        assertEquals("o pedido de sessão não é repetido na visita", 1, estacao.sessoes.get())
    }

    @Test
    fun `servico 503 na consulta fica indisponivel sem encerrar e continua consultando`() {
        val estacao = Estacao(leituras = listOf({ erroApi(503, "DEPENDENCIA_INDISPONIVEL") }))
        val vm = viewModel(estacao)
        val job = acompanhar(vm)

        val estado = esperar(vm) { it is EstadoCaptura.Exibindo } as EstadoCaptura.Exibindo
        esperarConsultas(estacao, 2)
        parar(job)

        assertTrue(estado.apresentacao.indisponivel)
        assertTrue("não encerra a tela", vm.estado.value is EstadoCaptura.Exibindo)
        assertTrue("segue indisponível", (vm.estado.value as EstadoCaptura.Exibindo).apresentacao.indisponivel)
    }

    @Test
    fun `pedido submetido 409 ao abrir encerra sem consultar`() {
        val estacao = Estacao(sessao = erroApi(409, "CONFLITO_ESTADO", "Pedido submetido em simulação; só consulta."))
        val vm = viewModel(estacao)

        runBlocking { withTimeout(5_000) { vm.acompanhar() } }

        assertTrue((vm.estado.value as EstadoCaptura.Encerrada).erro is ErroApi.Conflito)
        assertEquals(0, estacao.consultas.get())
    }

    @Test
    fun `sem permissao 403 ao abrir encerra`() {
        val estacao = Estacao(sessao = erroApi(403, "ACESSO_NEGADO"))
        val vm = viewModel(estacao)

        runBlocking { withTimeout(5_000) { vm.acompanhar() } }

        assertEquals(EstadoCaptura.Encerrada(ErroApi.SemPermissao), vm.estado.value)
        assertEquals(0, estacao.consultas.get())
    }

    @Test
    fun `404 na consulta encerra e nao volta a consultar ate nova visita`() {
        val estacao = Estacao(leituras = listOf({ erroApi(404, "NAO_ENCONTRADO") }))
        val vm = viewModel(estacao)

        runBlocking { withTimeout(5_000) { vm.acompanhar() } }
        assertEquals(EstadoCaptura.Encerrada(ErroApi.NaoEncontrado), vm.estado.value)
        runBlocking { withTimeout(5_000) { vm.acompanhar() } }
        assertEquals("a volta do segundo plano não consulta de novo", 1, estacao.consultas.get())

        vm.novaVisita()
        assertEquals(EstadoCaptura.Carregando, vm.estado.value)
        runBlocking { withTimeout(5_000) { vm.acompanhar() } }
        assertEquals(2, estacao.sessoes.get())
    }

    @Test
    fun `401 na consulta encerra e apaga a sessao local`() {
        var expirou = false
        val estacao = Estacao(leituras = listOf({ erroApi(401, "NAO_AUTENTICADO") }))
        val vm = viewModel(estacao, aoSessaoExpirar = { expirou = true })

        runBlocking { withTimeout(5_000) { vm.acompanhar() } }

        assertEquals(EstadoCaptura.Encerrada(ErroApi.SessaoExpirada), vm.estado.value)
        assertTrue(expirou)
    }
}
