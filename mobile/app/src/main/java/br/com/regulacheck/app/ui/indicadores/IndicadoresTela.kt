package br.com.regulacheck.app.ui.indicadores

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.regulacheck.app.api.ErroApi
import br.com.regulacheck.app.api.Indicadores
import br.com.regulacheck.app.api.ProtocoloIndicadores
import br.com.regulacheck.app.api.RegulaCheckApi
import br.com.regulacheck.app.api.Resultado
import br.com.regulacheck.app.ui.RotulosApi
import br.com.regulacheck.app.ui.SinalEstado
import br.com.regulacheck.app.ui.Textos
import br.com.regulacheck.app.ui.componentes.BlocoRetorno
import br.com.regulacheck.app.ui.componentes.CabecalhoMarca
import br.com.regulacheck.app.ui.componentes.Esqueleto
import br.com.regulacheck.app.ui.componentes.EstruturaTela
import br.com.regulacheck.app.ui.componentes.FORMA_SUPERFICIE
import br.com.regulacheck.app.ui.componentes.RotuloEstado
import br.com.regulacheck.app.ui.componentes.TipoRetorno
import br.com.regulacheck.app.ui.componentes.TituloSecao
import br.com.regulacheck.app.ui.componentes.TituloTela
import br.com.regulacheck.app.ui.navegacao.EstadoTela
import br.com.regulacheck.app.ui.tema.CoresRc
import br.com.regulacheck.app.ui.tema.IconesRc
import br.com.regulacheck.app.ui.tema.TiposRc
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Carrega os indicadores agregados (`GET /admin/indicadores`, RC-20). Uma consulta por pedido
 * explícito (entrada na tela ou "Tentar de novo"): nunca repete sozinho (#40).
 */
class IndicadoresViewModel(private val api: RegulaCheckApi) : ViewModel() {

    private val estadoMutavel = MutableStateFlow<EstadoTela<Indicadores>>(EstadoTela.Carregando)
    val estado: StateFlow<EstadoTela<Indicadores>> = estadoMutavel.asStateFlow()
    private var carga: Job? = null

    /**
     * Consulta os indicadores. Com dados já exibidos, atualiza sem voltar ao esqueleto; ignorada
     * enquanto outra consulta está em andamento (toques repetidos).
     *
     * @return o trabalho da consulta (os testes aguardam; a tela não precisa).
     */
    fun carregar(): Job? {
        if (carga?.isActive == true) return carga
        if (estadoMutavel.value !is EstadoTela.Sucesso) estadoMutavel.value = EstadoTela.Carregando
        carga = viewModelScope.launch {
            estadoMutavel.value = when (val resultado = api.consultarIndicadores()) {
                is Resultado.Sucesso -> EstadoTela.Sucesso(resultado.valor)
                is Resultado.Falha -> EstadoTela.Falha(resultado.erro)
            }
        }
        return carga
    }
}

/**
 * E4.2 Protocolo e indicadores (quadros T8XMZ, Ifg71, mcN6S): protocolo ativo com as regras, total
 * de pedidos, análises por resultado e regras com mais pendências. Só agregados de demonstração; o
 * administrador não tem outra tela neste app.
 *
 * @param aoSair encerra a sessão (a local é apagada antes da resposta do servidor).
 */
@Composable
fun IndicadoresTela(viewModel: IndicadoresViewModel, aoSair: () -> Unit) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val avisos = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.carregar() }

    EstruturaTela(avisos = avisos, cabecalho = { CabecalhoMarca(aoSair = aoSair) }) {
        TituloTela(Textos.INDICADORES_TITULO)
        Spacer(Modifier.height(24.dp))
        when (val atual = estado) {
            EstadoTela.Carregando, EstadoTela.Vazio -> Esqueleto(Textos.INDICADORES_CARREGANDO, linhas = 4)
            is EstadoTela.Falha -> FalhaIndicadores(atual.erro) { viewModel.carregar() }
            is EstadoTela.Sucesso -> ConteudoIndicadores(atual.dados)
        }
    }
}

@Composable
private fun FalhaIndicadores(erro: ErroApi, aoTentar: () -> Unit) {
    when (erro) {
        // 401: o cliente já encerrou a sessão e a navegação volta ao login; nada a mostrar aqui.
        ErroApi.SessaoExpirada -> Unit
        ErroApi.SemPermissao -> BlocoRetorno(TipoRetorno.SEM_PERMISSAO, Textos.SEM_PERMISSAO)
        ErroApi.SemConexao, ErroApi.TempoEsgotado ->
            BlocoRetorno(TipoRetorno.SEM_CONEXAO, Textos.SEM_CONEXAO_TITULO, Textos.SEM_CONEXAO_TEXTO, aoTentar = aoTentar)
        else -> BlocoRetorno(TipoRetorno.ERRO, Textos.INDICADORES_ERRO, aoTentar = aoTentar)
    }
}

@Composable
private fun ConteudoIndicadores(dados: Indicadores) {
    CartaoProtocolo(dados.protocolosAtivos)

    TituloSecao(Textos.TOTAL_PEDIDOS)
    Text(dados.totalPedidos.toString(), style = TiposRc.Display, color = CoresRc.Texto)

    TituloSecao(Textos.ANALISES_POR_RESULTADO)
    val grupos = dados.analisesPorResultado
    LinhaResultado(Textos.RESULTADO_SEM_PENDENCIAS, SinalEstado.SEM_PENDENCIA, grupos.semPendenciasIdentificadas)
    LinhaResultado(Textos.RESULTADO_COM_PENDENCIAS, SinalEstado.PENDENCIA, grupos.comPendencias)
    LinhaResultado(Textos.RESULTADO_CONFIRA, SinalEstado.CONFERIR, grupos.requerConferencia)
    LinhaResultado(Textos.RESULTADO_FALHA, SinalEstado.FALHA, dados.pedidosEmFalhaAnalise)
    Spacer(Modifier.height(8.dp))
    Text(Textos.INDICADORES_BASE, style = TiposRc.Meta, color = CoresRc.TextoApoio)

    TituloSecao(Textos.REGRAS_MAIS_PENDENCIAS)
    if (dados.regrasComMaisPendencias.isEmpty()) {
        Text(Textos.SEM_PENDENCIAS_REGISTRADAS, style = TiposRc.Corpo, color = CoresRc.TextoApoio)
    } else {
        dados.regrasComMaisPendencias.forEach { regra ->
            LinhaNumero(Textos.regraLinha(regra.codigo, regra.descricao), Textos.quantidadePendencias(regra.total))
        }
    }

    Spacer(Modifier.height(24.dp))
    NotaDemonstracao()
}

/** Cartão "Protocolo ativo": versão, quantidade e lista das regras (código e nome). */
@Composable
private fun CartaoProtocolo(protocolos: List<ProtocoloIndicadores>) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(CoresRc.Superficie, FORMA_SUPERFICIE)
            .padding(20.dp),
    ) {
        Text(
            Textos.PROTOCOLO_ATIVO,
            style = TiposRc.TituloItem,
            color = CoresRc.Texto,
            modifier = Modifier.semantics { heading() },
        )
        if (protocolos.isEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(Textos.SEM_PROTOCOLO_ATIVO, style = TiposRc.Corpo, color = CoresRc.Texto)
        }
        protocolos.forEach { protocolo ->
            Spacer(Modifier.height(8.dp))
            Text(
                Textos.protocoloAtivoResumo(RotulosApi.versaoProtocolo(protocolo.versao), protocolo.quantidadeRegras),
                style = TiposRc.Corpo,
                color = CoresRc.Texto,
            )
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                protocolo.regras.forEach { regra ->
                    Text(Textos.regraLinha(regra.codigo, regra.descricao), style = TiposRc.Meta, color = CoresRc.Texto)
                }
            }
        }
    }
}

/** Grupo de resultado: rótulo com ícone (nunca só cor) e o número, lidos juntos. */
@Composable
private fun LinhaResultado(rotulo: String, sinal: SinalEstado, quantidade: Long) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(Modifier.weight(1f)) { RotuloEstado(rotulo, sinal) }
            Text(quantidade.toString(), style = TiposRc.TituloItem, color = CoresRc.Texto)
        }
        HorizontalDivider(color = CoresRc.Divisor)
    }
}

/** Linha de texto com valor à direita (regras com mais pendências). */
@Composable
private fun LinhaNumero(texto: String, valor: String) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(vertical = 8.dp)
                .semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(texto, style = TiposRc.Corpo, color = CoresRc.Texto, modifier = Modifier.weight(1f))
            Spacer(Modifier.size(12.dp))
            Text(valor, style = TiposRc.MetaForte, color = CoresRc.Texto)
        }
        HorizontalDivider(color = CoresRc.Divisor)
    }
}

/** Nota fixa de E4.2: dados agregados de demonstração, sem documentos ou pedidos individuais. */
@Composable
private fun NotaDemonstracao() {
    Row(
        Modifier
            .fillMaxWidth()
            .background(CoresRc.FundoInfo, FORMA_SUPERFICIE)
            .padding(16.dp)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(IconesRc.Info, contentDescription = null, tint = CoresRc.Texto, modifier = Modifier.size(20.dp))
        Text(Textos.INDICADORES_NOTA, style = TiposRc.Meta, color = CoresRc.Texto)
    }
}
