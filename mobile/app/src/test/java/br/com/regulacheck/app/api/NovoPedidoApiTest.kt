package br.com.regulacheck.app.api

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** E2.1: `GET /protocolos` e `POST /solicitacoes` (RC-19). */
class NovoPedidoApiTest {

    private lateinit var servidor: MockWebServer

    @Before
    fun preparar() {
        servidor = MockWebServer()
        servidor.start()
    }

    @After
    fun encerrar() {
        servidor.shutdown()
    }

    @Test
    fun `lista de servicos traz o primeiro e aceita lista vazia`() = runTest {
        servidor.enqueue(
            json(
                200,
                """{"itens":[{"protocoloId":"PROTOCOLO-ACADEMICO-FICTICIO-v0.1","identificador":"PROTOCOLO-ACADEMICO-FICTICIO",
                    "versao":"v0.1","localidade":"Santa Aurora (município fictício)",
                    "especialidade":"Consulta ambulatorial em cardiologia","avisoFicticio":"Fictício.","simulacao":true}]}""",
            ),
        )
        servidor.enqueue(json(200, """{"itens":[]}"""))
        val api = apiDeTeste(servidor)

        val lista = (api.listarProtocolos() as Resultado.Sucesso).valor
        val vazia = (api.listarProtocolos() as Resultado.Sucesso).valor

        assertEquals("PROTOCOLO-ACADEMICO-FICTICIO-v0.1", lista.itens.first().protocoloId)
        assertEquals("v0.1", lista.itens.first().versao)
        assertTrue(vazia.itens.isEmpty())
    }

    @Test
    fun `criar rascunho envia so o protocoloId e devolve o pedido`() = runTest {
        servidor.enqueue(json(201, solicitacao(id = "15")))

        val pedido = (apiDeTeste(servidor).criarSolicitacao("PROTOCOLO-ACADEMICO-FICTICIO-v0.1") as Resultado.Sucesso).valor

        assertEquals("15", pedido.id)
        val requisicao = servidor.takeRequest()
        assertEquals("/solicitacoes", requisicao.path)
        assertEquals("""{"protocoloId":"PROTOCOLO-ACADEMICO-FICTICIO-v0.1"}""", requisicao.body.readUtf8())
    }

    @Test
    fun `servico inativo 422 vira requisicao invalida`() = runTest {
        servidor.enqueue(erroApi(422, "REGRA_DE_NEGOCIO", "Serviço indisponível."))

        val resultado = apiDeTeste(servidor).criarSolicitacao("X")

        assertTrue((resultado as Resultado.Falha).erro is ErroApi.RequisicaoInvalida)
    }
}
