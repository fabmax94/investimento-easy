package com.investimentoeasy.core.designsystem.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.investimentoeasy.core.designsystem.Castanha

/** Raios do Castanha usados no app. */
public object Raios {
    public val medio: RoundedCornerShape = RoundedCornerShape(16.dp)
    public val grande: RoundedCornerShape = RoundedCornerShape(24.dp)
    public val micro: RoundedCornerShape = RoundedCornerShape(4.dp)
    public val pilula: RoundedCornerShape = RoundedCornerShape(percent = 50)
}

/** Cartão de destaque: fundo `surface-01`, raio grande. */
@Composable
public fun CartaoSuave(
    modifier: Modifier = Modifier,
    espacamento: PaddingValues = PaddingValues(20.dp),
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(Raios.grande)
                .background(Castanha.cores.surface01)
                .padding(espacamento),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = conteudo,
    )
}

/** Cartão com contorno `border-semi-soft`, raio médio. */
@Composable
public fun CartaoContorno(
    modifier: Modifier = Modifier,
    espacamento: PaddingValues = PaddingValues(16.dp),
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .border(1.dp, Castanha.cores.borderSemiSoft, Raios.medio)
                .padding(espacamento),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = conteudo,
    )
}

/** Aviso colorido pelo [tom], com rótulo curto em maiúsculas ("▲ ATENÇÃO"). */
@Composable
public fun CartaoAviso(
    tom: Tom,
    rotulo: String,
    texto: String,
    modifier: Modifier = Modifier,
    acao: (@Composable () -> Unit)? = null,
) {
    val cores = tom.cores
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(Raios.medio)
                .background(cores.fundo)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(rotulo, style = Castanha.tipografia.rotuloForte, color = cores.texto)
        Text(texto, style = Castanha.tipografia.corpo, color = Castanha.cores.textIntense)
        acao?.invoke()
    }
}

@Composable
public fun TituloSecao(
    texto: String,
    modifier: Modifier = Modifier,
) {
    Text(
        texto,
        style = Castanha.tipografia.secao,
        color = Castanha.cores.textIntense,
        modifier = modifier.semantics { heading() },
    )
}
