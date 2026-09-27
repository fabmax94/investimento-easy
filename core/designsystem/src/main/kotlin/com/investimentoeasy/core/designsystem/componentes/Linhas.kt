package com.investimentoeasy.core.designsystem.componentes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.investimentoeasy.core.designsystem.Castanha

/**
 * Linha "rótulo ........ valor" dos cartões de resumo.
 *
 * Quando rótulo e valor não cabem lado a lado (fonte grande, valor largo), o valor desce
 * para a linha de baixo em vez de espremer o rótulo. O valor em texto nunca quebra: um
 * número partido ao meio engana.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun LinhaValor(
    rotulo: String,
    modifier: Modifier = Modifier,
    valor: @Composable () -> Unit,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(rotulo, style = Castanha.tipografia.corpo, color = Castanha.cores.textMedium, modifier = Modifier.padding(end = 12.dp))
        valor()
    }
}

@Composable
public fun LinhaValor(
    rotulo: String,
    valor: String,
    modifier: Modifier = Modifier,
) {
    LinhaValor(rotulo, modifier) {
        Text(valor, style = Castanha.tipografia.corpoForte, color = Castanha.cores.textIntense, softWrap = false)
    }
}

/** Título de tela com subtítulo opcional. */
@Composable
public fun CabecalhoTela(
    titulo: String,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            titulo,
            style = Castanha.tipografia.titulo,
            color = Castanha.cores.textIntense,
            modifier = Modifier.semantics { heading() },
        )
        subtitulo?.let { Text(it, style = Castanha.tipografia.corpo, color = Castanha.cores.textMedium) }
    }
}
