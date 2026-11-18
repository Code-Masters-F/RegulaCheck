package br.com.regulacheck.app.ui.resultado

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.regulacheck.app.api.ErroApi
import br.com.regulacheck.app.api.RegulaCheckApi
import br.com.regulacheck.app.api.Resultado
import br.com.regulacheck.app.ui.navegacao.EstadoTela
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Pedido de nova tentativa do resumo simulado (E3.3). */
sealed interface EstadoResumo {
    data object Ocioso : EstadoResumo
    data object Pedindo : EstadoResumo
    data class Falhou(val erro: ErroApi) : EstadoResumo
}

/**
 * E3.3 Resultado da verificação: consulta o histórico (`GET …/analises`) e apresenta só a análise
 * vigente — ou a análise [analiseId] pedida como "Resultado anterior" (spec 019).
 */
class ResultadoViewModel(
    private val api: RegulaCheckApi,
    private val pedidoId: String,
    private val analiseId: String?,
) : ViewModel() {

    private val estadoMutavel = MutableStateFlow<EstadoTela<ApresentacaoHistorico>>(EstadoTela.Carregando)
    val estado: StateFlow<EstadoTela<ApresentacaoHistorico>> = estadoMutavel.asStateFlow()

    private val resumoMutavel = MutableStateFlow<EstadoResumo>(EstadoResumo.Ocioso)
    val resumo: StateFlow<EstadoResumo> = resumoMutavel.asStateFlow()

    private var carga: Job? = null

    /**
     * Consulta o histórico; ignorada durante outra consulta ou um pedido de resumo. Com resultado
     * exibido (volta do detalhe, "Atualizar"), atualiza sem voltar ao esqueleto.
     */
    fun carregar() {
        if (carga?.isActive == true || resumoMutavel.value == EstadoResumo.Pedindo) return
        if (estadoMutavel.value !is EstadoTela.Sucesso) estadoMutavel.value = EstadoTela.Carregando
        carga = viewModelScope.launch { consultar() }
    }

    private suspend fun consultar() {
        estadoMutavel.value = when (val resultado = api.consultarAnalises(pedidoId)) {
            is Resultado.Sucesso -> EstadoTela.Sucesso(apresentarHistorico(resultado.valor, analiseId))
            is Resultado.Falha -> EstadoTela.Falha(resultado.erro)
        }
    }

    /**
     * "Tentar de novo" do resumo (`POST …/resumo`), só para a análise vigente. Toques durante o
     * pedido são ignorados. 409 reconsulta o histórico antes de reapresentar (spec 019); outra
     * falha mantém resultado e pendências e deixa tentar de novo.
     */
    fun pedirResumo() {
        val atual = (estadoMutavel.value as? EstadoTela.Sucesso)?.dados as? ApresentacaoHistorico.Analise ?: return
        if (!atual.vigente || resumoMutavel.value == EstadoResumo.Pedindo || carga?.isActive == true) return
        resumoMutavel.value = EstadoResumo.Pedindo
        carga = viewModelScope.launch {
            when (val resultado = api.gerarResumo(pedidoId)) {
                is Resultado.Sucesso -> {
                    // A análise é atualizada antes de liberar o botão: a tela nunca mostra
                    // "Tentar de novo" habilitado com o resumo antigo depois de um sucesso.
                    if (resultado.valor.id == atual.analise.id) {
                        estadoMutavel.value = EstadoTela.Sucesso(atual.copy(analise = resultado.valor))
                    } else {
                        // A vigente mudou: nada de juntar dados de análises diferentes.
                        consultar()
                    }
                    resumoMutavel.value = EstadoResumo.Ocioso
                }
                is Resultado.Falha -> {
                    if (resultado.erro is ErroApi.Conflito) {
                        resumoMutavel.value = EstadoResumo.Ocioso
                        consultar()
                    } else {
                        resumoMutavel.value = EstadoResumo.Falhou(resultado.erro)
                    }
                }
            }
        }
    }
}
