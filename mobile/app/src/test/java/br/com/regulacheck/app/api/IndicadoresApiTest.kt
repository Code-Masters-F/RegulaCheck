package br.com.regulacheck.app.api

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Corpo sintético de `GET /admin/indicadores` (contrato da spec 024). */
fun indicadores() = """
    {"dadosSinteticos":true,"aviso":"Indicadores agregados de demonstracao.","consultadoEm":"2026-11-18T20:00:00Z",
     "protocolosAtivos":[{"protocoloId":"PROTOCOLO-ACADEMICO-FICTICIO-v0.1","identificador":"PROTOCOLO-ACADEMICO-FICTICIO",
       "versao":"v0.1","quantidadeRegras":2,"regras":[{"codigo":"RC-R01","descricao":"Encaminhamento presente"},
       {"codigo":"RC-R02","descricao":"Identificação coerente entre documentos"}]}],
     "totalPedidos":7,"analisesPorResultado":{"semPendenciasIdentificadas":0,"comPendencias":2,"requerConferencia":1},
     "pedidosEmFalhaAnalise":1,
     "regrasComMaisPendencias":[{"codigo":"RC-R02","descricao":"Identificação coerente entre documentos","total":2,
       "pendentes":1,"naoVerificaveis":1}],
     "campoFuturo":"ignorado"}
""".trimIndent()

/** `consultarIndicadores` (RC-20): rota, token, leitura do contrato e erros 401/403. */
class IndicadoresApiTest {

    private lateinit var servidor: MockWebServer

    @Before
    fun preparar() {
        servidor = MockWebServer().apply { start() }
    }

    @After
    fun encerrar() {
        servidor.shutdown()
    }

    @Test
    fun `le os agregados do contrato com o token da sessao`() = runTest {
        servidor.enqueue(json(200, indicadores()))

        val resultado = apiDeTeste(servidor).consultarIndicadores()

        val dados = (resultado as Resultado.Sucesso).valor
        assertEquals(7L, dados.totalPedidos)
        assertEquals(2L, dados.analisesPorResultado.comPendencias)
        assertEquals(1L, dados.analisesPorResultado.requerConferencia)
        assertEquals(1L, dados.pedidosEmFalhaAnalise)
        assertEquals("v0.1", dados.protocolosAtivos.single().versao)
        assertEquals(listOf("RC-R01", "RC-R02"), dados.protocolosAtivos.single().regras.map { it.codigo })
        assertEquals(2L, dados.regrasComMaisPendencias.single().total)
        val requisicao = servidor.takeRequest()
        assertEquals("GET", requisicao.method)
        assertEquals("/admin/indicadores", requisicao.path)
        assertEquals("Bearer token-de-teste", requisicao.getHeader("Authorization"))
    }

    @Test
    fun `403 de conta de paciente vira sem permissao sem encerrar a sessao`() = runTest {
        var expirou = false
        servidor.enqueue(erroApi(403, "ACESSO_NEGADO"))

        val resultado = apiDeTeste(servidor, aoSessaoExpirar = { expirou = true }).consultarIndicadores()

        assertEquals(Resultado.Falha(ErroApi.SemPermissao), resultado)
        assertEquals(false, expirou)
    }

    @Test
    fun `401 encerra a sessao local`() = runTest {
        var expirou = false
        servidor.enqueue(erroApi(401, "NAO_AUTENTICADO"))

        val resultado = apiDeTeste(servidor, aoSessaoExpirar = { expirou = true }).consultarIndicadores()

        assertEquals(Resultado.Falha(ErroApi.SessaoExpirada), resultado)
        assertTrue(expirou)
    }
}
