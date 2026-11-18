package br.com.regulacheck.app.ui.navegacao

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import br.com.regulacheck.app.api.ErroApi
import br.com.regulacheck.app.ui.resultado.DetalhePendencia

/** Telas do app (navegação sem biblioteca, #40): RC-18 e RC-19. */
sealed interface Tela {
    /** E1.1; com [sessaoExpirada], mostra o aviso de E1.4 acima do formulário. */
    data class Login(val sessaoExpirada: Boolean = false) : Tela

    /** E1.2. */
    data object MeusPedidos : Tela

    /** E1.3 do pedido [id] (identificador opaco devolvido pela API). */
    data class Pedido(val id: String) : Tela

    /** E2.1 Novo pedido (RC-19). */
    data object NovoPedido : Tela

    /** E2.2 Seus documentos do pedido [id], com a folha E2.7 e as recusas E2.3 (RC-19). */
    data class Documentos(val id: String) : Tela

    /** E3.1 Ler documentos e E3.8 Verificar critérios do pedido [id] (RC-19). */
    data class Leitura(val id: String) : Tela

    /**
     * E3.3 Resultado da verificação do pedido [id] (RC-19).
     *
     * @property analiseId análise a consultar como "Resultado anterior"; `null` = resultado atual.
     */
    data class Resultado(val id: String, val analiseId: String? = null) : Tela

    /** E3.4 Detalhe da pendência, com os dados da mesma análise já carregada (RC-19). */
    data class Pendencia(val detalhe: DetalhePendencia) : Tela
}

/** Estado de uma tela que carrega dados da API (#40): usado por todas as telas com consulta. */
sealed interface EstadoTela<out T> {
    data object Carregando : EstadoTela<Nothing>
    data object Vazio : EstadoTela<Nothing>
    data class Sucesso<T>(val dados: T) : EstadoTela<T>
    data class Falha(val erro: ErroApi) : EstadoTela<Nothing>
}

/**
 * Entrada da pilha de navegação. Cada entrada tem o próprio [ViewModelStore]: os ViewModels de
 * uma tela vivem enquanto ela está na pilha e são limpos quando ela sai (research D3).
 */
class EntradaPilha internal constructor(val tela: Tela) : ViewModelStoreOwner {
    override val viewModelStore: ViewModelStore = ViewModelStore()
}

/**
 * Pilha de telas, guardada num ViewModel da Activity para sobreviver à rotação.
 *
 * Ao encerrar a sessão, [reiniciar] limpa todas as entradas: nenhum dado de uma conta fica em
 * memória para a próxima.
 */
class Navegador(inicial: Tela) : ViewModel() {

    var pilha: List<EntradaPilha> by mutableStateOf(listOf(EntradaPilha(inicial)))
        private set

    val atual: EntradaPilha get() = pilha.last()

    /** Abre [tela] sobre a atual. */
    fun abrir(tela: Tela) {
        pilha = pilha + EntradaPilha(tela)
    }

    /**
     * Troca a tela atual por [tela] (novo pedido → documentos; leitura → resultado), para que
     * "voltar" não reabra uma operação já concluída. O estado da tela trocada é descartado.
     */
    fun substituir(tela: Tela) {
        val topo = pilha.last()
        pilha = pilha.dropLast(1) + EntradaPilha(tela)
        topo.viewModelStore.clear()
    }

    /** Volta uma tela. @return `false` se já está na primeira (o sistema fecha o app). */
    fun voltar(): Boolean {
        if (pilha.size <= 1) return false
        val topo = pilha.last()
        pilha = pilha.dropLast(1)
        topo.viewModelStore.clear()
        return true
    }

    /** Substitui toda a pilha por [tela] e descarta o estado das telas anteriores. */
    fun reiniciar(tela: Tela) {
        val anteriores = pilha
        pilha = listOf(EntradaPilha(tela))
        anteriores.forEach { it.viewModelStore.clear() }
    }

    override fun onCleared() {
        pilha.forEach { it.viewModelStore.clear() }
    }
}
