package br.com.regulacheck.app.ui.pedido

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
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
import br.com.regulacheck.app.ui.componentes.BotaoPrimario
import br.com.regulacheck.app.ui.componentes.CabecalhoVoltar
import br.com.regulacheck.app.ui.componentes.Esqueleto
import br.com.regulacheck.app.ui.componentes.EstruturaTela
import br.com.regulacheck.app.ui.componentes.FORMA_SUPERFICIE
import br.com.regulacheck.app.ui.componentes.RotuloEstado
import br.com.regulacheck.app.ui.componentes.TipoRetorno
import br.com.regulacheck.app.ui.componentes.TituloTela
import br.com.regulacheck.app.ui.formatarDataHora
import br.com.regulacheck.app.ui.navegacao.EstadoTela
import br.com.regulacheck.app.ui.pedidos.avisarEtapaIndisponivel
import br.com.regulacheck.app.ui.rotuloResultadoGeral
import br.com.regulacheck.app.ui.tema.CoresRc
import br.com.regulacheck.app.ui.tema.TiposRc
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Carrega um pedido próprio (`GET /solicitacoes/{id}`). */
class PedidoViewModel(private val api: RegulaCheckApi, private val id: String) : ViewModel() {

    private val estadoMutavel = MutableStateFlow<EstadoTela<Solicitacao>>(EstadoTela.Carregando)
    val estado: StateFlow<EstadoTela<Solicitacao>> = estadoMutavel.asStateFlow()
    private var carga: Job? = null

    init {
        carregar()
    }

    /** Consulta (ou "Atualizar" em verificação) o pedido; ignorada se já houver consulta em andamento. */
    fun carregar() {
        if (carga?.isActive == true) return
        estadoMutavel.value = EstadoTela.Carregando
        carga = viewModelScope.launch {
            estadoMutavel.value = when (val resultado = api.obterSolicitacao(id)) {
                is Resultado.Sucesso -> EstadoTela.Sucesso(resultado.valor)
                is Resultado.Falha -> EstadoTela.Falha(resultado.erro)
            }
        }
    }
}

/**
 * E1.3 Pedido: dados persistidos e a ação principal por estado (`data-model.md`). As ações que
 * dependem da RC-19 avisam que a etapa ainda não está disponível.
 */
@Composable
fun PedidoTela(viewModel: PedidoViewModel, aoVoltar: () -> Unit) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val avisos = remember { SnackbarHostState() }
    val escopo = rememberCoroutineScope()

    EstruturaTela(avisos = avisos, cabecalho = { CabecalhoVoltar(aoVoltar) }) {
        when (val atual = estado) {
            EstadoTela.Carregando, EstadoTela.Vazio -> {
                Spacer(Modifier.height(32.dp))
                Esqueleto(Textos.PEDIDO_CARREGANDO)
            }
            is EstadoTela.Falha -> FalhaPedido(atual.erro, aoVoltar, viewModel::carregar)
            is EstadoTela.Sucesso -> DetalhePedido(atual.dados) { acao ->
                if (acao == AcaoPedido.ATUALIZAR) viewModel.carregar() else avisarEtapaIndisponivel(escopo, avisos)
            }
        }
    }
}

@Composable
private fun FalhaPedido(erro: ErroApi, aoVoltar: () -> Unit, aoTentar: () -> Unit) {
    Spacer(Modifier.height(32.dp))
    when (erro) {
        // 404: inexistente ou de outra paciente (mesma resposta); a ação leva de volta à lista.
        ErroApi.NaoEncontrado -> {
            Text(
                Textos.NAO_ENCONTRADO_TITULO,
                style = TiposRc.Secao,
                color = CoresRc.Texto,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(12.dp))
            Text(Textos.NAO_ENCONTRADO_TEXTO, style = TiposRc.Corpo, color = CoresRc.TextoApoio)
            Spacer(Modifier.height(24.dp))
            BotaoPrimario(Textos.VOLTAR_PARA_PEDIDOS, aoVoltar)
        }
        ErroApi.SemPermissao -> {
            BlocoRetorno(TipoRetorno.SEM_PERMISSAO, Textos.SEM_PERMISSAO)
            Spacer(Modifier.height(24.dp))
            BotaoPrimario(Textos.VOLTAR, aoVoltar)
        }
        ErroApi.SemConexao, ErroApi.TempoEsgotado ->
            BlocoRetorno(TipoRetorno.SEM_CONEXAO, Textos.SEM_CONEXAO_TITULO, Textos.SEM_CONEXAO_TEXTO, aoTentar = aoTentar)
        ErroApi.SessaoExpirada -> Unit
        else -> BlocoRetorno(TipoRetorno.ERRO, Textos.INDISPONIVEL_TITULO, Textos.DETALHE_INDISPONIVEL_TEXTO, aoTentar = aoTentar)
    }
}

@Composable
private fun DetalhePedido(pedido: Solicitacao, aoAgir: (AcaoPedido) -> Unit) {
    val apresentacao = apresentarEstado(EstadoPedido.daApi(pedido.estado), pedido.documentosDisponiveis)
    TituloTela(
        Textos.tituloPedido(pedido.id),
        abaixo = null,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        "${pedido.servico.especialidade} · ${pedido.servico.localidade}",
        style = TiposRc.Corpo,
        color = CoresRc.TextoApoio,
    )
    Spacer(Modifier.height(24.dp))

    // Pedido/Próximo passo
    Column(
        Modifier
            .fillMaxWidth()
            .background(CoresRc.Superficie, FORMA_SUPERFICIE)
            .padding(20.dp),
    ) {
        Text(Textos.PROXIMO_PASSO, style = TiposRc.MetaForte, color = CoresRc.TextoApoio)
        Spacer(Modifier.height(12.dp))
        RotuloEstado(apresentacao.rotulo, apresentacao.sinal)
        Spacer(Modifier.height(12.dp))
        Text(apresentacao.mensagem, style = TiposRc.Corpo, color = CoresRc.Texto)
        apresentacao.acao?.let { acao ->
            Spacer(Modifier.height(20.dp))
            BotaoPrimario(acao.rotulo, { aoAgir(acao) })
        }
    }
    Spacer(Modifier.height(16.dp))

    LinhaDado(Textos.DOCUMENTOS, Textos.documentosAnexados(pedido.documentosDisponiveis))
    rotuloResultadoGeral(pedido.analise?.resultadoGeral)?.let { resultado ->
        LinhaDado(Textos.RESULTADO, resultado)
    }

    Spacer(Modifier.height(24.dp))
    Text(
        Textos.metadados(formatarDataHora(pedido.criadaEm), formatarDataHora(pedido.atualizadaEm), pedido.revisao),
        style = TiposRc.Meta,
        color = CoresRc.TextoApoio,
    )
    pedido.servico.avisoFicticio?.takeIf { it.isNotBlank() }?.let { aviso ->
        Spacer(Modifier.height(8.dp))
        // Texto do backend exibido como texto puro.
        Text(aviso, style = TiposRc.Meta, color = CoresRc.TextoApoio)
    }
}

/** `Dado/Linha`: rótulo e valor, sem navegação nesta história (as telas de destino são da RC-19). */
@Composable
private fun LinhaDado(rotulo: String, valor: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
    ) {
        Column(Modifier.padding(vertical = 16.dp)) {
            Text(rotulo, style = TiposRc.Acao, color = CoresRc.Texto)
            Spacer(Modifier.height(4.dp))
            Text(valor, style = TiposRc.Corpo, color = CoresRc.TextoApoio)
        }
        HorizontalDivider(color = CoresRc.Divisor)
    }
}
