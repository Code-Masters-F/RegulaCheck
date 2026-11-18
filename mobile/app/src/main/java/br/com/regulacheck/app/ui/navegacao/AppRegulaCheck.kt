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
import br.com.regulacheck.app.ui.AcaoPedido
import br.com.regulacheck.app.ui.documentos.DocumentosTela
import br.com.regulacheck.app.ui.documentos.DocumentosViewModel
import br.com.regulacheck.app.ui.leitura.LeituraTela
import br.com.regulacheck.app.ui.leitura.LeituraViewModel
import br.com.regulacheck.app.ui.novopedido.NovoPedidoTela
import br.com.regulacheck.app.ui.novopedido.NovoPedidoViewModel
import br.com.regulacheck.app.ui.pendencia.PendenciaTela
import br.com.regulacheck.app.ui.resultado.ResultadoTela
import br.com.regulacheck.app.ui.resultado.ResultadoViewModel

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
                    aoNovoPedido = { navegador.abrir(Tela.NovoPedido) },
                    aoAgir = { id, acao -> navegador.abrirAcao(id, acao) },
                    aoSair = { dependencias.servicoSessao.sair() },
                )
                is Tela.Pedido -> PedidoTela(
                    viewModel = viewModel { PedidoViewModel(dependencias.api, tela.id) },
                    aoVoltar = { navegador.voltar() },
                    aoAgir = { acao -> navegador.abrirAcao(tela.id, acao) },
                )
                Tela.NovoPedido -> NovoPedidoTela(
                    viewModel = viewModel { NovoPedidoViewModel(dependencias.api) },
                    aoVoltar = { navegador.voltar() },
                    // Substitui: "voltar" a partir dos documentos leva a "Meus pedidos", não a um novo rascunho.
                    aoCriar = { id -> navegador.substituir(Tela.Documentos(id)) },
                )
                is Tela.Documentos -> DocumentosTela(
                    viewModel = viewModel { DocumentosViewModel(dependencias.api, tela.id) },
                    pedidoId = tela.id,
                    aoVoltar = { navegador.voltar() },
                    aoLer = { navegador.abrir(Tela.Leitura(tela.id)) },
                    aoVerResultado = { navegador.abrir(Tela.Resultado(tela.id)) },
                )
                is Tela.Leitura -> LeituraTela(
                    viewModel = viewModel { LeituraViewModel(dependencias.api, tela.id) },
                    pedidoId = tela.id,
                    aoVoltar = { navegador.voltar() },
                    aoAnexar = { navegador.substituir(Tela.Documentos(tela.id)) },
                    // Substitui: "voltar" do resultado não repete a leitura nem a análise.
                    aoVerResultado = { navegador.substituir(Tela.Resultado(tela.id)) },
                )
                is Tela.Resultado -> ResultadoTela(
                    viewModel = viewModel { ResultadoViewModel(dependencias.api, tela.id, tela.analiseId) },
                    pedidoId = tela.id,
                    aoVoltar = { navegador.voltar() },
                    aoVoltarParaPedidos = { navegador.reiniciar(Tela.MeusPedidos) },
                    aoAbrirPendencia = { detalhe -> navegador.abrir(Tela.Pendencia(detalhe)) },
                    aoAbrirAnterior = { analiseId -> navegador.abrir(Tela.Resultado(tela.id, analiseId)) },
                    aoLerDeNovo = { navegador.substituir(Tela.Leitura(tela.id)) },
                )
                is Tela.Pendencia -> PendenciaTela(detalhe = tela.detalhe, aoVoltar = { navegador.voltar() })
            }
        }
    }
}

/**
 * Abre a tela da ação principal de um pedido (E1.2/E1.3, RC-19). "Atualizar" é tratada pela própria
 * tela, que consulta o pedido de novo.
 */
private fun Navegador.abrirAcao(id: String, acao: AcaoPedido) {
    when (acao) {
        AcaoPedido.ANEXAR_DOCUMENTOS -> abrir(Tela.Documentos(id))
        AcaoPedido.LER_DOCUMENTOS, AcaoPedido.TENTAR_DE_NOVO -> abrir(Tela.Leitura(id))
        AcaoPedido.VER_O_QUE_CONFERIR, AcaoPedido.VER_PENDENCIAS, AcaoPedido.VER_RESULTADO -> abrir(Tela.Resultado(id))
        AcaoPedido.ATUALIZAR -> Unit
    }
}
