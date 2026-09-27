package com.investimentoeasy.core.designsystem

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** Ícones de traço do protótipo (viewBox 24, traço 1.75), com os mesmos caminhos SVG. */
public object Icones {
    public val carteira: ImageVector = icone("carteira", "M3 8h18v12H3zM16 14h2M5 8l3-4h10l1 4")
    public val analise: ImageVector = icone("analise", "M5 20V11M11 20V5M17 20v-6M3 20h18")
    public val alertas: ImageVector = icone("alertas", "M6 16v-5a6 6 0 0 1 12 0v5l2 2H4zM10 21h4")
    public val enviar: ImageVector = icone("enviar", "M12 16V4M7 9l5-5 5 5M4 20h16")
    public val arquivo: ImageVector = icone("arquivo", "M14 3H6v18h12V7zM14 3v4h4M12 17v-6M9.5 13.5 12 11l2.5 2.5", traco = 1.5f)

    private fun icone(
        nome: String,
        caminho: String,
        traco: Float = 1.75f,
    ): ImageVector =
        ImageVector
            .Builder(name = nome, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .addPath(
                pathData = PathParser().parsePathString(caminho).toNodes(),
                stroke = SolidColor(androidx.compose.ui.graphics.Color.Black),
                strokeLineWidth = traco,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ).build()
}
