package br.com.regulacheck.app.ui.captura

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import br.com.regulacheck.app.api.ErroApi
import br.com.regulacheck.app.ui.Textos
import br.com.regulacheck.app.ui.componentes.BlocoRetorno
import br.com.regulacheck.app.ui.componentes.BotaoContorno
import br.com.regulacheck.app.ui.componentes.BotaoPrimario
import br.com.regulacheck.app.ui.componentes.CabecalhoVoltar
import br.com.regulacheck.app.ui.componentes.Esqueleto
import br.com.regulacheck.app.ui.componentes.EstruturaTela
import br.com.regulacheck.app.ui.componentes.FORMA_SUPERFICIE
import br.com.regulacheck.app.ui.componentes.SinalTexto
import br.com.regulacheck.app.ui.componentes.TipoRetorno
import br.com.regulacheck.app.ui.componentes.TituloTela
import br.com.regulacheck.app.ui.tema.CoresRc
import br.com.regulacheck.app.ui.tema.IconesRc
import br.com.regulacheck.app.ui.tema.TiposRc

/**
 * E4.1 Condições da captura (quadros o3kkGI, fOrmB, NyDDm, DGf2T, Cna25, kaHJz): os dois sensores
 * SIMULADOS, a orientação da API e a idade da leitura, antes de a paciente escolher a foto.
 *
 * Enquanto a tela está visível (ciclo de vida `STARTED`), [CondicoesCapturaViewModel.acompanhar]
 * consulta a telemetria; sair da tela ou abrir o seletor cancela a consulta. Em todos os estados de
 * telemetria as duas ações ficam disponíveis: sensor orienta, nunca bloqueia (CLAUDE.md §12). O app
 * não abre a câmera: "Escolher a foto" abre o seletor do sistema com imagens (research D2).
 *
 * @param aoEscolherFoto seletor do sistema com JPEG/PNG e o tipo já escolhido.
 * @param aoEscolherArquivo seletor do sistema com PDF/JPEG/PNG.
 * @param aoPedidoFechado volta aos documentos depois do 409 (pedido submetido) e os recarrega.
 */
@Composable
fun CondicoesCapturaTela(
    viewModel: CondicoesCapturaViewModel,
    pedidoId: String,
    aoVoltar: () -> Unit,
    aoEscolherFoto: () -> Unit,
    aoEscolherArquivo: () -> Unit,
    aoPedidoFechado: () -> Unit,
    aoVoltarParaPedidos: () -> Unit,
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val avisos = remember { SnackbarHostState() }
    val ciclo = LocalLifecycleOwner.current

    LaunchedEffect(viewModel, ciclo) {
        ciclo.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.acompanhar() }
    }

    EstruturaTela(
        avisos = avisos,
        cabecalho = { CabecalhoVoltar(aoVoltar, titulo = Textos.pedidoNumero(pedidoId), simulado = true) },
    ) {
        TituloTela(Textos.CAPTURA_TITULO, abaixo = Textos.CAPTURA_APOIO)
        Spacer(Modifier.height(24.dp))
        when (val atual = estado) {
            EstadoCaptura.Carregando -> {
                Esqueleto(Textos.CAPTURA_CARREGANDO, linhas = 2)
                Acoes(mesmoAssim = false, aoEscolherFoto, aoEscolherArquivo)
            }
            is EstadoCaptura.Exibindo -> {
                atual.apresentacao.sensores.forEachIndexed { indice, sensor ->
                    if (indice > 0) Spacer(Modifier.height(12.dp))
                    CartaoSensor(sensor)
                }
                Spacer(Modifier.height(16.dp))
                Text(Textos.NOTA_DEMONSTRACAO, style = TiposRc.Meta, color = CoresRc.TextoApoio)
                Acoes(atual.apresentacao.mesmoAssim, aoEscolherFoto, aoEscolherArquivo)
            }
            is EstadoCaptura.Encerrada -> Encerramento(atual.erro, aoVoltar, aoPedidoFechado, aoVoltarParaPedidos)
        }
    }
}

/** Ação principal e "Escolher arquivo", presentes em todo estado de telemetria (FR-010). */
@Composable
private fun Acoes(mesmoAssim: Boolean, aoEscolherFoto: () -> Unit, aoEscolherArquivo: () -> Unit) {
    Spacer(Modifier.height(32.dp))
    BotaoPrimario(if (mesmoAssim) Textos.ESCOLHER_FOTO_MESMO_ASSIM else Textos.ESCOLHER_FOTO, aoEscolherFoto)
    Spacer(Modifier.height(12.dp))
    BotaoContorno(Textos.ESCOLHER_ARQUIVO, aoEscolherArquivo)
}

/**
 * `Sensor/Na faixa`, `Sensor/Fora da faixa` e `Sensor/Indisponível`. A situação e a orientação
 * formam uma região anunciada ao leitor de tela; a idade fica fora dela para não ser anunciada a
 * cada atualização.
 */
@Composable
private fun CartaoSensor(sensor: ApresentacaoSensor) {
    val (icone, cor) = sensor.situacao.sinal()
    Column(
        Modifier
            .fillMaxWidth()
            .background(CoresRc.Superficie, FORMA_SUPERFICIE)
            .padding(20.dp),
    ) {
        Row(
            Modifier.semantics(mergeDescendants = true) { heading() },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(sensor.nome, style = TiposRc.TituloItem, color = CoresRc.Texto, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            Text(sensor.valor, style = TiposRc.TituloItem, color = CoresRc.Texto)
        }
        Spacer(Modifier.height(12.dp))
        Column(Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
            SinalTexto(icone, cor, sensor.rotulo)
            if (sensor.orientacao != null) {
                Spacer(Modifier.height(8.dp))
                // Texto da API, exibido como texto puro.
                Text(sensor.orientacao, style = TiposRc.Corpo, color = CoresRc.Texto)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(sensor.linhaLeitura, style = TiposRc.Meta, color = CoresRc.TextoApoio)
    }
}

/** Ícone e cor de cada situação: a cor reforça, o rótulo carrega o significado. */
private fun SituacaoSensorUi.sinal(): Pair<ImageVector, Color> = when (this) {
    SituacaoSensorUi.ADEQUADA, SituacaoSensorUi.ADEQUADA_DE_NOVO -> IconesRc.Atende to CoresRc.Sucesso
    SituacaoSensorUi.BAIXA, SituacaoSensorUi.ALTA, SituacaoSensorUi.FORA_DO_IDEAL -> IconesRc.Alerta to CoresRc.Pendencia
    SituacaoSensorUi.DESATUALIZADA -> IconesRc.Relogio to CoresRc.Pendencia
    SituacaoSensorUi.SEM_LEITURA, SituacaoSensorUi.INDISPONIVEL -> IconesRc.NaoSeAplica to CoresRc.TextoApoio
}

/** 401 (o app já volta ao login), 403, 404 e 409: explicação e caminho de volta, sem consultar mais. */
@Composable
private fun Encerramento(
    erro: ErroApi,
    aoVoltar: () -> Unit,
    aoPedidoFechado: () -> Unit,
    aoVoltarParaPedidos: () -> Unit,
) {
    when (erro) {
        ErroApi.SessaoExpirada -> Unit
        ErroApi.NaoEncontrado -> {
            Text(Textos.NAO_ENCONTRADO_TITULO, style = TiposRc.Secao, color = CoresRc.Texto, modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(12.dp))
            Text(Textos.NAO_ENCONTRADO_TEXTO, style = TiposRc.Corpo, color = CoresRc.TextoApoio)
            Spacer(Modifier.height(24.dp))
            BotaoPrimario(Textos.VOLTAR_PARA_PEDIDOS, aoVoltarParaPedidos)
        }
        ErroApi.SemPermissao ->
            BlocoRetorno(TipoRetorno.SEM_PERMISSAO, Textos.SEM_PERMISSAO, rotuloAcao = Textos.VOLTAR, aoTentar = aoVoltar)
        is ErroApi.Conflito ->
            BlocoRetorno(TipoRetorno.AVISO, Textos.PEDIDO_SUBMETIDO, Textos.PEDIDO_NAO_RECEBE, rotuloAcao = Textos.VOLTAR, aoTentar = aoPedidoFechado)
        else ->
            BlocoRetorno(TipoRetorno.ERRO, Textos.INDISPONIVEL_TITULO, rotuloAcao = Textos.VOLTAR, aoTentar = aoVoltar)
    }
}
