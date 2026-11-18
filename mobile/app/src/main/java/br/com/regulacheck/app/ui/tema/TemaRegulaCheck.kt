package br.com.regulacheck.app.ui.tema

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/** Esquema Material 3 a partir dos tokens [CoresRc]. Só modo claro (modo escuro fora do épico, #40). */
private val EsquemaCores = lightColorScheme(
    primary = CoresRc.Acao,
    onPrimary = CoresRc.NaAcao,
    secondary = CoresRc.Acao,
    onSecondary = CoresRc.NaAcao,
    tertiary = CoresRc.Acao,
    onTertiary = CoresRc.NaAcao,
    background = CoresRc.Fundo,
    onBackground = CoresRc.Texto,
    surface = CoresRc.Fundo,
    onSurface = CoresRc.Texto,
    surfaceVariant = CoresRc.Superficie,
    onSurfaceVariant = CoresRc.TextoApoio,
    surfaceContainer = CoresRc.Superficie,
    outline = CoresRc.Borda,
    outlineVariant = CoresRc.Divisor,
    error = CoresRc.Erro,
    onError = CoresRc.NaAcao,
    errorContainer = CoresRc.FundoErro,
    onErrorContainer = CoresRc.Texto,
    inverseSurface = CoresRc.Texto,
    inverseOnSurface = CoresRc.NaAcao,
    inversePrimary = CoresRc.FundoInfo,
)

/** Tema do app: cores e tipografia do design system v1.0 em Material 3. */
@Composable
fun TemaRegulaCheck(conteudo: @Composable () -> Unit) {
    MaterialTheme(colorScheme = EsquemaCores, typography = TipografiaMaterial, content = conteudo)
}
