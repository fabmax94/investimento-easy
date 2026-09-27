package com.investimentoeasy.core.designsystem.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.investimentoeasy.core.designsystem.Castanha
import com.investimentoeasy.core.designsystem.CastanhaTipografia

/** Ação principal da tela: pílula na cor de destaque, 52dp de altura. */
@Composable
public fun BotaoPrimario(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
) {
    val cores = Castanha.cores
    Button(
        onClick = onClick,
        enabled = habilitado,
        shape = Raios.pilula,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = cores.accentSolidMedium,
                contentColor = cores.supportLighter,
                disabledContainerColor = cores.surface01,
                disabledContentColor = cores.textMedium,
            ),
        modifier = modifier.fillMaxWidth().heightIn(min = 52.dp),
    ) {
        Text(texto, style = CastanhaTipografia.corpoGrande.copy(fontWeight = CastanhaTipografia.secao.fontWeight))
    }
}

/** Ação secundária com contorno ("Escolher arquivo"). */
@Composable
public fun BotaoContorno(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
) {
    val cores = Castanha.cores
    OutlinedButton(
        onClick = onClick,
        enabled = habilitado,
        shape = Raios.pilula,
        border = BorderStroke(1.dp, cores.textIntense),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = cores.textIntense),
        modifier = modifier.heightIn(min = 44.dp),
    ) {
        Text(texto, style = CastanhaTipografia.acao)
    }
}

/**
 * Ação de texto ("Atualizar", "‹ Carteira"). Com [alinhadoAoTexto], não tem recuo lateral e
 * alinha com o texto ao redor (links de voltar, ações dentro de avisos); o alvo de toque
 * continua com 44dp de altura.
 */
@Composable
public fun BotaoTexto(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    alinhadoAoTexto: Boolean = false,
) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(contentColor = Castanha.cores.accentSolidSemiIntense),
        contentPadding = if (alinhadoAoTexto) PaddingValues(vertical = 8.dp) else ButtonDefaults.TextButtonContentPadding,
        modifier = modifier.heightIn(min = 44.dp),
    ) {
        Text(texto, style = CastanhaTipografia.acao)
    }
}
