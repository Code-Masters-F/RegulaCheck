package br.com.regulacheck.app.ui

/**
 * Estados do pedido no contrato da API, mais [DESCONHECIDO] para qualquer valor novo: o app
 * mostra "Estado não reconhecido" em vez de falhar (#41).
 */
enum class EstadoPedido {
    RASCUNHO,
    PROCESSANDO,
    REQUER_CONFERENCIA,
    COM_PENDENCIAS,
    SEM_PENDENCIAS_IDENTIFICADAS,
    FALHA_ANALISE,
    SUBMETIDA_SIMULACAO,
    DESCONHECIDO;

    companion object {
        /** Converte o texto da API; valor desconhecido (ou o próprio "DESCONHECIDO") vira [DESCONHECIDO]. */
        fun daApi(valor: String): EstadoPedido =
            entries.firstOrNull { it != DESCONHECIDO && it.name == valor } ?: DESCONHECIDO
    }
}

/**
 * Ação principal de um pedido (E1.3, coluna "Ação principal").
 *
 * @property disponivel `false` quando depende de rotas da RC-19: o botão aparece, mas avisa que a
 * etapa ainda não está disponível (research D7), sem simular que algo foi feito.
 */
enum class AcaoPedido(val rotulo: String, val disponivel: Boolean) {
    ANEXAR_DOCUMENTOS("Anexar documentos", false),
    LER_DOCUMENTOS("Ler documentos", false),
    ATUALIZAR("Atualizar", true),
    VER_O_QUE_CONFERIR("Ver o que conferir", false),
    VER_PENDENCIAS("Ver pendências", false),
    VER_RESULTADO("Ver resultado", false),
    TENTAR_DE_NOVO("Tentar de novo", false),
}

/** Sinal visual do estado: a tela traduz em ícone e cor; o texto sempre acompanha. */
enum class SinalEstado { RASCUNHO, EM_ANDAMENTO, CONFERIR, PENDENCIA, SEM_PENDENCIA, FALHA, CONCLUIDO, DESCONHECIDO }

/** Como um estado aparece: rótulo (texto + ícone), mensagem de próximo passo e ação principal. */
data class ApresentacaoEstado(
    val rotulo: String,
    val sinal: SinalEstado,
    val mensagem: String,
    val acao: AcaoPedido?,
)

/**
 * Apresentação de um pedido conforme `mobile/design/textos.md` (F4 e E1.3).
 *
 * No rascunho, a RC-18 só distingue "sem documentos" de "com documentos" ([documentosDisponiveis]):
 * as situações "a conferir" e "pronto para verificar" dependem das rotas de extração da RC-19.
 * `SEM_PENDENCIAS_IDENTIFICADAS` usa "Ver resultado" porque a submissão está fora do épico (#40).
 */
fun apresentarEstado(estado: EstadoPedido, documentosDisponiveis: Int): ApresentacaoEstado = when (estado) {
    EstadoPedido.RASCUNHO ->
        if (documentosDisponiveis <= 0) {
            ApresentacaoEstado(
                "Rascunho",
                SinalEstado.RASCUNHO,
                "Anexe o encaminhamento e um documento de identificação para começar.",
                AcaoPedido.ANEXAR_DOCUMENTOS,
            )
        } else {
            ApresentacaoEstado(
                "Rascunho",
                SinalEstado.RASCUNHO,
                "Leia os documentos para conferir o que foi encontrado.",
                AcaoPedido.LER_DOCUMENTOS,
            )
        }
    EstadoPedido.PROCESSANDO -> ApresentacaoEstado(
        "Em verificação",
        SinalEstado.EM_ANDAMENTO,
        "A verificação ainda não terminou. Isso não é um resultado.",
        AcaoPedido.ATUALIZAR,
    )
    EstadoPedido.REQUER_CONFERENCIA -> ApresentacaoEstado(
        "Confira a leitura",
        SinalEstado.CONFERIR,
        "Alguma informação não pôde ser verificada.",
        AcaoPedido.VER_O_QUE_CONFERIR,
    )
    EstadoPedido.COM_PENDENCIAS -> ApresentacaoEstado(
        "Com pendências",
        SinalEstado.PENDENCIA,
        "Veja o que falta e quem pode resolver.",
        AcaoPedido.VER_PENDENCIAS,
    )
    EstadoPedido.SEM_PENDENCIAS_IDENTIFICADAS -> ApresentacaoEstado(
        "Sem pendências identificadas",
        SinalEstado.SEM_PENDENCIA,
        "Você pode concluir a simulação. Isso não é aprovação do SUS.",
        AcaoPedido.VER_RESULTADO,
    )
    EstadoPedido.FALHA_ANALISE -> ApresentacaoEstado(
        "Falha na verificação",
        SinalEstado.FALHA,
        "Nenhuma conclusão foi tirada. Tente de novo.",
        AcaoPedido.TENTAR_DE_NOVO,
    )
    EstadoPedido.SUBMETIDA_SIMULACAO -> ApresentacaoEstado(
        "Simulação concluída",
        SinalEstado.CONCLUIDO,
        "Nada foi enviado ao SUS. Consulte o comprovante.",
        null,
    )
    EstadoPedido.DESCONHECIDO -> ApresentacaoEstado(
        Textos.ESTADO_NAO_RECONHECIDO,
        SinalEstado.DESCONHECIDO,
        Textos.MENSAGEM_ESTADO_NAO_RECONHECIDO,
        null,
    )
}

/**
 * Rótulo do resultado geral da análise concluída, ou `null` se não houver resultado.
 * Valor desconhecido aparece como "Estado não reconhecido".
 */
fun rotuloResultadoGeral(resultadoGeral: String?): String? = when (resultadoGeral) {
    null -> null
    "COM_PENDENCIAS" -> "Com pendências"
    "REQUER_CONFERENCIA" -> "Confira a leitura"
    "SEM_PENDENCIAS_IDENTIFICADAS" -> "Sem pendências identificadas"
    else -> Textos.ESTADO_NAO_RECONHECIDO
}
