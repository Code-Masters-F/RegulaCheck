package br.com.regulacheck.app.ui.resultado

import br.com.regulacheck.app.api.AnaliseGravada
import br.com.regulacheck.app.api.HistoricoAnalises
import br.com.regulacheck.app.api.PendenciaGravada
import br.com.regulacheck.app.ui.RotulosApi
import br.com.regulacheck.app.ui.Textos
import br.com.regulacheck.app.ui.rotuloResultadoGeral

/**
 * O que a tela de resultado (E3.3) pode mostrar a partir do histórico de análises, conforme o
 * contrato de apresentação da RC-15 (`specs/019-exibir-resultado-correcao/contracts/
 * apresentacao-resultado.md`). Cada estado usa dados de uma única análise; nenhum mistura revisões.
 */
sealed interface ApresentacaoHistorico {

    /** `execucao.situacao = EM_ANDAMENTO`: andamento em texto, nenhuma conclusão nova. */
    data object EmAndamento : ApresentacaoHistorico

    /** Pedido em `FALHA_ANALISE` sem análise vigente: nenhuma conclusão foi tirada. */
    data class Falha(val revisao: Int) : ApresentacaoHistorico

    /** Nenhuma análise gravada. */
    data object SemAnalise : ApresentacaoHistorico

    /**
     * Há análises, mas nenhuma vale para a versão atual dos documentos.
     *
     * @property anterior a mais recente, só para consulta como "Resultado anterior".
     */
    data class SemVigente(val anterior: AnaliseGravada) : ApresentacaoHistorico

    /**
     * Uma análise para exibir.
     *
     * @property vigente `true` só para a análise atual (`id == vigenteId` e `vigente`); `false` é
     * exibido como "Resultado anterior".
     */
    data class Analise(val analise: AnaliseGravada, val vigente: Boolean) : ApresentacaoHistorico
}

/**
 * Escolhe o que mostrar do [historico].
 *
 * @param analiseId análise pedida explicitamente (consulta de "Resultado anterior"); `null` pede o
 * resultado atual.
 */
fun apresentarHistorico(historico: HistoricoAnalises, analiseId: String? = null): ApresentacaoHistorico {
    if (analiseId != null) {
        historico.analises.firstOrNull { it.id == analiseId }?.let { pedida ->
            return ApresentacaoHistorico.Analise(pedida, ehVigente(historico, pedida))
        }
    }
    if (historico.execucao?.situacao == "EM_ANDAMENTO") return ApresentacaoHistorico.EmAndamento
    val vigente = historico.analises.firstOrNull { ehVigente(historico, it) }
    return when {
        vigente != null -> ApresentacaoHistorico.Analise(vigente, vigente = true)
        historico.estado == "FALHA_ANALISE" -> ApresentacaoHistorico.Falha(historico.versaoConjuntoDocumental)
        historico.analises.isEmpty() -> ApresentacaoHistorico.SemAnalise
        else -> ApresentacaoHistorico.SemVigente(historico.analises.first())
    }
}

/** Só é atual a análise apontada por `vigenteId` e marcada `vigente` pela API (as duas condições). */
private fun ehVigente(historico: HistoricoAnalises, analise: AnaliseGravada): Boolean =
    historico.vigenteId != null && analise.id == historico.vigenteId && analise.vigente

/** Rótulo do resultado geral: o texto da RC-15 quando a API o envia, senão o rótulo local (research D12). */
fun rotuloDoResultado(analise: AnaliseGravada): String =
    analise.rotuloResultado?.takeIf { it.isNotBlank() }
        ?: rotuloResultadoGeral(analise.resultadoGeral)
        ?: Textos.ESTADO_NAO_RECONHECIDO

/**
 * Dados do detalhe de uma pendência (E3.4), todos da mesma análise.
 *
 * @property nomesArquivos `documentoId` → `nomeArquivo` de `documentos[]` da análise.
 * @property vigente `false` quando a análise é um "Resultado anterior".
 * @property revisao revisão avaliada pela análise.
 */
data class DetalhePendencia(
    val pedidoId: String,
    val versaoProtocolo: String,
    val vigente: Boolean,
    val revisao: Int,
    val pendencia: PendenciaGravada,
    val nomesArquivos: Map<String, String>,
)

/** Monta o [DetalhePendencia] de [pendencia] a partir da própria [analise]. */
fun detalheDe(pedidoId: String, analise: AnaliseGravada, vigente: Boolean, pendencia: PendenciaGravada) =
    DetalhePendencia(
        pedidoId = pedidoId,
        versaoProtocolo = RotulosApi.versaoProtocolo(analise.protocolo.versao),
        vigente = vigente,
        revisao = analise.revisao,
        pendencia = pendencia,
        nomesArquivos = analise.documentos.associate { it.documentoId to it.nomeArquivo },
    )

/** Linha de evidência pronta para exibir: documento, campo/situação e origem só se existir. */
data class LinhaEvidencia(val documento: String, val trecho: String?)

/**
 * Evidências do detalhe. O nome vem de `documentos[]` pelo `documentoId` (sem nome conhecido,
 * "documento"); página e trecho só aparecem quando a API os envia — nunca são fabricados. O trecho
 * é devolvido literal (texto puro na tela).
 */
fun linhasEvidencia(detalhe: DetalhePendencia): List<LinhaEvidencia> =
    detalhe.pendencia.evidencias.map { evidencia ->
        val nome = detalhe.nomesArquivos[evidencia.documentoId] ?: "documento"
        val partes = buildList {
            add("${RotulosApi.campo(evidencia.campo)} ${RotulosApi.situacaoEvidencia(evidencia.situacao)}")
            evidencia.origem?.pagina?.let { add(Textos.pagina(it)) }
        }
        LinhaEvidencia(
            documento = Textos.documentoEvidencia(nome, partes.joinToString(" · ")),
            trecho = evidencia.origem?.trecho?.takeIf { it.isNotBlank() }?.let { Textos.trechoLido(it) },
        )
    }

/** Situação do detalhe: "Pendente" ou "Não verificável · {motivo}". */
fun situacaoDaPendencia(pendencia: PendenciaGravada): String {
    val tipo = RotulosApi.resultadoRegra(pendencia.tipo)
    val motivo = pendencia.motivo?.let { RotulosApi.motivo(it).replaceFirstChar { c -> c.lowercase() } }
    return if (motivo != null) "$tipo · $motivo" else tipo
}

/** Regras cuja pendência é a falta de um documento (RC-R01, RC-R03 e RC-R06 do protocolo fictício). */
private val REGRAS_DE_DOCUMENTO = setOf("RC-R01", "RC-R03", "RC-R06")

/**
 * Orientações fixas do app para a pendência, além da `orientacao` da API (spec 019, "Detalhe de
 * pendência"; spec 022 FR-023). Distinguem o que a paciente resolve do que exige a unidade emissora
 * sem afirmar qual original está errado, e explicam limites desta versão em vez de mostrar botões
 * inoperantes.
 */
fun orientacoesDoApp(pendencia: PendenciaGravada): List<String> = buildList {
    // Sem a orientação da RC-15 (backend antigo), a RC-R02 ainda precisa da frase condicional.
    if (pendencia.codigoRegra == "RC-R02" && pendencia.orientacao.isNullOrBlank()) add(Textos.ORIENTACAO_RC_R02)
    if (pendencia.responsavel == "UNIDADE_EMISSORA") add(Textos.UNIDADE_CORRIGE)
    // Arquivo ilegível, não lido ou ausente: a correção é enviar um arquivo novo (RC-16, no backend);
    // o app ainda não oferece esse reenvio e diz isso, sem botão inoperante.
    val precisaArquivoNovo = pendencia.motivo in setOf("CAMPO_ILEGIVEL", "DOCUMENTO_NAO_LIDO") ||
        (pendencia.tipo == "PENDENTE" && pendencia.codigoRegra in REGRAS_DE_DOCUMENTO)
    if (precisaArquivoNovo) add(Textos.REENVIO_INDISPONIVEL)
}

/**
 * Linha de apoio de um item de "O que falta": "{tipo} · {primeiro arquivo}". O código da regra não
 * aparece para a paciente (ajuste de 19/11/2026).
 */
fun apoioItemPendencia(pendencia: PendenciaGravada, nomesArquivos: Map<String, String>): String {
    val arquivo = pendencia.evidencias.firstNotNullOfOrNull { nomesArquivos[it.documentoId] }
    return listOfNotNull(RotulosApi.resultadoRegra(pendencia.tipo), arquivo).joinToString(" · ")
}
