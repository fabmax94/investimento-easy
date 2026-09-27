package com.investimentoeasy.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

internal val LocalCastanhaCores = staticCompositionLocalOf { CastanhaCoresClaras }

/** Acesso às cores e à tipografia do Castanha dentro de [CastanhaTema]. */
public object Castanha {
    public val cores: CastanhaCores
        @Composable
        @ReadOnlyComposable
        get() = LocalCastanhaCores.current

    public val tipografia: CastanhaTipografia = CastanhaTipografia
}

/**
 * Tema do app: cores do Castanha (claro e escuro) e um `MaterialTheme` alinhado a elas,
 * para os componentes do Material 3 (diálogos, seletores de data) herdarem a mesma paleta.
 */
@Composable
public fun CastanhaTema(
    escuro: Boolean = isSystemInDarkTheme(),
    conteudo: @Composable () -> Unit,
) {
    val cores = if (escuro) CastanhaCoresEscuras else CastanhaCoresClaras
    val esquema =
        if (escuro) {
            darkColorScheme(
                primary = cores.accentSolidMedium,
                onPrimary = cores.supportLighter,
                background = cores.surfaceDefault,
                onBackground = cores.textIntense,
                surface = cores.surfaceDefault,
                onSurface = cores.textIntense,
                surfaceVariant = cores.surface01,
                onSurfaceVariant = cores.textMedium,
                outline = cores.borderSemiSoft,
                error = cores.feedbackNegativeSemiIntense,
            )
        } else {
            lightColorScheme(
                primary = cores.accentSolidMedium,
                onPrimary = cores.supportLighter,
                background = cores.surfaceDefault,
                onBackground = cores.textIntense,
                surface = cores.surfaceDefault,
                onSurface = cores.textIntense,
                surfaceVariant = cores.surface01,
                onSurfaceVariant = cores.textMedium,
                outline = cores.borderSemiSoft,
                error = cores.feedbackNegativeSemiIntense,
            )
        }
    CompositionLocalProvider(LocalCastanhaCores provides cores) {
        MaterialTheme(colorScheme = esquema, typography = CastanhaTipografia.material, content = conteudo)
    }
}
