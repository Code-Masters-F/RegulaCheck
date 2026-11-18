package br.com.regulacheck.app.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import br.com.regulacheck.app.ui.Textos
import br.com.regulacheck.app.ui.tema.CoresRc
import br.com.regulacheck.app.ui.tema.IconesRc
import br.com.regulacheck.app.ui.tema.TiposRc

private val FORMA_CAMPO = RoundedCornerShape(16.dp)

/**
 * `Campo/Padrão` com rótulo visível acima (nunca só texto dentro do campo), borda 1,5 e altura 52.
 * Com [mensagemErro], a borda fica vermelha e a mensagem aparece abaixo com ícone (nunca só cor).
 */
@Composable
fun CampoTexto(
    valor: String,
    aoMudar: (String) -> Unit,
    rotulo: String,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    mensagemErro: String? = null,
    teclado: KeyboardOptions = KeyboardOptions.Default,
    acoesTeclado: KeyboardActions = KeyboardActions.Default,
    transformacao: VisualTransformation = VisualTransformation.None,
    iconeFinal: (@Composable () -> Unit)? = null,
) {
    val interacao = remember { MutableInteractionSource() }
    val focado by interacao.collectIsFocusedAsState()
    val corBorda = when {
        mensagemErro != null -> CoresRc.Erro
        focado -> CoresRc.Acao
        else -> CoresRc.Borda
    }
    Column(modifier.fillMaxWidth()) {
        Text(rotulo, style = TiposRc.MetaForte, color = CoresRc.Texto)
        Spacer(Modifier.height(8.dp))
        BasicTextField(
            value = valor,
            onValueChange = aoMudar,
            enabled = habilitado,
            singleLine = true,
            textStyle = TiposRc.Corpo.copy(color = if (habilitado) CoresRc.Texto else CoresRc.TextoApoio),
            cursorBrush = SolidColor(CoresRc.Acao),
            keyboardOptions = teclado,
            keyboardActions = acoesTeclado,
            visualTransformation = transformacao,
            interactionSource = interacao,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = rotulo
                    if (mensagemErro != null) error(mensagemErro)
                },
            decorationBox = { campo ->
                Row(
                    Modifier
                        .heightIn(min = 52.dp)
                        .background(if (habilitado) CoresRc.Fundo else CoresRc.Desabilitado, FORMA_CAMPO)
                        .border(if (focado || mensagemErro != null) 2.dp else 1.5.dp, corBorda, FORMA_CAMPO)
                        .padding(start = 16.dp, end = if (iconeFinal != null) 4.dp else 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f)) { campo() }
                    iconeFinal?.invoke()
                }
            },
        )
        if (mensagemErro != null) {
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            ) {
                Icon(IconesRc.Alerta, contentDescription = null, tint = CoresRc.Erro, modifier = Modifier.size(16.dp))
                Text(mensagemErro, style = TiposRc.Meta, color = CoresRc.Erro)
            }
        }
    }
}

/**
 * `Campo/Senha`: oculta por padrão; o ícone de olho (alvo 48 × 48) mostra ou oculta. A senha nunca
 * é exibida sem esse pedido explícito (#41).
 */
@Composable
fun CampoSenha(
    valor: String,
    aoMudar: (String) -> Unit,
    visivel: Boolean,
    aoAlternarVisibilidade: () -> Unit,
    habilitado: Boolean,
    mensagemErro: String?,
    teclado: KeyboardOptions,
    acoesTeclado: KeyboardActions,
) {
    CampoTexto(
        valor = valor,
        aoMudar = aoMudar,
        rotulo = Textos.CAMPO_SENHA,
        habilitado = habilitado,
        mensagemErro = mensagemErro,
        teclado = teclado,
        acoesTeclado = acoesTeclado,
        transformacao = if (visivel) VisualTransformation.None else PasswordVisualTransformation(),
        iconeFinal = {
            IconButton(onClick = aoAlternarVisibilidade, enabled = habilitado, modifier = Modifier.size(48.dp)) {
                Icon(
                    if (visivel) IconesRc.OlhoFechado else IconesRc.Olho,
                    contentDescription = if (visivel) Textos.OCULTAR_SENHA else Textos.MOSTRAR_SENHA,
                    tint = CoresRc.Texto,
                )
            }
        },
    )
}
