package com.investimentoeasy.core.designsystem.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.investimentoeasy.core.designsystem.Castanha
import com.investimentoeasy.core.model.Origem

/** Etiqueta em pílula ("Pronto", "Estimativa", "✓ 0,02% · dentro de 0,5%"). */
@Composable
public fun Etiqueta(
    texto: String,
    tom: Tom,
    modifier: Modifier = Modifier,
) {
    val cores = tom.cores
    Text(
        texto,
        style = Castanha.tipografia.rotulo,
        color = cores.texto,
        modifier =
            modifier
                .clip(Raios.pilula)
                .background(cores.fundo)
                .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** Regra zero: todo número mostra de onde veio. */
public val Origem.rotulo: String
    get() =
        when (this) {
            Origem.RELATORIO -> "relatório"
            Origem.MERCADO -> "mercado"
            Origem.CALCULADO -> "calculado"
            Origem.ESTIMADO -> "estimado"
            Origem.IA -> "extraído por IA"
        }

@Composable
public fun ChipOrigem(
    origem: Origem,
    modifier: Modifier = Modifier,
) {
    Text(
        origem.rotulo,
        style = Castanha.tipografia.legenda,
        color = Castanha.cores.textMedium,
        modifier =
            modifier
                .semantics { contentDescription = "Origem: ${origem.rotulo}" }
                // border-medium: visível tanto no fundo padrão quanto dentro de cartões (surface-01) no escuro.
                .border(1.dp, Castanha.cores.borderMedium, Raios.micro)
                .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}
