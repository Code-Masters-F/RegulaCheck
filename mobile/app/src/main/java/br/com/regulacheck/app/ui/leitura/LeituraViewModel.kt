package br.com.regulacheck.app.ui.leitura

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.regulacheck.app.api.ErroApi
import br.com.regulacheck.app.api.Extracao
import br.com.regulacheck.app.api.RegulaCheckApi
import br.com.regulacheck.app.api.Resultado
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Leitura simulada (E3.1). */
sealed interface EstadoLeitura {
    data object Lendo : EstadoLeitura
    data class Lida(val extracao: Extracao) : EstadoLeitura
    data class Falhou(val erro: ErroApi) : EstadoLeitura
}

/** Verificação dos critérios depois da leitura (E3.8 e research D6). */
sealed interface EstadoAnalise {
    data object Ociosa : EstadoAnalise

    /** Diálogo "Verificar critérios?" aberto. */
    data object Confirmando : EstadoAnalise

    /** `POST …/analises` (ou a consulta ao histórico depois de uma falha) em andamento. */
    data object Analisando : EstadoAnalise

    /** Outra análise do pedido está rodando no servidor; [retryAfterSegundos] quando a API informa. */
    data class EmAndamento(val retryAfterSegundos: Long?) : EstadoAnalise

    /** Tempo esgotado ou sem conexão, e a consulta ao histórico também falhou: nada é presumido. */
    data object Incerta : EstadoAnalise

    /** A análise não foi feita (erro da API, ou falha de rede confirmada pelo histórico). */
    data class Falhou(val erro: ErroApi) : EstadoAnalise

    /** Análise confirmada pela API: a tela abre o resultado. */
    data object Concluida : EstadoAnalise
}

/**
 * E3.1 Ler documentos e E3.8 Verificar critérios do pedido [pedidoId].
 *
 * Nenhuma chamada é repetida sozinha (#40): cada nova tentativa vem de um toque, e toques durante
 * uma operação são ignorados. A leitura começa ao abrir a tela — a paciente acabou de tocar "Ler
 * documentos"; repetir sobre a mesma versão devolve a leitura existente (200), sem duplicar.
 */
class LeituraViewModel(private val api: RegulaCheckApi, private val pedidoId: String) : ViewModel() {

    private val leituraMutavel = MutableStateFlow<EstadoLeitura>(EstadoLeitura.Lendo)
    val leitura: StateFlow<EstadoLeitura> = leituraMutavel.asStateFlow()

    private val analiseMutavel = MutableStateFlow<EstadoAnalise>(EstadoAnalise.Ociosa)
    val analise: StateFlow<EstadoAnalise> = analiseMutavel.asStateFlow()

    private var lendo = false

    init {
        ler()
    }

    /** `POST …/extracoes` (70 s). Ignorada durante outra leitura ou uma análise. */
    fun ler() {
        if (lendo || analiseMutavel.value == EstadoAnalise.Analisando) return
        lendo = true
        leituraMutavel.value = EstadoLeitura.Lendo
        analiseMutavel.value = EstadoAnalise.Ociosa
        viewModelScope.launch {
            leituraMutavel.value = when (val resultado = api.lerDocumentos(pedidoId)) {
                is Resultado.Sucesso -> EstadoLeitura.Lida(resultado.valor)
                is Resultado.Falha -> EstadoLeitura.Falhou(resultado.erro)
            }
            lendo = false
        }
    }

    /** Abre o diálogo E3.8; só depois de uma leitura confirmada e sem análise em curso. */
    fun pedirConfirmacao() {
        if (leituraMutavel.value !is EstadoLeitura.Lida) return
        when (analiseMutavel.value) {
            EstadoAnalise.Analisando, EstadoAnalise.Concluida -> return
            else -> analiseMutavel.value = EstadoAnalise.Confirmando
        }
    }

    /** "Voltar" do diálogo. */
    fun cancelarConfirmacao() {
        if (analiseMutavel.value == EstadoAnalise.Confirmando) analiseMutavel.value = EstadoAnalise.Ociosa
    }

    /**
     * `POST …/analises` (70 s). Em tempo esgotado ou queda de conexão, consulta o histórico antes de
     * oferecer nova tentativa (research D6). Depois de uma análise confirmada sem resumo, pede o
     * resumo simulado **uma** vez (research D8); a falha dele não impede abrir o resultado.
     */
    fun analisar() {
        when (analiseMutavel.value) {
            EstadoAnalise.Analisando, EstadoAnalise.Concluida -> return
            else -> Unit
        }
        if (leituraMutavel.value !is EstadoLeitura.Lida) return
        analiseMutavel.value = EstadoAnalise.Analisando
        viewModelScope.launch {
            when (val resultado = api.analisar(pedidoId)) {
                is Resultado.Sucesso -> {
                    if (resultado.valor.resumo == null) api.gerarResumo(pedidoId)
                    analiseMutavel.value = EstadoAnalise.Concluida
                }
                is Resultado.Falha -> when (val erro = resultado.erro) {
                    ErroApi.TempoEsgotado, ErroApi.SemConexao -> verificarHistorico(erro)
                    is ErroApi.Conflito ->
                        analiseMutavel.value = if (erro.codigo == CODIGO_ANALISE_EM_ANDAMENTO) {
                            EstadoAnalise.EmAndamento(erro.retryAfterSegundos)
                        } else {
                            EstadoAnalise.Falhou(erro)
                        }
                    else -> analiseMutavel.value = EstadoAnalise.Falhou(erro)
                }
            }
        }
    }

    /** "Consultar de novo" depois de [EstadoAnalise.Incerta]: repete só a consulta, nunca o `POST`. */
    fun consultarDeNovo() {
        if (analiseMutavel.value != EstadoAnalise.Incerta) return
        analiseMutavel.value = EstadoAnalise.Analisando
        viewModelScope.launch { verificarHistorico(ErroApi.TempoEsgotado) }
    }

    private suspend fun verificarHistorico(erroOriginal: ErroApi) {
        analiseMutavel.value = when (val historico = api.consultarAnalises(pedidoId)) {
            is Resultado.Sucesso -> when (decidirAposFalhaDaAnalise(historico.valor)) {
                DecisaoAposFalha.MOSTRAR_RESULTADO -> EstadoAnalise.Concluida
                DecisaoAposFalha.AGUARDAR -> EstadoAnalise.EmAndamento(null)
                DecisaoAposFalha.PERMITIR_NOVA_TENTATIVA -> EstadoAnalise.Falhou(erroOriginal)
            }
            is Resultado.Falha ->
                if (historico.erro == ErroApi.SessaoExpirada) EstadoAnalise.Falhou(historico.erro) else EstadoAnalise.Incerta
        }
    }

    companion object {
        /** 409 de outra análise do mesmo pedido dentro do limite de 1 min (`docs/api/erros.md`). */
        const val CODIGO_ANALISE_EM_ANDAMENTO = "ANALISE_EM_ANDAMENTO"
    }
}
