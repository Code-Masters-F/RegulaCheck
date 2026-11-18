package br.com.regulacheck.app.api

/**
 * Falha de uma chamada à API do RegulaCheck, em categorias fechadas (issue #41; research D6).
 *
 * As telas escolhem o texto a partir da categoria (textos de `mobile/design/textos.md`). A
 * mensagem do servidor só é guardada nas categorias em que o contrato (`docs/api/erros.md`) a
 * dirige à pessoa: conflito, arquivo recusado e requisição inválida.
 */
sealed interface ErroApi {

    /** Falha de rede antes de obter resposta (servidor parado, sem rede, nome não resolvido). */
    data object SemConexao : ErroApi

    /** A conexão ou a leitura passou do tempo configurado no cliente. */
    data object TempoEsgotado : ErroApi

    /** 401 no login (`CREDENCIAIS_INVALIDAS`): e-mail ou senha incorretos. */
    data object CredenciaisInvalidas : ErroApi

    /** 401 em rota privada (`NAO_AUTENTICADO`): o token deixou de valer; a sessão local é apagada. */
    data object SessaoExpirada : ErroApi

    /** 403 (`ACESSO_NEGADO`): a conta não pode usar a rota; a sessão continua. */
    data object SemPermissao : ErroApi

    /** 404 (`NAO_ENCONTRADO`): recurso inexistente ou de outra paciente (mesma resposta). */
    data object NaoEncontrado : ErroApi

    /**
     * 409: operação incompatível com o estado atual.
     *
     * @property retryAfterSegundos valor de `Retry-After` em segundos, quando o servidor o envia
     * (`ANALISE_EM_ANDAMENTO`); `null` se ausente ou ilegível. O app não repete sozinho.
     */
    data class Conflito(
        val codigo: String?,
        val mensagem: String?,
        val retryAfterSegundos: Long?,
    ) : ErroApi

    /** 413 ou 415: arquivo grande demais ou de tipo não suportado. */
    data class ArquivoRecusado(val codigo: String?, val mensagem: String?) : ErroApi

    /** 400 ou 422: entrada recusada pelo servidor, com detalhes por campo quando houver. */
    data class RequisicaoInvalida(
        val codigo: String?,
        val mensagem: String?,
        val detalhes: List<DetalheErro>,
    ) : ErroApi

    /**
     * 5xx: serviço ou dependência indisponível.
     *
     * @property codigo código do corpo de erro quando houver (ex.: `LEITURA_INDISPONIVEL`,
     * `ANALISE_INDISPONIVEL`, `DEPENDENCIA_INDISPONIVEL`); a tela não depende dele.
     */
    data class Indisponivel(val status: Int, val codigo: String? = null) : ErroApi

    /**
     * O arquivo escolhido pela paciente não pôde ser aberto (permissão do seletor perdida, arquivo
     * apagado). Nada foi enviado; é preciso escolher o arquivo de novo (RC-19, research D1).
     */
    data object ArquivoInacessivel : ErroApi

    /** Status não previsto no contrato ou corpo de sucesso fora do formato esperado. */
    data class RespostaInesperada(val status: Int) : ErroApi
}
