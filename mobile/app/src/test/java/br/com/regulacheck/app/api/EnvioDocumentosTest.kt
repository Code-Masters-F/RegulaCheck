package br.com.regulacheck.app.api

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Envio de documento (`POST /solicitacoes/{id}/documentos`, RC-19 #42): multipart, chave de envio
 * reutilizada, repetição 200 e recusas, contra um servidor HTTP local.
 */
class EnvioDocumentosTest {

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

    private fun <T> falha(resultado: Resultado<T>): ErroApi {
        assertTrue("esperava falha, veio $resultado", resultado is Resultado.Falha)
        return (resultado as Resultado.Falha).erro
    }

    @Test
    fun `envio 201 manda multipart com arquivo, tipo documental e chave`() = runTest {
        servidor.enqueue(json(201, documentoEnviado()))

        val resultado = apiDeTeste(servidor).enviarDocumento("12", FonteDeTeste(), "ENCAMINHAMENTO", "chave-teste-0001")

        val documento = (resultado as Resultado.Sucesso).valor
        assertEquals("41", documento.id)
        assertEquals(1, documento.documentosDisponiveis)
        val requisicao = servidor.takeRequest()
        assertEquals("POST", requisicao.method)
        assertEquals("/solicitacoes/12/documentos", requisicao.path)
        assertEquals("chave-teste-0001", requisicao.getHeader("X-Chave-Envio"))
        assertEquals("Bearer token-de-teste", requisicao.getHeader("Authorization"))
        assertTrue(requisicao.getHeader("Content-Type")!!.startsWith("multipart/form-data; boundary="))
        val corpo = requisicao.body.readUtf8()
        assertTrue(corpo.contains("""name="arquivo"; filename="encaminhamento-sintetico.pdf""""))
        assertTrue(corpo.contains("Content-Type: application/pdf"))
        assertTrue(corpo.contains("%PDF-1.4 conteudo sintetico"))
        assertTrue(corpo.contains("name=\"tipoDocumental\""))
        assertTrue(corpo.contains("ENCAMINHAMENTO"))
    }

    @Test
    fun `repeticao com a mesma chave devolve 200 com o mesmo documento e reabre o arquivo`() = runTest {
        servidor.enqueue(json(201, documentoEnviado()))
        servidor.enqueue(json(200, documentoEnviado()))
        val fonte = FonteDeTeste()
        val api = apiDeTeste(servidor)

        val primeiro = api.enviarDocumento("12", fonte, "ENCAMINHAMENTO", "chave-teste-0001")
        val repeticao = api.enviarDocumento("12", fonte, "ENCAMINHAMENTO", "chave-teste-0001")

        assertEquals((primeiro as Resultado.Sucesso).valor.id, (repeticao as Resultado.Sucesso).valor.id)
        assertEquals("chave-teste-0001", servidor.takeRequest().getHeader("X-Chave-Envio"))
        assertEquals("chave-teste-0001", servidor.takeRequest().getHeader("X-Chave-Envio"))
        assertEquals(2, fonte.aberturas)
    }

    @Test
    fun `413 e 415 viram arquivo recusado com a mensagem da API`() = runTest {
        servidor.enqueue(erroApi(413, "ARQUIVO_MUITO_GRANDE", "O arquivo excede 5 MB."))
        servidor.enqueue(erroApi(415, "TIPO_NAO_SUPORTADO", "Envie um arquivo PDF, JPEG ou PNG."))
        val api = apiDeTeste(servidor)

        val grande = falha(api.enviarDocumento("12", FonteDeTeste(), "OUTRO", "chave-teste-0002"))
        val tipo = falha(api.enviarDocumento("12", FonteDeTeste(), "OUTRO", "chave-teste-0003"))

        assertEquals(ErroApi.ArquivoRecusado("ARQUIVO_MUITO_GRANDE", "O arquivo excede 5 MB."), grande)
        assertEquals(ErroApi.ArquivoRecusado("TIPO_NAO_SUPORTADO", "Envie um arquivo PDF, JPEG ou PNG."), tipo)
    }

    @Test
    fun `422 do limite de 3 e 409 de chave reutilizada mantem codigo e mensagem`() = runTest {
        servidor.enqueue(erroApi(422, "REGRA_DE_NEGOCIO", "O pedido já tem 3 documentos."))
        servidor.enqueue(erroApi(409, "CHAVE_ENVIO_REUTILIZADA", "Chave usada com outro arquivo."))
        val api = apiDeTeste(servidor)

        val limite = falha(api.enviarDocumento("12", FonteDeTeste(), "OUTRO", "chave-teste-0004"))
        val chave = falha(api.enviarDocumento("12", FonteDeTeste(), "OUTRO", "chave-teste-0004"))

        assertEquals("REGRA_DE_NEGOCIO", (limite as ErroApi.RequisicaoInvalida).codigo)
        assertEquals("CHAVE_ENVIO_REUTILIZADA", (chave as ErroApi.Conflito).codigo)
    }

    @Test
    fun `arquivo que nao abre vira ArquivoInacessivel sem derrubar o processo`() = runTest {
        // Sem resposta enfileirada: a falha acontece ao escrever o corpo, antes de qualquer resposta.
        val erro = falha(
            apiDeTeste(servidor).enviarDocumento("12", FonteDeTeste(falhar = true), "OUTRO", "chave-teste-0005"),
        )

        assertEquals(ErroApi.ArquivoInacessivel, erro)
    }

    @Test
    fun `503 guarda o codigo do corpo`() = runTest {
        servidor.enqueue(erroApi(503, "DEPENDENCIA_INDISPONIVEL"))

        val erro = falha(apiDeTeste(servidor).enviarDocumento("12", FonteDeTeste(), "OUTRO", "chave-teste-0006"))

        assertEquals(ErroApi.Indisponivel(503, "DEPENDENCIA_INDISPONIVEL"), erro)
    }
}
