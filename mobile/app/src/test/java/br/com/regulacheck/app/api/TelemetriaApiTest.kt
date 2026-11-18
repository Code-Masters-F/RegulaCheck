package br.com.regulacheck.app.api

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Rotas da estação de captura SIMULADA consumidas pelo app (RC-22, spec 023,
 * contracts/api-consumida.md): pedido de sessão sem corpo nem identificador da paciente, telemetria
 * em cada situação e mapeamento dos erros.
 */
class TelemetriaApiTest {

    private lateinit var servidor: MockWebServer
    private lateinit var api: RegulaCheckApi

    @Before
    fun preparar() {
        servidor = MockWebServer()
        servidor.start()
        api = apiDeTeste(servidor)
    }

    @After
    fun encerrar() {
        servidor.shutdown()
    }

    @Test
    fun `abrir sessao 201 envia POST sem corpo com o token da sessao`() {
        servidor.enqueue(json(201, sessaoCaptura(criada = true)))

        val resultado = runBlocking { api.abrirSessaoCaptura("12") }

        assertEquals("42", (resultado as Resultado.Sucesso).valor.id)
        assertEquals(true, resultado.valor.criada)
        val requisicao = servidor.takeRequest()
        assertEquals("POST", requisicao.method)
        assertEquals("/solicitacoes/12/sessoes-captura", requisicao.path)
        assertEquals("Bearer token-de-teste", requisicao.getHeader("Authorization"))
        assertEquals(0L, requisicao.bodySize)
        assertNull(requisicao.getHeader("X-Chave-Simulador"))
    }

    @Test
    fun `abrir sessao 200 devolve a sessao ja aberta`() {
        servidor.enqueue(json(200, sessaoCaptura(criada = false)))

        val resultado = runBlocking { api.abrirSessaoCaptura("12") }

        assertEquals("42", (resultado as Resultado.Sucesso).valor.id)
        assertEquals(false, resultado.valor.criada)
    }

    @Test
    fun `abrir sessao mapeia 409 403 404 e 503`() {
        servidor.enqueue(erroApi(409, "CONFLITO_ESTADO", "Pedido submetido em simulação; só consulta."))
        servidor.enqueue(erroApi(403, "ACESSO_NEGADO"))
        servidor.enqueue(erroApi(404, "NAO_ENCONTRADO"))
        servidor.enqueue(erroApi(503, "DEPENDENCIA_INDISPONIVEL"))

        val erros = runBlocking { List(4) { (api.abrirSessaoCaptura("12") as Resultado.Falha).erro } }

        assertEquals("CONFLITO_ESTADO", (erros[0] as ErroApi.Conflito).codigo)
        assertEquals(ErroApi.SemPermissao, erros[1])
        assertEquals(ErroApi.NaoEncontrado, erros[2])
        assertEquals(503, (erros[3] as ErroApi.Indisponivel).status)
    }

    @Test
    fun `telemetria traz cada situacao dos dois sensores`() {
        val situacoes = listOf("DENTRO_DA_FAIXA", "ABAIXO_DA_FAIXA", "ACIMA_DA_FAIXA", "DESATUALIZADA")
        situacoes.forEach { s ->
            servidor.enqueue(json(200, telemetria(sensor("LUMINOSIDADE", s, 500.0), sensor("DISTANCIA", s, 30.0))))
        }
        servidor.enqueue(json(200, telemetria(LUMINOSIDADE_SEM_LEITURA, DISTANCIA_SEM_LEITURA)))

        val lidas = runBlocking { List(5) { (api.consultarTelemetria("12") as Resultado.Sucesso).valor } }

        assertEquals(situacoes + "SEM_LEITURA", lidas.map { it.sensores[0].situacao })
        assertEquals(listOf("LUMINOSIDADE", "DISTANCIA"), lidas[0].sensores.map { it.tipo })
        assertEquals(500.0, lidas[0].sensores[0].ultimaLeitura!!.valor, 0.0)
        assertEquals("LUX", lidas[0].sensores[0].unidade)
        val requisicao = servidor.takeRequest()
        assertEquals("GET", requisicao.method)
        assertEquals("/solicitacoes/12/leituras", requisicao.path)
    }

    @Test
    fun `telemetria sem sessao responde SEM_LEITURA sem sessao nem leitura`() {
        servidor.enqueue(json(200, telemetria(LUMINOSIDADE_SEM_LEITURA, DISTANCIA_SEM_LEITURA, comSessao = false)))

        val lida = (runBlocking { api.consultarTelemetria("12") } as Resultado.Sucesso).valor

        assertNull(lida.sessao)
        assertTrue(lida.sensores.all { it.situacao == "SEM_LEITURA" && it.ultimaLeitura == null })
        assertEquals("CAPTURA_SEGUE_SEM_SENSOR", lida.sensores[0].orientacao!!.codigo)
    }

    @Test
    fun `telemetria mapeia 403 404 e 503`() {
        servidor.enqueue(erroApi(403, "ACESSO_NEGADO"))
        servidor.enqueue(erroApi(404, "NAO_ENCONTRADO"))
        servidor.enqueue(erroApi(503, "DEPENDENCIA_INDISPONIVEL"))

        val erros = runBlocking { List(3) { (api.consultarTelemetria("12") as Resultado.Falha).erro } }

        assertEquals(listOf(ErroApi.SemPermissao, ErroApi.NaoEncontrado, ErroApi.Indisponivel(503, "DEPENDENCIA_INDISPONIVEL")), erros)
    }
}
