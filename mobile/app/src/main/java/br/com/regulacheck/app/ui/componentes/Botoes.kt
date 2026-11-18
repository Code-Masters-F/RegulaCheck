package br.com.regulacheck.app.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import br.com.regulacheck.app.ui.tema.CoresRc
import br.com.regulacheck.app.ui.tema.IconesRc
import br.com.regulacheck.app.ui.tema.TiposRc

/** Altura dos controles (`rc-control-height`). */
private val ALTURA_CONTROLE = 52.dp
private val ESPACO_INTERNO = PaddingValues(horizontal = 24.dp, vertical = 12.dp)

/**
 * `Botão/Primário` (cápsula, largura total). Com [carregando], mostra o indicador e ignora toques,
 * o que impede envio repetido (`Botão/Carregando`).
 */
@Composable
fun BotaoPrimario(
    texto: String,
    aoTocar: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    carregando: Boolean = false,
) {
    Button(
        onClick = { if (!carregando) aoTocar() },
        enabled = habilitado || carregando,
        shape = CircleShape,
        contentPadding = ESPACO_INTERNO,
        colors = ButtonDefaults.buttonColors(
            containerColor = CoresRc.Acao,
            contentColor = CoresRc.NaAcao,
            disabledContainerColor = CoresRc.Desabilitado,
            disabledContentColor = CoresRc.TextoApoio,
        ),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ALTURA_CONTROLE),
    ) {
        if (carregando) {
            CircularProgressIndicator(
                color = CoresRc.NaAcao,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
        }
        Text(texto, style = TiposRc.Acao)
    }
}

/** `Botão/Contorno` com ícone opcional (ex.: "Tentar de novo" com seta circular). */
@Composable
fun BotaoContorno(
    texto: String,
    aoTocar: () -> Unit,
    modifier: Modifier = Modifier,
    icone: ImageVector? = null,
    habilitado: Boolean = true,
) {
    OutlinedButton(
        onClick = aoTocar,
        enabled = habilitado,
        shape = CircleShape,
        contentPadding = ESPACO_INTERNO,
        border = BorderStroke(1.5.dp, if (habilitado) CoresRc.Acao else CoresRc.Borda),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = CoresRc.Fundo, contentColor = CoresRc.Acao),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ALTURA_CONTROLE),
    ) {
        if (icone != null) {
            Icon(icone, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
        }
        Text(texto, style = TiposRc.Acao)
    }
}

/** `Botão/Adicionar`: ação de apoio em texto com "+" (ex.: "Novo pedido"). */
@Composable
fun BotaoAdicionar(texto: String, aoTocar: () -> Unit) {
    TextButton(
        onClick = aoTocar,
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 12.dp),
        modifier = Modifier.heightIn(min = 48.dp),
    ) {
        Icon(IconesRc.Mais, contentDescription = null, tint = CoresRc.Acao, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(texto, style = TiposRc.Acao, color = CoresRc.Acao)
    }
}
