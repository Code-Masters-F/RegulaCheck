package br.com.regulacheck.app.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupPositionProvider
import kotlinx.coroutines.launch
import br.com.regulacheck.app.ui.Textos
import br.com.regulacheck.app.ui.tema.CoresRc
import br.com.regulacheck.app.ui.tema.IconesRc
import br.com.regulacheck.app.ui.tema.TiposRc

/** Margem lateral da tela (`rc-screen-margin`). */
val MARGEM_TELA = 24.dp

/** Superfície da tarefa: raio 24, padding 20, sem borda nem sombra (design system). */
val FORMA_SUPERFICIE = RoundedCornerShape(24.dp)

/**
 * Estrutura comum de tela: conteúdo rolável com margem 24, rodapé acadêmico fixo (F3, presente em
 * toda tela, #41) e espaço para barras do sistema e teclado.
 */
@Composable
fun EstruturaTela(
    avisos: SnackbarHostState,
    cabecalho: @Composable () -> Unit,
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        containerColor = CoresRc.Fundo,
        snackbarHost = { SnackbarHost(avisos) },
        bottomBar = { AvisoRodape() },
    ) { espacos ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(espacos)
                .consumeWindowInsets(espacos)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MARGEM_TELA),
        ) {
            Spacer(Modifier.height(16.dp))
            cabecalho()
            conteudo()
            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * `Aviso/Rodapé` + `Aviso/Completo` (F3, decisão de 17/11/2026): o rodapé discreto identifica o
 * protótipo em toda tela; tocar nele (ou toque longo) abre, logo acima, o balão com o aviso
 * acadêmico completo, e tocar fora fecha. Alvo de 48 dp e rótulo próprio para leitor de tela.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvisoRodape() {
    val balao = rememberTooltipState(isPersistent = true)
    val escopo = rememberCoroutineScope()
    Box(
        Modifier
            .fillMaxWidth()
            .background(CoresRc.Fundo)
            .navigationBarsPadding()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        val espacoBalao = with(LocalDensity.current) { 8.dp.roundToPx() }
        TooltipBox(
            positionProvider = remember(espacoBalao) { AcimaCentralizado(espacoBalao) },
            state = balao,
            tooltip = {
                RichTooltip(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(IconesRc.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(Textos.AVISO_COMPLETO_TITULO, style = TiposRc.MetaForte.copy(fontWeight = FontWeight.ExtraBold))
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    // Sem elevação (design system): o contraste do balão escuro basta.
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    colors = TooltipDefaults.richTooltipColors(
                        containerColor = CoresRc.Texto,
                        contentColor = CoresRc.NaAcao,
                        titleContentColor = CoresRc.NaAcao,
                        actionContentColor = CoresRc.NaAcao,
                    ),
                ) {
                    Text(Textos.AVISO_COMPLETO, style = TiposRc.Meta.copy(lineHeight = 21.sp))
                }
            },
        ) {
            Row(
                Modifier
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Button) { escopo.launch { balao.show() } }
                    .padding(horizontal = 12.dp)
                    .clearAndSetSemantics {
                        contentDescription = Textos.AVISO_RODAPE_LEITOR
                        role = Role.Button
                        onClick { escopo.launch { balao.show() }; true }
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(IconesRc.Info, contentDescription = null, tint = CoresRc.TextoApoio, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(Textos.AVISO_RODAPE, style = TiposRc.Legenda, color = CoresRc.TextoApoio)
            }
        }
    }
}

/** Posiciona o balão centralizado acima do rodapé, sem sair da janela. */
private class AcimaCentralizado(private val espaco: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
        val y = anchorBounds.top - popupContentSize.height - espaco
        return IntOffset(
            x.coerceIn(0, maxOf(0, windowSize.width - popupContentSize.width)),
            y.coerceAtLeast(0),
        )
    }
}

/**
 * `Aviso/Proposta` (login): o que o app faz, em duas linhas. Não substitui o aviso acadêmico, que
 * fica no rodapé.
 */
@Composable
fun AvisoProposta(modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .background(CoresRc.FundoInfo, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .background(CoresRc.Fundo, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(IconesRc.DocumentoConcluido, contentDescription = null, tint = CoresRc.Acao, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(Textos.PROPOSTA_TITULO, style = TiposRc.Acao, color = CoresRc.Texto)
            Spacer(Modifier.height(2.dp))
            Text(Textos.PROPOSTA_TEXTO, style = TiposRc.Meta, color = CoresRc.TextoApoio)
        }
    }
}

/** `Cabeçalho/Marca`: marca à esquerda e, quando há sessão, "Sair" à direita. */
@Composable
fun CabecalhoMarca(aoSair: (() -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(Textos.MARCA, style = TiposRc.Secao, color = CoresRc.Texto, modifier = Modifier.weight(1f))
        if (aoSair != null) {
            TextButton(onClick = aoSair, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(Textos.SAIR, style = TiposRc.Acao, color = CoresRc.Acao)
            }
        }
    }
}

/** `Cabeçalho/Voltar`: seta de 24 com alvo de 48 × 48 e rótulo para leitor de tela. */
@Composable
fun CabecalhoVoltar(aoVoltar: () -> Unit) {
    Box(Modifier.heightIn(min = 48.dp)) {
        IconButton(onClick = aoVoltar, modifier = Modifier.size(48.dp)) {
            Icon(IconesRc.Voltar, contentDescription = Textos.VOLTAR, tint = CoresRc.Texto)
        }
    }
}

/**
 * Bloco de título da tela: apoio acima (opcional), título 32 marcado como cabeçalho e abertura
 * abaixo (opcional).
 */
@Composable
fun TituloTela(titulo: String, acima: String? = null, abaixo: String? = null) {
    Spacer(Modifier.height(32.dp))
    if (acima != null) {
        Text(acima, style = TiposRc.Abertura, color = CoresRc.TextoApoio)
        Spacer(Modifier.height(8.dp))
    }
    Text(
        titulo,
        style = TiposRc.Display,
        color = CoresRc.Texto,
        modifier = Modifier.semantics { heading() },
    )
    if (abaixo != null) {
        Spacer(Modifier.height(8.dp))
        Text(abaixo, style = TiposRc.Abertura, color = CoresRc.TextoApoio)
    }
}
