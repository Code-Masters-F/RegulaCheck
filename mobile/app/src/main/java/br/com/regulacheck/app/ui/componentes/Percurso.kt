package br.com.regulacheck.app.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import br.com.regulacheck.app.ui.tema.CoresRc
import br.com.regulacheck.app.ui.tema.IconesRc
import br.com.regulacheck.app.ui.tema.TiposRc

/*
 * Componentes do percurso da RC-19 (F2 Percurso e F3 Confiança do design system): selo SIMULADO,
 * opção de escolha, superfície da tarefa e título de seção.
 */

/** Texto do selo; o leitor de tela lê a palavra, sem depender da cor. */
private const val SIMULADO = "SIMULADO"

/** `Selo/Simulado`: cápsula com `rc-simulated-fg/-bg` (6,82:1), Nunito 800 12 caixa-alta. */
@Composable
fun SeloSimulado(modifier: Modifier = Modifier) {
    Text(
        SIMULADO,
        style = TiposRc.Selo,
        color = CoresRc.Simulado,
        modifier = modifier
            .background(CoresRc.FundoSimulado, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 5.dp),
    )
}

/**
 * `Opção/Selecionada` e `Opção/Não selecionada`: rádio com título e apoio, linha inteira tocável
 * (alvo ≥ 48 dp) e lida como uma opção pelo leitor de tela.
 */
@Composable
fun OpcaoEscolha(
    titulo: String,
    apoio: String?,
    selecionada: Boolean,
    aoEscolher: () -> Unit,
    habilitada: Boolean = true,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .selectable(selected = selecionada, enabled = habilitada, role = Role.RadioButton, onClick = aoEscolher)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selecionada,
            onClick = null,
            enabled = habilitada,
            colors = RadioButtonDefaults.colors(selectedColor = CoresRc.Acao, unselectedColor = CoresRc.Borda),
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(titulo, style = TiposRc.Acao, color = CoresRc.Texto)
            if (apoio != null) {
                Spacer(Modifier.height(4.dp))
                Text(apoio, style = TiposRc.Meta, color = CoresRc.TextoApoio)
            }
        }
    }
}

/** Superfície da tarefa (raio 24, padding 20, fundo neutro) com título opcional. */
@Composable
fun SuperficieTarefa(
    titulo: String? = null,
    fundo: Color = CoresRc.Superficie,
    acaoTitulo: (@Composable () -> Unit)? = null,
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(fundo, FORMA_SUPERFICIE)
            .padding(20.dp),
    ) {
        if (titulo != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    titulo,
                    style = TiposRc.TituloItem,
                    color = CoresRc.Texto,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() },
                )
                acaoTitulo?.invoke()
            }
            Spacer(Modifier.height(12.dp))
        }
        conteudo()
    }
}

/** Título de seção (ex.: "O que falta", "Outros critérios"), marcado como cabeçalho. */
@Composable
fun TituloSecao(texto: String) {
    Text(
        texto,
        style = TiposRc.TituloItem,
        color = CoresRc.Texto,
        modifier = Modifier
            .padding(top = 24.dp, bottom = 8.dp)
            .semantics { heading() },
    )
}

/** `Linha/Navegação` em texto com seta (ex.: "Voltar para meus pedidos →"), alvo ≥ 48 dp. */
@Composable
fun BotaoSeta(texto: String, aoTocar: () -> Unit) {
    TextButton(onClick = aoTocar, modifier = Modifier.heightIn(min = 48.dp)) {
        Text(texto, style = TiposRc.Acao, color = CoresRc.Acao)
        Spacer(Modifier.width(8.dp))
        Icon(IconesRc.SetaDireita, contentDescription = null, tint = CoresRc.Acao, modifier = Modifier.size(20.dp))
    }
}

/** Ícone 16 + texto curto lidos juntos (estado do envio, resultado por regra): nunca só cor. */
@Composable
fun SinalTexto(icone: ImageVector, cor: Color, texto: String, modifier: Modifier = Modifier) {
    Row(
        modifier.semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icone, contentDescription = null, tint = cor, modifier = Modifier.size(16.dp))
        Text(texto, style = TiposRc.MetaForte, color = CoresRc.Texto)
    }
}
