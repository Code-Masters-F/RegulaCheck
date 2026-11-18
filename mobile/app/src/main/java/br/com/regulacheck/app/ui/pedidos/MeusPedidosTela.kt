package br.com.regulacheck.app.ui.pedidos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.regulacheck.app.api.ErroApi
import br.com.regulacheck.app.api.RegulaCheckApi
import br.com.regulacheck.app.api.Resultado
import br.com.regulacheck.app.api.Solicitacao
import br.com.regulacheck.app.ui.AcaoPedido
import br.com.regulacheck.app.ui.EstadoPedido
import br.com.regulacheck.app.ui.Textos
import br.com.regulacheck.app.ui.apresentarEstado
import br.com.regulacheck.app.ui.componentes.BlocoRetorno
import br.com.regulacheck.app.ui.componentes.BotaoAdicionar
import br.com.regulacheck.app.ui.componentes.BotaoPrimario
import br.com.regulacheck.app.ui.componentes.CabecalhoMarca
import br.com.regulacheck.app.ui.componentes.Esqueleto
import br.com.regulacheck.app.ui.componentes.EstruturaTela
import br.com.regulacheck.app.ui.componentes.FORMA_SUPERFICIE
import br.com.regulacheck.app.ui.componentes.RotuloEstado
import br.com.regulacheck.app.ui.componentes.TipoRetorno
import br.com.regulacheck.app.ui.componentes.TituloTela
import br.com.regulacheck.app.ui.formatarDataHora
import br.com.regulacheck.app.ui.navegacao.EstadoTela
import br.com.regulacheck.app.ui.tema.CoresRc
import br.com.regulacheck.app.ui.tema.IconesRc
import br.com.regulacheck.app.ui.tema.TiposRc
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Carrega os pedidos da paciente da sessão (`GET /solicitacoes`). */
class MeusPedidosViewModel(private val api: RegulaCheckApi) : ViewModel() {

    private val estadoMutavel = MutableStateFlow<EstadoTela<List<Solicitacao>>>(EstadoTela.Carregando)
    val estado: StateFlow<EstadoTela<List<Solicitacao>>> = estadoMutavel.asStateFlow()
    private var carga: Job? = null

    /**
     * Consulta a lista. Com dados já exibidos (volta do detalhe), atualiza sem voltar ao
     * esqueleto. Ignorada enquanto outra consulta está em andamento.
     */
    fun carregar() {
        if (carga?.isActive == true) return
        if (estadoMutavel.value !is EstadoTela.Sucesso) estadoMutavel.value = EstadoTela.Carregando
        carga = viewModelScope.launch {
            estadoMutavel.value = when (val resultado = api.listarSolicitacoes()) {
                is Resultado.Sucesso ->
                    if (resultado.valor.itens.isEmpty()) EstadoTela.Vazio else EstadoTela.Sucesso(resultado.valor.itens)
                is Resultado.Falha -> EstadoTela.Falha(resultado.erro)
            }
        }
    }
}

/**
 * E1.2 Meus pedidos (quadros H0BJi, HX74z, v6aVEO, FzpeX, MKpMg). O pedido mais recente aparece
 * em destaque com o próximo passo; os demais em linhas que abrem o pedido.
 *
 * @param primeiroNome da sessão, para "Olá, {primeiro nome}".
 * @param aoNovoPedido abre E2.1 (RC-19).
 * @param aoAgir executa a ação principal do pedido em destaque (exceto "Atualizar", tratada aqui).
 */
@Composable
fun MeusPedidosTela(
    viewModel: MeusPedidosViewModel,
    primeiroNome: String,
    aoAbrirPedido: (String) -> Unit,
    aoNovoPedido: () -> Unit,
    aoAgir: (String, AcaoPedido) -> Unit,
    aoSair: () -> Unit,
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val avisos = remember { SnackbarHostState() }

    // A cada entrada na tela (inclusive ao voltar do detalhe): a lista reflete o estado atual.
    LaunchedEffect(Unit) { viewModel.carregar() }

    EstruturaTela(avisos = avisos, cabecalho = { CabecalhoMarca(aoSair = aoSair) }) {
        TituloTela(
            Textos.PEDIDOS_TITULO,
            acima = Textos.saudacao(primeiroNome),
            abaixo = Textos.PEDIDOS_APOIO,
        )
        Spacer(Modifier.height(32.dp))
        when (val atual = estado) {
            EstadoTela.Carregando -> Esqueleto(Textos.PEDIDOS_CARREGANDO)
            EstadoTela.Vazio -> {
                Text(
                    Textos.PEDIDOS_VAZIO_TITULO,
                    style = TiposRc.Secao,
                    color = CoresRc.Texto,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(12.dp))
                Text(Textos.PEDIDOS_VAZIO_TEXTO, style = TiposRc.Corpo, color = CoresRc.TextoApoio)
                Spacer(Modifier.height(24.dp))
                BotaoPrimario(Textos.NOVO_PEDIDO, aoNovoPedido)
            }
            is EstadoTela.Falha -> {
                FalhaLista(atual.erro, viewModel::carregar)
                if (atual.erro != ErroApi.SemConexao && atual.erro != ErroApi.TempoEsgotado) {
                    Spacer(Modifier.height(24.dp))
                    BotaoAdicionar(Textos.NOVO_PEDIDO, aoNovoPedido)
                }
            }
            is EstadoTela.Sucesso -> {
                val (destaque, demais) = atual.dados.first() to atual.dados.drop(1)
                PedidoDestaque(destaque, aoAbrir = { aoAbrirPedido(destaque.id) }, aoAgir = { acao ->
                    if (acao == AcaoPedido.ATUALIZAR) viewModel.carregar() else aoAgir(destaque.id, acao)
                })
                Spacer(Modifier.height(24.dp))
                demais.forEach { pedido ->
                    LinhaPedido(pedido) { aoAbrirPedido(pedido.id) }
                }
                Spacer(Modifier.height(16.dp))
                BotaoAdicionar(Textos.NOVO_PEDIDO, aoNovoPedido)
            }
        }
    }
}

@Composable
private fun FalhaLista(erro: ErroApi, aoTentar: () -> Unit) {
    when (erro) {
        ErroApi.SemConexao, ErroApi.TempoEsgotado ->
            BlocoRetorno(TipoRetorno.SEM_CONEXAO, Textos.SEM_CONEXAO_TITULO, Textos.PEDIDOS_SEM_CONEXAO_TEXTO, aoTentar = aoTentar)
        ErroApi.SemPermissao -> BlocoRetorno(TipoRetorno.SEM_PERMISSAO, Textos.SEM_PERMISSAO)
        // 401: a sessão já foi encerrada e a navegação leva ao login; nada a mostrar aqui.
        ErroApi.SessaoExpirada -> Unit
        else -> BlocoRetorno(TipoRetorno.ERRO, Textos.PEDIDOS_ERRO_TITULO, Textos.PEDIDOS_ERRO_TEXTO, aoTentar = aoTentar)
    }
}

/** `Pedido/Destaque`: o pedido mais recente, com estado, próximo passo e a ação principal. */
@Composable
private fun PedidoDestaque(pedido: Solicitacao, aoAbrir: () -> Unit, aoAgir: (AcaoPedido) -> Unit) {
    val apresentacao = apresentarEstado(EstadoPedido.daApi(pedido.estado), pedido.documentosDisponiveis)
    Column(
        Modifier
            .fillMaxWidth()
            .background(CoresRc.Superficie, FORMA_SUPERFICIE)
            .padding(20.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clickable(onClickLabel = Textos.tituloPedido(pedido.id), role = Role.Button, onClick = aoAbrir),
        ) {
            Text(
                Textos.linhaPedido(pedido.id, formatarDataHora(pedido.atualizadaEm)),
                style = TiposRc.Meta,
                color = CoresRc.TextoApoio,
            )
            Spacer(Modifier.height(8.dp))
            Text(pedido.servico.especialidade, style = TiposRc.Secao, color = CoresRc.Texto)
            Spacer(Modifier.height(12.dp))
            RotuloEstado(apresentacao.rotulo, apresentacao.sinal)
            Spacer(Modifier.height(12.dp))
            Text(apresentacao.mensagem, style = TiposRc.Corpo, color = CoresRc.Texto)
        }
        apresentacao.acao?.let { acao ->
            Spacer(Modifier.height(20.dp))
            BotaoPrimario(acao.rotulo, { aoAgir(acao) })
        }
    }
}

/** `Pedido/Linha`: número e data, serviço e estado; a linha toda abre o pedido. */
@Composable
private fun LinhaPedido(pedido: Solicitacao, aoAbrir: () -> Unit) {
    val apresentacao = apresentarEstado(EstadoPedido.daApi(pedido.estado), pedido.documentosDisponiveis)
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = aoAbrir),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    Textos.linhaPedido(pedido.id, formatarDataHora(pedido.atualizadaEm)),
                    style = TiposRc.Meta,
                    color = CoresRc.TextoApoio,
                )
                Spacer(Modifier.height(4.dp))
                Text(pedido.servico.especialidade, style = TiposRc.TituloItem, color = CoresRc.Texto)
                Spacer(Modifier.height(8.dp))
                RotuloEstado(apresentacao.rotulo, apresentacao.sinal)
            }
            Icon(IconesRc.SetaDireita, contentDescription = null, tint = CoresRc.Texto, modifier = Modifier.size(24.dp))
        }
        HorizontalDivider(color = CoresRc.Divisor)
    }
}
