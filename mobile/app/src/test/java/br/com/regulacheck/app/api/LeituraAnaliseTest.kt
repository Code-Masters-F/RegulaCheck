package br.com.regulacheck.app.api

import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Leitura (`POST …/extracoes`), análise (`POST`/`GET …/analises`) e resumo (`POST …/resumo`) da
 * RC-19 (#42): sucesso, 422 `SEM_DOCUMENTOS`, 409 sem leitura, 409 `ANALISE_EM_ANDAMENTO` com
 * `Retry-After`, 503 e o tempo de leitura estendido.
 */
class LeituraAnaliseTest {

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
    fun `leitura 201 e repeticao 200 devolvem a leitura simulada`() = runTest {
        servidor.enqueue(json(201, extracao()))
        servidor.enqueue(json(200, extracao()))
        val api = apiDeTeste(servidor)

        val nova = (api.lerDocumentos("12") as Resultado.Sucesso).valor
        val repetida = (api.lerDocumentos("12") as Resultado.Sucesso).valor

        assertEquals("LIDO", nova.documentos.single().situacao)
        assertEquals(nova.id, repetida.id)
        val requisicao = servidor.takeRequest()
        assertEquals("POST", requisicao.method)
        assertEquals("/solicitacoes/12/extracoes", requisicao.path)
    }

    @Test
    fun `leitura sem documentos 422 SEM_DOCUMENTOS`() = runTest {
        servidor.enqueue(erroApi(422, "SEM_DOCUMENTOS", "Envie ao menos um documento."))

        val erro = falha(apiDeTeste(servidor).lerDocumentos("12"))

        assertEquals("SEM_DOCUMENTOS", (erro as ErroApi.RequisicaoInvalida).codigo)
    }

    @Test
    fun `leitura 503 LEITURA_INDISPONIVEL`() = runTest {
        servidor.enqueue(erroApi(503, "LEITURA_INDISPONIVEL"))

        assertEquals(ErroApi.Indisponivel(503, "LEITURA_INDISPONIVEL"), falha(apiDeTeste(servidor).lerDocumentos("12")))
    }

    @Test
    fun `analise 201 traz rotulo, orientacao e evidencia com origem literal`() = runTest {
        servidor.enqueue(json(201, analiseGravada()))

        val analise = (apiDeTeste(servidor).analisar("12") as Resultado.Sucesso).valor

        assertEquals("Com pendências", analise.rotuloResultado)
        val pendencia = analise.pendencias.single()
        assertEquals("UNIDADE_EMISSORA", pendencia.responsavel)
        assertEquals("Solicite à unidade emissora a revisão do encaminhamento.", pendencia.orientacao)
        assertEquals("Motivo: <a> & b", pendencia.evidencias.single().origem?.trecho)
        assertNull(analise.resumo)
        assertEquals("/solicitacoes/12/analises", servidor.takeRequest().path)
    }

    @Test
    fun `analise sem leitura atual 409 CONFLITO_ESTADO com a mensagem`() = runTest {
        servidor.enqueue(erroApi(409, "CONFLITO_ESTADO", "Leia os documentos antes de pedir a análise."))

        val erro = falha(apiDeTeste(servidor).analisar("12")) as ErroApi.Conflito

        assertEquals("CONFLITO_ESTADO", erro.codigo)
        assertEquals("Leia os documentos antes de pedir a análise.", erro.mensagem)
        assertNull(erro.retryAfterSegundos)
    }

    @Test
    fun `analise em andamento 409 traz Retry-After em segundos`() = runTest {
        servidor.enqueue(erroApi(409, "ANALISE_EM_ANDAMENTO").setHeader("Retry-After", "42"))

        val erro = falha(apiDeTeste(servidor).analisar("12"))

        assertEquals(ErroApi.Conflito("ANALISE_EM_ANDAMENTO", "Mensagem.", 42), erro)
        assertEquals(1, servidor.requestCount)
    }

    @Test
    fun `analise 503 ANALISE_INDISPONIVEL nao e repetida`() = runTest {
        servidor.enqueue(erroApi(503, "ANALISE_INDISPONIVEL"))

        assertEquals(ErroApi.Indisponivel(503, "ANALISE_INDISPONIVEL"), falha(apiDeTeste(servidor).analisar("12")))
        assertEquals(1, servidor.requestCount)
    }

    @Test
    fun `ler e analisar usam o tempo estendido e a consulta usa o normal`() = runTest {
        // Cliente com leitura de 300 ms; a resposta demora 1 s. Ler e analisar ganham 70 s.
        val http = OkHttpClient.Builder().readTimeout(300, TimeUnit.MILLISECONDS).retryOnConnectionFailure(false).build()
        servidor.enqueue(json(201, extracao()).setBodyDelay(1, TimeUnit.SECONDS))
        servidor.enqueue(json(201, analiseGravada()).setBodyDelay(1, TimeUnit.SECONDS))
        servidor.enqueue(json(200, historico()).setBodyDelay(1, TimeUnit.SECONDS))
        val api = apiDeTeste(servidor, http)

        assertTrue(api.lerDocumentos("12") is Resultado.Sucesso)
        assertTrue(api.analisar("12") is Resultado.Sucesso)
        assertEquals(ErroApi.TempoEsgotado, falha(api.consultarAnalises("12")))
    }

    @Test
    fun `historico traz vigente, execucao e analises`() = runTest {
        servidor.enqueue(json(200, historico(analiseGravada(), vigenteId = "31", execucao = "EM_ANDAMENTO", estado = "PROCESSANDO")))

        val historico = (apiDeTeste(servidor).consultarAnalises("12") as Resultado.Sucesso).valor

        assertEquals("31", historico.vigenteId)
        assertEquals("EM_ANDAMENTO", historico.execucao?.situacao)
        assertEquals(1, historico.analises.size)
    }

    @Test
    fun `resumo PADRAO e 409 do resumo`() = runTest {
        servidor.enqueue(json(201, analiseGravada(resumo = """{"texto":"Resumo padrão.","origem":"PADRAO","simulado":true,"itens":[]}""")))
        servidor.enqueue(erroApi(409, "CONFLITO_ESTADO", "Sem análise vigente."))
        val api = apiDeTeste(servidor)

        val comResumo = (api.gerarResumo("12") as Resultado.Sucesso).valor

        assertEquals("PADRAO", comResumo.resumo?.origem)
        assertTrue(falha(api.gerarResumo("12")) is ErroApi.Conflito)
    }

    @Test
    fun `backend sem os campos da RC-15 nao derruba a tela`() = runTest {
        val semRc15 = analiseGravada().replace(""""rotuloResultado":"Com pendências",""", "")
            .replace(""""orientacao":"Solicite à unidade emissora a revisão do encaminhamento.",""", "")
        servidor.enqueue(json(200, semRc15))

        val analise = (apiDeTeste(servidor).analisar("12") as Resultado.Sucesso).valor

        assertNull(analise.rotuloResultado)
        assertNull(analise.pendencias.single().orientacao)
    }
}
