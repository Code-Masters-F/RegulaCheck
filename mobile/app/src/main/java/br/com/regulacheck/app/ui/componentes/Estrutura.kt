package br.com.regulacheck.app.ui.componentes

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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

/** `Aviso/Rodapé`: "Protótipo acadêmico · dados fictícios". */
@Composable
fun AvisoRodape() {
    Text(
        Textos.AVISO_RODAPE,
        style = TiposRc.Legenda,
        color = CoresRc.TextoApoio,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .background(CoresRc.Fundo)
            .navigationBarsPadding()
            .padding(vertical = 16.dp),
    )
}

/** `Aviso/Entrada` (login): aviso acadêmico completo em superfície informativa. */
@Composable
fun AvisoEntrada(modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .background(CoresRc.FundoInfo, FORMA_SUPERFICIE)
            .padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(IconesRc.Info, contentDescription = null, tint = CoresRc.Texto, modifier = Modifier.size(24.dp))
        Text(Textos.AVISO_ENTRADA, style = TiposRc.Corpo, color = CoresRc.Texto)
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
