@file:OptIn(ExperimentalTextApi::class)

package br.com.regulacheck.app.ui.tema

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import br.com.regulacheck.app.R

/*
 * Tipografia do design system v1.0: Bricolage Grotesque 600 só em títulos, Nunito Sans no resto.
 * As fontes são variáveis (res/font, licença OFL em mobile/app/licencas/). O desenho (pen.dev) usa
 * a Bricolage no tamanho óptico 96 em todos os títulos: conferido no emulador, "Entrar" mede 84 dp
 * com opsz 96, como na prévia KulXG; com opsz 32 ficava cerca de 10% mais largo.
 */

private val Bricolage = FontFamily(
    Font(
        R.font.bricolage_grotesque,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(600),
            FontVariation.Setting("opsz", 96f),
        ),
    ),
)

private val NunitoSans = FontFamily(
    Font(R.font.nunito_sans, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.nunito_sans, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    Font(R.font.nunito_sans, FontWeight.ExtraBold, variationSettings = FontVariation.Settings(FontVariation.weight(800))),
)

/** Papéis tipográficos (tokens `rc-size-*`), na ordem da tabela do design system. */
object TiposRc {
    /** Título de tela, 32/1,15 (`rc-size-display`). */
    val Display = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 36.8.sp)

    /** Título de seção ou item em destaque, 22/1,2 (`rc-size-heading`); também a marca. */
    val Secao = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 26.4.sp)

    /** Título de item de lista, 18/1,25 (`rc-size-title`). */
    val TituloItem = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 22.5.sp)

    /** Abertura, 18/1,4 (`rc-size-lead`). */
    val Abertura = TextStyle(fontFamily = NunitoSans, fontWeight = FontWeight.Normal, fontSize = 18.sp, lineHeight = 25.2.sp)

    /** Corpo, 16/1,5 (`rc-size-body`). */
    val Corpo = TextStyle(fontFamily = NunitoSans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp)

    /** Ação e ênfase, 16/1,4 negrito. */
    val Acao = TextStyle(fontFamily = NunitoSans, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.4.sp)

    /** Metadado, 14/1,4 (`rc-size-meta`). */
    val Meta = TextStyle(fontFamily = NunitoSans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 19.6.sp)

    /** Estado e rótulo de campo, 14/1,4 negrito. */
    val MetaForte = Meta.copy(fontWeight = FontWeight.Bold)

    /** Legenda, 13/1,4 (`rc-size-caption`). */
    val Legenda = TextStyle(fontFamily = NunitoSans, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.2.sp)

    /** Selo, 12 caixa-alta +0,6 (`rc-size-badge`); usado pelo selo SIMULADO (RC-19). */
    val Selo = TextStyle(fontFamily = NunitoSans, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, letterSpacing = 0.05.em)
}

/** Mapeamento para os papéis do Material 3 usados pelos componentes padrão. */
internal val TipografiaMaterial = Typography(
    displaySmall = TiposRc.Display,
    headlineSmall = TiposRc.Secao,
    titleLarge = TiposRc.Secao,
    titleMedium = TiposRc.TituloItem,
    bodyLarge = TiposRc.Corpo,
    bodyMedium = TiposRc.Meta,
    bodySmall = TiposRc.Legenda,
    labelLarge = TiposRc.Acao,
    labelMedium = TiposRc.MetaForte,
    labelSmall = TiposRc.Selo,
)
