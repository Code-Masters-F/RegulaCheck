package br.com.regulacheck.app.ui.tema

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/*
 * Ícones de traço do desenho (Lucide, licença ISC, https://lucide.dev), recriados a partir dos
 * caminhos SVG na grade 24 × 24 com traço 2. Só os usados pela RC-18 (research D8). A cor vem do
 * `tint` de quem usa; o significado sempre acompanha um texto visível.
 */

private fun circulo(cx: Float, cy: Float, r: Float): String =
    "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0"

private fun icone(nome: String, vararg caminhos: String): ImageVector =
    ImageVector.Builder(
        name = nome,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        caminhos.forEach { caminho ->
            addPath(
                pathData = addPathNodes(caminho),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()

/** Conjunto de ícones do app. */
object IconesRc {
    private val Circulo = circulo(12f, 12f, 10f)

    val Info = icone("info", Circulo, "M12 16v-4", "M12 8h.01")
    val Alerta = icone("circle-alert", Circulo, "M12 8v4", "M12 16h.01")
    val Falha = icone("circle-x", Circulo, "m15 9-6 6", "m9 9 6 6")
    val Atende = icone("circle-check", Circulo, "m9 12 2 2 4-4")
    val Interrogacao = icone("circle-help", Circulo, "M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3", "M12 17h.01")
    val Relogio = icone("clock", Circulo, "M12 6v6l4 2")
    val Lapis = icone("pencil", "M17 3a2.85 2.83 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z", "m15 5 4 4")
    val Olho = icone("eye", "M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z", circulo(12f, 12f, 3f))
    val OlhoFechado = icone(
        "eye-off",
        "M9.88 9.88a3 3 0 1 0 4.24 4.24",
        "M10.73 5.08A10.43 10.43 0 0 1 12 5c7 0 10 7 10 7a13.16 13.16 0 0 1-1.67 2.68",
        "M6.61 6.61A13.526 13.526 0 0 0 2 12s3 7 10 7a9.74 9.74 0 0 0 5.39-1.61",
        "m2 2 20 20",
    )
    val DocumentoConcluido = icone(
        "file-check",
        "M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7Z",
        "M14 2v4a2 2 0 0 0 2 2h4",
        "m9 15 2 2 4-4",
    )
    val Cadeado = icone(
        "lock",
        "M5 11h14a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2Z",
        "M7 11V7a5 5 0 0 1 10 0v4",
    )
    val SemConexao = icone(
        "wifi-off",
        "M12 20h.01",
        "M8.5 16.429a5 5 0 0 1 7 0",
        "M5 12.859a10 10 0 0 1 5.17-2.69",
        "M19 12.859a10 10 0 0 0-2.007-1.523",
        "M2 8.82a15 15 0 0 1 4.177-2.643",
        "M22 8.82a15 15 0 0 0-11.288-3.764",
        "m2 2 20 20",
    )
    val SetaDireita = icone("chevron-right", "m9 18 6-6-6-6")
    val Voltar = icone("arrow-left", "m12 19-7-7 7-7", "M19 12H5")
    val Mais = icone("plus", "M5 12h14", "M12 5v14")
    val Recarregar = icone("rotate-ccw", "M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8", "M3 3v5h5")
}
