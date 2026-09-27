package com.investimentoeasy.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** Work Sans nos títulos e números de destaque; Roboto (fonte do sistema) no corpo. */
internal val WorkSans =
    FontFamily(
        Font(R.font.work_sans_semibold, FontWeight.SemiBold),
        Font(R.font.work_sans_bold, FontWeight.Bold),
    )

private val Corpo = FontFamily.SansSerif

/** Escala tipográfica do protótipo (tamanhos 12–36, alturas de linha 1.2/1.5). */
public object CastanhaTipografia {
    /** Valor principal da carteira ("R$ 284.912"). */
    public val destaque: TextStyle =
        TextStyle(fontFamily = WorkSans, fontWeight = FontWeight.Bold, fontSize = 36.sp, letterSpacing = (-0.5).sp, lineHeight = 1.2.em)

    /** Título de tela. */
    public val titulo: TextStyle = TextStyle(fontFamily = WorkSans, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 1.2.em)

    /** Números em cartões de indicador. */
    public val indicador: TextStyle = TextStyle(fontFamily = WorkSans, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 1.2.em)

    public val destaqueTexto: TextStyle =
        TextStyle(fontFamily = Corpo, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 1.35.em)

    /** Título de seção. */
    public val secao: TextStyle = TextStyle(fontFamily = Corpo, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 1.5.em)

    public val corpoGrande: TextStyle = TextStyle(fontFamily = Corpo, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 1.5.em)

    public val corpo: TextStyle = TextStyle(fontFamily = Corpo, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 1.5.em)

    public val corpoForte: TextStyle = TextStyle(fontFamily = Corpo, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 1.5.em)

    public val acao: TextStyle = TextStyle(fontFamily = Corpo, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 1.5.em)

    public val legenda: TextStyle = TextStyle(fontFamily = Corpo, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 1.4.em)

    public val rotulo: TextStyle = TextStyle(fontFamily = Corpo, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 1.4.em)

    public val rotuloForte: TextStyle = TextStyle(fontFamily = Corpo, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 1.4.em)

    internal val material: Typography =
        Typography(
            headlineSmall = titulo,
            titleMedium = secao,
            bodyLarge = corpoGrande,
            bodyMedium = corpo,
            bodySmall = legenda,
            labelLarge = acao,
            labelMedium = rotulo,
        )
}
