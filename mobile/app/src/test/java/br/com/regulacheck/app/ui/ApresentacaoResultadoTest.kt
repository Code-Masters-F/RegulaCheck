package br.com.regulacheck.app.ui

import br.com.regulacheck.app.api.HistoricoAnalises
import br.com.regulacheck.app.api.JsonApi
import br.com.regulacheck.app.api.PendenciaGravada
import br.com.regulacheck.app.api.analiseGravada
import br.com.regulacheck.app.api.historico
import br.com.regulacheck.app.ui.leitura.DecisaoAposFalha
import br.com.regulacheck.app.ui.leitura.decidirAposFalhaDaAnalise
import br.com.regulacheck.app.ui.resultado.ApresentacaoHistorico
import br.com.regulacheck.app.ui.resultado.apresentarHistorico
import br.com.regulacheck.app.ui.resultado.detalheDe
import br.com.regulacheck.app.ui.resultado.linhasEvidencia
import br.com.regulacheck.app.ui.resultado.orientacoesDoApp
import br.com.regulacheck.app.ui.resultado.rotuloDoResultado
import br.com.regulacheck.app.ui.resultado.situacaoDaPendencia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regras de apresentação do resultado (contrato da spec 019) e decisão depois de falha da análise
 * (research D6 da spec 022), com entradas montadas a partir do contrato da API.
 */
class ApresentacaoResultadoTest {

    private fun historicoDe(json: String): HistoricoAnalises = JsonApi.decodeFromString(HistoricoAnalises.serializer(), json)

    @Test
    fun `resultado atual e so a analise apontada por vigenteId e marcada vigente`() {
        val dados = historicoDe(historico("${analiseGravada("32", vigente = true)},${analiseGravada("31", vigente = false)}", vigenteId = "32"))

        val apresentacao = apresentarHistorico(dados) as ApresentacaoHistorico.Analise

        assertEquals("32", apresentacao.analise.id)
        assertTrue(apresentacao.vigente)
    }

    @Test
    fun `sem vigenteId nenhuma analise vira atual e a antiga so aparece como anterior`() {
        val dados = historicoDe(historico(analiseGravada("31", vigente = false)))

        val apresentacao = apresentarHistorico(dados)

        assertEquals("31", (apresentacao as ApresentacaoHistorico.SemVigente).anterior.id)
        val anterior = apresentarHistorico(dados, analiseId = "31") as ApresentacaoHistorico.Analise
        assertFalse(anterior.vigente)
    }

    @Test
    fun `vigenteId sem a marca vigente nao e tratado como atual`() {
        val dados = historicoDe(historico(analiseGravada("31", vigente = false), vigenteId = "31"))

        assertTrue(apresentarHistorico(dados) is ApresentacaoHistorico.SemVigente)
    }

    @Test
    fun `em andamento, falha e sem analise nao apresentam conclusao`() {
        assertEquals(
            ApresentacaoHistorico.EmAndamento,
            apresentarHistorico(historicoDe(historico(execucao = "EM_ANDAMENTO", estado = "PROCESSANDO"))),
        )
        assertEquals(
            ApresentacaoHistorico.Falha(3),
            apresentarHistorico(historicoDe(historico(estado = "FALHA_ANALISE"))),
        )
        assertEquals(ApresentacaoHistorico.SemAnalise, apresentarHistorico(historicoDe(historico(estado = "RASCUNHO"))))
    }

    @Test
    fun `decisao apos tempo esgotado consulta o historico antes de nova tentativa`() {
        assertEquals(
            DecisaoAposFalha.MOSTRAR_RESULTADO,
            decidirAposFalhaDaAnalise(historicoDe(historico(analiseGravada(), vigenteId = "31"))),
        )
        assertEquals(
            DecisaoAposFalha.AGUARDAR,
            decidirAposFalhaDaAnalise(historicoDe(historico(execucao = "EM_ANDAMENTO", estado = "PROCESSANDO"))),
        )
        assertEquals(
            DecisaoAposFalha.PERMITIR_NOVA_TENTATIVA,
            decidirAposFalhaDaAnalise(historicoDe(historico(analiseGravada("30", vigente = false)))),
        )
    }

    @Test
    fun `rotulo usa o texto da RC-15 e, sem ele, o rotulo local`() {
        val analise = historicoDe(historico(analiseGravada(), vigenteId = "31")).analises.single()

        assertEquals("Com pendências", rotuloDoResultado(analise))
        assertEquals("Com pendências", rotuloDoResultado(analise.copy(rotuloResultado = null)))
        assertEquals("Estado não reconhecido", rotuloDoResultado(analise.copy(rotuloResultado = null, resultadoGeral = "NOVO")))
    }

    @Test
    fun `detalhe usa o nome do arquivo da mesma analise e o trecho literal`() {
        val analise = historicoDe(historico(analiseGravada(), vigenteId = "31")).analises.single()
        val detalhe = detalheDe("12", analise, vigente = true, pendencia = analise.pendencias.single())

        val linha = linhasEvidencia(detalhe).single()

        assertEquals("0.1", detalhe.versaoProtocolo)
        assertEquals(
            "Documento: encaminhamento-sintetico.pdf · motivo do encaminhamento não localizado na leitura · página 1",
            linha.documento,
        )
        assertEquals("Trecho lido: “Motivo: <a> & b”", linha.trecho)
    }

    @Test
    fun `sem origem nao ha trecho nem pagina inventados`() {
        val analise = historicoDe(historico(analiseGravada(), vigenteId = "31")).analises.single()
        val pendencia = analise.pendencias.single().let { p -> p.copy(evidencias = p.evidencias.map { it.copy(origem = null) }) }
        val detalhe = detalheDe("12", analise, vigente = true, pendencia = pendencia)

        val linha = linhasEvidencia(detalhe).single()

        assertNull(linha.trecho)
        assertFalse(linha.documento.contains("página"))
    }

    private fun pendencia(
        regra: String,
        tipo: String = "PENDENTE",
        responsavel: String = "PACIENTE",
        motivo: String? = null,
        orientacao: String? = "Orientação da API.",
    ) = PendenciaGravada(regra, "Critério", tipo, "Explicação.", motivo, responsavel, orientacao)

    @Test
    fun `unidade emissora corrige o original e o app nao oferece edicao`() {
        val textos = orientacoesDoApp(pendencia("RC-R04", responsavel = "UNIDADE_EMISSORA"))

        assertTrue(Textos.UNIDADE_CORRIGE in textos)
        assertFalse(Textos.REENVIO_INDISPONIVEL in textos)
    }

    @Test
    fun `arquivo ilegivel ou ausente explica que o reenvio nao esta disponivel`() {
        val ilegivel = orientacoesDoApp(pendencia("RC-R03", tipo = "NAO_VERIFICAVEL", motivo = "CAMPO_ILEGIVEL"))
        val ausente = orientacoesDoApp(pendencia("RC-R03"))

        assertEquals(listOf(Textos.REENVIO_INDISPONIVEL), ilegivel)
        assertEquals(listOf(Textos.REENVIO_INDISPONIVEL), ausente)
    }

    @Test
    fun `RC-R02 sem orientacao da API ganha a frase condicional e mantem o responsavel`() {
        val semOrientacao = pendencia("RC-R02", tipo = "NAO_VERIFICAVEL", motivo = "DIVERGENCIA_APONTADA", orientacao = null)

        val textos = orientacoesDoApp(semOrientacao)

        assertEquals(Textos.ORIENTACAO_RC_R02, textos.first())
        assertFalse(Textos.UNIDADE_CORRIGE in textos)
        assertEquals("Não verificável · você apontou uma diferença", situacaoDaPendencia(semOrientacao))
        assertFalse(orientacoesDoApp(semOrientacao.copy(orientacao = "Da API.")).contains(Textos.ORIENTACAO_RC_R02))
    }
}
