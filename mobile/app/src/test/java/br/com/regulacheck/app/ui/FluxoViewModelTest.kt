package br.com.regulacheck.app.ui

import br.com.regulacheck.app.api.ErroApi
import br.com.regulacheck.app.api.FonteDeTeste
import br.com.regulacheck.app.api.analiseGravada
import br.com.regulacheck.app.api.apiDeTeste
import br.com.regulacheck.app.api.documentoEnviado
import br.com.regulacheck.app.api.erroApi
import br.com.regulacheck.app.api.extracao
import br.com.regulacheck.app.api.historico
import br.com.regulacheck.app.api.json
import br.com.regulacheck.app.api.solicitacao
import br.com.regulacheck.app.ui.documentos.DocumentosViewModel
import br.com.regulacheck.app.ui.documentos.SituacaoEnvio
import br.com.regulacheck.app.ui.leitura.EstadoAnalise
import br.com.regulacheck.app.ui.leitura.EstadoLeitura
import br.com.regulacheck.app.ui.leitura.LeituraViewModel
import br.com.regulacheck.app.ui.navegacao.EstadoTela
import br.com.regulacheck.app.ui.resultado.ApresentacaoHistorico
import br.com.regulacheck.app.ui.resultado.EstadoResumo
import br.com.regulacheck.app.ui.resultado.ResultadoViewModel
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Comportamento das telas de documentos e leitura (RC-19 #42) contra um servidor HTTP local: o
 * documento só aparece depois da confirmação, "Tentar de novo" reutiliza a chave, nenhuma chamada
 * é repetida sozinha e o tempo esgotado na análise consulta o histórico antes de nova tentativa.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FluxoViewModelTest {

    private lateinit var servidor: MockWebServer

    @Before
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        servidor = MockWebServer()
        servidor.start()
    }

    @After
    fun encerrar() {
        servidor.shutdown()
        Dispatchers.resetMain()
    }

    /** Espera (em tempo real: a rede é real) até [condicao] valer para o estado. */
    private fun <T> esperar(fluxo: StateFlow<T>, condicao: (T) -> Boolean): T =
        runBlocking { withTimeout(5_000) { fluxo.first(condicao) } }

    private fun documentosCarregados(): DocumentosViewModel {
        servidor.enqueue(json(200, solicitacao()))
        servidor.enqueue(json(200, """{"documentos":[],"total":0,"maximo":3,"versaoConjuntoDocumental":1}"""))
        var chaves = 0
        val vm = DocumentosViewModel(apiDeTeste(servidor), "12", gerarChave = { "chave-teste-${++chaves}" })
        vm.carregar()
        esperar(vm.estado) { it is EstadoTela.Sucesso }
        servidor.takeRequest()
        servidor.takeRequest()
        return vm
    }

    private fun documentos(vm: DocumentosViewModel) = (vm.estado.value as EstadoTela.Sucesso).dados.documentos

    @Test
    fun `documento so entra na lista depois do 201`() {
        val vm = documentosCarregados()
        servidor.enqueue(json(201, documentoEnviado()).setBodyDelay(300, TimeUnit.MILLISECONDS))

        vm.arquivoEscolhido("ENCAMINHAMENTO", FonteDeTeste())

        assertEquals(SituacaoEnvio.Enviando, vm.envio.value?.situacao)
        assertTrue(documentos(vm).isEmpty())
        esperar(vm.envio) { it == null }
        assertEquals(listOf("41"), documentos(vm).map { it.id })
    }

    @Test
    fun `tentar de novo reutiliza arquivo e chave e a repeticao 200 nao duplica`() {
        val vm = documentosCarregados()
        servidor.enqueue(json(201, documentoEnviado()).setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))
        servidor.enqueue(json(200, documentoEnviado()))
        val fonte = FonteDeTeste()

        vm.arquivoEscolhido("ENCAMINHAMENTO", fonte)
        val interrompido = esperar(vm.envio) { it?.situacao is SituacaoEnvio.NaoEnviado }
        assertEquals(SituacaoEnvio.NaoEnviado(ErroApi.SemConexao), interrompido?.situacao)
        assertTrue(documentos(vm).isEmpty())

        vm.tentarDeNovo()
        esperar(vm.envio) { it == null }

        assertEquals(listOf("41"), documentos(vm).map { it.id })
        assertEquals("chave-teste-1", servidor.takeRequest().getHeader("X-Chave-Envio"))
        assertEquals("chave-teste-1", servidor.takeRequest().getHeader("X-Chave-Envio"))
    }

    @Test
    fun `recusa 415 mostra a mensagem da API e nao lista o arquivo`() {
        val vm = documentosCarregados()
        servidor.enqueue(erroApi(415, "TIPO_NAO_SUPORTADO", "Envie um arquivo PDF, JPEG ou PNG."))

        vm.arquivoEscolhido("OUTRO", FonteDeTeste(nome = "exame.heic", tipoMidia = "image/heic"))

        val recusa = esperar(vm.envio) { it?.situacao is SituacaoEnvio.Recusado }
        assertEquals(SituacaoEnvio.Recusado("Envie um arquivo PDF, JPEG ou PNG."), recusa?.situacao)
        assertTrue(documentos(vm).isEmpty())
        vm.tentarDeNovo() // recusa não se repete: só nova escolha
        assertEquals(1, servidor.requestCount - 2)
    }

    @Test
    fun `arquivo inacessivel pede nova escolha`() {
        val vm = documentosCarregados()

        vm.arquivoEscolhido("OUTRO", FonteDeTeste(falhar = true))

        assertEquals(SituacaoEnvio.EscolherDeNovo, esperar(vm.envio) { it?.situacao == SituacaoEnvio.EscolherDeNovo }?.situacao)
    }

    private fun leituraConcluida(): LeituraViewModel {
        servidor.enqueue(json(201, extracao()))
        val vm = LeituraViewModel(apiDeTeste(servidor), "12")
        esperar(vm.leitura) { it is EstadoLeitura.Lida }
        servidor.takeRequest()
        return vm
    }

    @Test
    fun `analise confirmada sem resumo pede o resumo uma vez e conclui`() {
        val vm = leituraConcluida()
        servidor.enqueue(json(201, analiseGravada()))
        servidor.enqueue(erroApi(503, "DEPENDENCIA_INDISPONIVEL"))

        vm.pedirConfirmacao()
        vm.analisar()
        vm.analisar() // toque repetido durante a análise: ignorado

        esperar(vm.analise) { it == EstadoAnalise.Concluida }
        assertEquals("/solicitacoes/12/analises", servidor.takeRequest().path)
        assertEquals("/solicitacoes/12/resumo", servidor.takeRequest().path)
        assertEquals(3, servidor.requestCount)
    }

    @Test
    fun `queda durante a analise consulta o historico e mostra o resultado gravado`() {
        val vm = leituraConcluida()
        servidor.enqueue(json(201, analiseGravada()).setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))
        servidor.enqueue(json(200, historico(analiseGravada(), vigenteId = "31")))

        vm.analisar()

        esperar(vm.analise) { it == EstadoAnalise.Concluida }
        assertEquals("POST", servidor.takeRequest().method)
        assertEquals("GET", servidor.takeRequest().method)
        assertEquals(3, servidor.requestCount)
    }

    @Test
    fun `queda sem analise gravada so libera nova tentativa depois da consulta`() {
        val vm = leituraConcluida()
        servidor.enqueue(json(201, analiseGravada()).setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))
        servidor.enqueue(json(200, historico(estado = "RASCUNHO")))

        vm.analisar()

        val estado = esperar(vm.analise) { it is EstadoAnalise.Falhou }
        assertEquals(EstadoAnalise.Falhou(ErroApi.SemConexao), estado)
        assertEquals(3, servidor.requestCount)
    }

    @Test
    fun `consulta do historico tambem falhou fica incerta e repete so a consulta`() {
        val vm = leituraConcluida()
        servidor.enqueue(json(201, analiseGravada()).setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))
        servidor.enqueue(erroApi(503, "DEPENDENCIA_INDISPONIVEL"))
        servidor.enqueue(json(200, historico(execucao = "EM_ANDAMENTO", estado = "PROCESSANDO")))

        vm.analisar()
        esperar(vm.analise) { it == EstadoAnalise.Incerta }
        vm.consultarDeNovo()

        assertEquals(EstadoAnalise.EmAndamento(null), esperar(vm.analise) { it is EstadoAnalise.EmAndamento })
        assertEquals("POST", servidor.takeRequest().method)
        assertEquals("GET", servidor.takeRequest().method)
        assertEquals("GET", servidor.takeRequest().method)
    }

    @Test
    fun `analise em andamento 409 mostra a espera sem repetir`() {
        val vm = leituraConcluida()
        servidor.enqueue(erroApi(409, "ANALISE_EM_ANDAMENTO").setHeader("Retry-After", "37"))

        vm.analisar()

        assertEquals(EstadoAnalise.EmAndamento(37), esperar(vm.analise) { it is EstadoAnalise.EmAndamento })
        assertEquals(2, servidor.requestCount)
    }

    private fun resultadoCarregado(analise: String): ResultadoViewModel {
        servidor.enqueue(json(200, historico(analise, vigenteId = "31")))
        val vm = ResultadoViewModel(apiDeTeste(servidor), "12", analiseId = null)
        vm.carregar()
        esperar(vm.estado) { it is EstadoTela.Sucesso }
        servidor.takeRequest()
        return vm
    }

    private fun analiseExibida(vm: ResultadoViewModel) =
        ((vm.estado.value as EstadoTela.Sucesso).dados as ApresentacaoHistorico.Analise).analise

    @Test
    fun `tentar de novo o resumo troca so o resumo da mesma analise`() {
        val vm = resultadoCarregado(analiseGravada())
        servidor.enqueue(json(201, analiseGravada(resumo = """{"texto":"Resumo <b> & cia.","origem":"ADAPTADOR_MOCKADO","simulado":true,"itens":[]}""")))

        vm.pedirResumo()
        vm.pedirResumo() // toque repetido: ignorado
        esperar(vm.resumo) { it == EstadoResumo.Ocioso }

        assertEquals("Resumo <b> & cia.", analiseExibida(vm).resumo?.texto)
        assertEquals(2, servidor.requestCount)
    }

    @Test
    fun `409 no resumo reconsulta o historico antes de reapresentar`() {
        val vm = resultadoCarregado(analiseGravada())
        servidor.enqueue(erroApi(409, "CONFLITO_ESTADO", "Sem análise vigente."))
        servidor.enqueue(json(200, historico(analiseGravada("40"), vigenteId = "40")))

        vm.pedirResumo()

        esperar(vm.estado) { (it as? EstadoTela.Sucesso)?.dados.let { d -> d is ApresentacaoHistorico.Analise && d.analise.id == "40" } }
        assertEquals("POST", servidor.takeRequest().method)
        assertEquals("GET", servidor.takeRequest().method)
    }

    @Test
    fun `falha do resumo mantem resultado e pendencias`() {
        val vm = resultadoCarregado(analiseGravada())
        servidor.enqueue(erroApi(503, "DEPENDENCIA_INDISPONIVEL"))

        vm.pedirResumo()

        assertTrue(esperar(vm.resumo) { it is EstadoResumo.Falhou } is EstadoResumo.Falhou)
        assertEquals(1, analiseExibida(vm).pendencias.size)
    }

    @Test
    fun `leitura sem documentos 422 nao vira leitura concluida`() {
        servidor.enqueue(erroApi(422, "SEM_DOCUMENTOS"))
        val vm = LeituraViewModel(apiDeTeste(servidor), "12")

        val estado = esperar(vm.leitura) { it is EstadoLeitura.Falhou } as EstadoLeitura.Falhou

        assertEquals("SEM_DOCUMENTOS", (estado.erro as ErroApi.RequisicaoInvalida).codigo)
        vm.pedirConfirmacao()
        assertEquals(EstadoAnalise.Ociosa, vm.analise.value)
        assertNull((vm.leitura.value as? EstadoLeitura.Lida))
    }
}
