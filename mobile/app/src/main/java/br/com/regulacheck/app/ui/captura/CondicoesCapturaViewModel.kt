package br.com.regulacheck.app.ui.captura

import androidx.lifecycle.ViewModel
import br.com.regulacheck.app.api.ErroApi
import br.com.regulacheck.app.api.RegulaCheckApi
import br.com.regulacheck.app.api.Resultado
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex

/** Intervalo entre o fim de uma consulta de telemetria e o início da próxima (#47: 3 a 5 s). */
const val INTERVALO_ATUALIZACAO_MS = 4_000L

/** Estado da E4.1 (research D6). */
sealed interface EstadoCaptura {
    /** Primeira consulta da visita em andamento; a ação de seguir já está disponível. */
    data object Carregando : EstadoCaptura

    /** Telemetria (ou o estado indisponível) exibida; a consulta periódica continua. */
    data class Exibindo(val apresentacao: ApresentacaoCaptura) : EstadoCaptura

    /**
     * 401, 403, 404 ou 409 ao abrir a sessão: a tela explica e oferece o caminho de volta; nenhuma
     * consulta é feita até uma nova visita.
     */
    data class Encerrada(val erro: ErroApi) : EstadoCaptura
}

/**
 * E4.1 Condições da captura (RC-22): abre a sessão de captura SIMULADA do pedido e acompanha a
 * telemetria enquanto a tela está visível.
 *
 * O laço vive na corrotina de quem chama [acompanhar] (a tela, dentro de `repeatOnLifecycle`):
 * sair da tela ou ir para segundo plano o cancela, e o cancelamento cancela a chamada HTTP em
 * andamento. Não há retentativa: cada consulta é uma leitura nova, feita só depois que a anterior
 * terminou (research D3).
 *
 * @param intervaloMs espera entre consultas; os testes usam valores menores.
 */
class CondicoesCapturaViewModel(
    private val api: RegulaCheckApi,
    private val pedidoId: String,
    private val intervaloMs: Long = INTERVALO_ATUALIZACAO_MS,
) : ViewModel() {

    private val estadoMutavel = MutableStateFlow<EstadoCaptura>(EstadoCaptura.Carregando)
    val estado: StateFlow<EstadoCaptura> = estadoMutavel.asStateFlow()

    /** Impede dois laços de consulta ao mesmo tempo (ex.: recomposição durante a volta à tela). */
    private val laco = Mutex()

    /** A sessão de captura foi confirmada (201/200) nesta visita; não é pedida de novo. */
    private var sessaoConfirmada = false

    private var memoria = MemoriaCaptura()

    /**
     * Começa uma visita: volta a "carregando", esquece a recuperação da visita anterior e libera um
     * novo pedido de sessão. Chamada ao tocar em "Fotografar", não na rotação.
     */
    fun novaVisita() {
        estadoMutavel.value = EstadoCaptura.Carregando
        memoria = MemoriaCaptura()
        sessaoConfirmada = false
    }

    /**
     * Pede a sessão (uma vez por visita, se ainda não confirmada) e consulta a telemetria a cada
     * [intervaloMs] até ser cancelada ou até um erro que encerra a tela. Uma segunda chamada
     * simultânea retorna sem fazer nada.
     *
     * Falha de rede ou do serviço no pedido de sessão não é repetida aqui: a tela fica
     * "indisponível" e segue só com a consulta, que é leitura. Uma nova visita, ou a volta do
     * segundo plano, pede a sessão de novo; a rota é idempotente.
     */
    suspend fun acompanhar() {
        if (!laco.tryLock()) return
        try {
            if (estadoMutavel.value is EstadoCaptura.Encerrada) return
            if (!sessaoConfirmada) {
                when (val resultado = api.abrirSessaoCaptura(pedidoId)) {
                    is Resultado.Sucesso -> sessaoConfirmada = true
                    is Resultado.Falha -> if (encerraCaptura(resultado.erro, aoAbrirSessao = true)) {
                        estadoMutavel.value = EstadoCaptura.Encerrada(resultado.erro)
                        return
                    }
                }
            }
            while (consultar()) delay(intervaloMs)
        } finally {
            laco.unlock()
        }
    }

    /** Uma consulta. @return `false` quando a tela foi encerrada e o laço deve parar. */
    private suspend fun consultar(): Boolean {
        when (val resultado = api.consultarTelemetria(pedidoId)) {
            is Resultado.Sucesso -> {
                val calculado = apresentarTelemetria(resultado.valor, memoria)
                memoria = calculado.memoria
                estadoMutavel.value = EstadoCaptura.Exibindo(calculado.apresentacao)
            }
            is Resultado.Falha -> {
                if (encerraCaptura(resultado.erro, aoAbrirSessao = false)) {
                    estadoMutavel.value = EstadoCaptura.Encerrada(resultado.erro)
                    return false
                }
                // Rede, tempo ou serviço: indisponível, sem travar; a memória da visita é mantida.
                estadoMutavel.value = EstadoCaptura.Exibindo(apresentacaoIndisponivel())
            }
        }
        return true
    }
}

/**
 * Erros que encerram a E4.1 (não são passageiros). 409 só existe ao abrir a sessão (pedido
 * submetido). Os demais (rede, tempo, 5xx, resposta fora do contrato) viram "indisponível".
 */
internal fun encerraCaptura(erro: ErroApi, aoAbrirSessao: Boolean): Boolean = when (erro) {
    ErroApi.SessaoExpirada, ErroApi.SemPermissao, ErroApi.NaoEncontrado -> true
    is ErroApi.Conflito -> aoAbrirSessao
    else -> false
}
