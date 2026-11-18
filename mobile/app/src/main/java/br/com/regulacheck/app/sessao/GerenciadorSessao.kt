package br.com.regulacheck.app.sessao

import java.security.GeneralSecurityException
import java.security.ProviderException
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Por que não há sessão: decide se o login mostra o aviso de sessão expirada (E1.4). */
enum class MotivoEncerramento { NENHUMA, SAIU, EXPIROU }

/** Estado da conta no aparelho, observado pela navegação. */
sealed interface EstadoSessao {
    data class Ativa(val sessao: SessaoLocal) : EstadoSessao
    data class Encerrada(val motivo: MotivoEncerramento) : EstadoSessao
}

/**
 * Fonte única da sessão do app. A navegação observa [estado]: `Ativa` leva a "Meus pedidos";
 * `Encerrada` leva ao login. Seguro para chamadas de qualquer thread.
 *
 * @param relogio instante atual (injetável nos testes).
 */
class GerenciadorSessao(
    private val cofre: CofreSessao,
    private val relogio: () -> Instant = Instant::now,
) {
    private val estadoMutavel = MutableStateFlow(carregarInicial())

    val estado: StateFlow<EstadoSessao> = estadoMutavel.asStateFlow()

    /** Token da sessão ativa, ou `null`. Nunca deve ser registrado em log. */
    fun tokenAtual(): String? = (estadoMutavel.value as? EstadoSessao.Ativa)?.sessao?.token

    /**
     * Abre a sessão e a guarda cifrada. Se o Keystore recusar a gravação, a sessão vale só
     * enquanto o app estiver aberto: o token nunca é gravado sem cifra.
     */
    fun abrir(sessao: SessaoLocal) {
        try {
            cofre.salvar(sessao)
        } catch (e: GeneralSecurityException) {
            cofre.apagar()
        } catch (e: ProviderException) {
            cofre.apagar()
        }
        estadoMutavel.value = EstadoSessao.Ativa(sessao)
    }

    /** Apaga a sessão local (sempre) e informa o motivo à navegação. */
    fun encerrar(motivo: MotivoEncerramento) {
        cofre.apagar()
        estadoMutavel.value = EstadoSessao.Encerrada(motivo)
    }

    private fun carregarInicial(): EstadoSessao {
        val sessao = cofre.ler() ?: return EstadoSessao.Encerrada(MotivoEncerramento.NENHUMA)
        if (sessao.expirou(relogio())) {
            cofre.apagar()
            return EstadoSessao.Encerrada(MotivoEncerramento.EXPIROU)
        }
        return EstadoSessao.Ativa(sessao)
    }
}
