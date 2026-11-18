package br.com.regulacheck.app.ui.leitura

import br.com.regulacheck.app.api.HistoricoAnalises

/** O que fazer depois de a análise passar do tempo ou perder a conexão (research D6). */
enum class DecisaoAposFalha {
    /** Há análise vigente: ela foi gravada; abrir o resultado em vez de pedir de novo. */
    MOSTRAR_RESULTADO,

    /** A análise ainda está em andamento no servidor: informar e deixar consultar depois. */
    AGUARDAR,

    /** Nada foi gravado nem está em andamento: a paciente pode pedir a análise de novo. */
    PERMITIR_NOVA_TENTATIVA,
}

/**
 * Decide, pelo histórico consultado **depois** da falha, se a análise interrompida no cliente
 * chegou a ser gravada. O `POST` não é repetido sem essa consulta (#40, #42): repetir às cegas
 * poderia esconder um resultado já gravado ou esbarrar numa análise em andamento.
 */
fun decidirAposFalhaDaAnalise(historico: HistoricoAnalises): DecisaoAposFalha = when {
    historico.vigenteId != null && historico.analises.any { it.id == historico.vigenteId && it.vigente } ->
        DecisaoAposFalha.MOSTRAR_RESULTADO
    historico.execucao?.situacao == "EM_ANDAMENTO" -> DecisaoAposFalha.AGUARDAR
    else -> DecisaoAposFalha.PERMITIR_NOVA_TENTATIVA
}
