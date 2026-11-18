package br.com.regulacheck.app.ui.navegacao

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.regulacheck.app.Dependencias
import br.com.regulacheck.app.sessao.EstadoSessao
import br.com.regulacheck.app.sessao.MotivoEncerramento
import br.com.regulacheck.app.ui.login.LoginTela
import br.com.regulacheck.app.ui.login.LoginViewModel
import br.com.regulacheck.app.ui.pedido.PedidoTela
import br.com.regulacheck.app.ui.pedido.PedidoViewModel
import br.com.regulacheck.app.ui.pedidos.MeusPedidosTela
import br.com.regulacheck.app.ui.pedidos.MeusPedidosViewModel

/** Tela inicial a partir da sessão guardada. */
private fun telaInicial(estado: EstadoSessao): Tela = when (estado) {
    is EstadoSessao.Ativa -> Tela.MeusPedidos
    is EstadoSessao.Encerrada -> Tela.Login(sessaoExpirada = estado.motivo == MotivoEncerramento.EXPIROU)
}

/**
 * Raiz da interface: escolhe a tela pela pilha do [Navegador] e reage à sessão — sessão aberta
 * leva a "Meus pedidos"; sessão encerrada (sair, 401 ou expiração) leva ao login e descarta as
 * telas anteriores.
 */
@Composable
fun AppRegulaCheck(dependencias: Dependencias) {
    val estadoSessao by dependencias.gerenciadorSessao.estado.collectAsStateWithLifecycle()
    val navegador: Navegador = viewModel { Navegador(telaInicial(dependencias.gerenciadorSessao.estado.value)) }

    LaunchedEffect(estadoSessao) {
        val emLogin = navegador.atual.tela is Tela.Login
        when (val sessao = estadoSessao) {
            is EstadoSessao.Ativa -> if (emLogin) navegador.reiniciar(Tela.MeusPedidos)
            // No login, não reinicia: preservaria a tela, mas apagaria o e-mail digitado.
            is EstadoSessao.Encerrada -> if (!emLogin) {
                navegador.reiniciar(Tela.Login(sessaoExpirada = sessao.motivo == MotivoEncerramento.EXPIROU))
            }
        }
    }

    BackHandler(enabled = navegador.pilha.size > 1) { navegador.voltar() }

    val entrada = navegador.atual
    key(entrada) {
        CompositionLocalProvider(LocalViewModelStoreOwner provides entrada) {
            when (val tela = entrada.tela) {
                is Tela.Login -> LoginTela(
                    viewModel = viewModel { LoginViewModel(dependencias.servicoSessao) },
                    sessaoExpirada = tela.sessaoExpirada,
                )
                Tela.MeusPedidos -> MeusPedidosTela(
                    viewModel = viewModel { MeusPedidosViewModel(dependencias.api) },
                    primeiroNome = (estadoSessao as? EstadoSessao.Ativa)?.sessao?.primeiroNome.orEmpty(),
                    aoAbrirPedido = { id -> navegador.abrir(Tela.Pedido(id)) },
                    aoSair = { dependencias.servicoSessao.sair() },
                )
                is Tela.Pedido -> PedidoTela(
                    viewModel = viewModel { PedidoViewModel(dependencias.api, tela.id) },
                    aoVoltar = { navegador.voltar() },
                )
            }
        }
    }
}
