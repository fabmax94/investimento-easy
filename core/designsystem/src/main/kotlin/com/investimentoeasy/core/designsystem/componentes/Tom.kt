package com.investimentoeasy.core.designsystem.componentes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.investimentoeasy.core.designsystem.Castanha

/** Tom semântico de etiquetas e avisos (feedback do Castanha). */
public enum class Tom { POSITIVO, NEGATIVO, ATENCAO, INFORMATIVO, NEUTRO }

internal data class ParDeCores(
    val fundo: Color,
    val texto: Color,
)

internal val Tom.cores: ParDeCores
    @Composable
    @ReadOnlyComposable
    get() {
        val c = Castanha.cores
        return when (this) {
            Tom.POSITIVO -> ParDeCores(c.feedbackPositiveSoft, c.feedbackPositiveSemiIntense)
            Tom.NEGATIVO -> ParDeCores(c.feedbackNegativeSoft, c.feedbackNegativeSemiIntense)
            Tom.ATENCAO -> ParDeCores(c.feedbackWarningSoft, c.feedbackWarningIntense)
            Tom.INFORMATIVO -> ParDeCores(c.feedbackInformativeSoft, c.feedbackInformativeSemiIntense)
            Tom.NEUTRO -> ParDeCores(c.surface01, c.textIntense)
        }
    }
