package com.investimentoeasy.feature.analysis

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.investimentoeasy.core.designsystem.Castanha
import com.investimentoeasy.core.designsystem.componentes.CartaoContorno
import com.investimentoeasy.core.designsystem.componentes.CartaoSuave
import com.investimentoeasy.core.designsystem.componentes.Etiqueta
import com.investimentoeasy.core.designsystem.componentes.Tom
import com.investimentoeasy.core.domain.mercado.Perfil

/** Sem perfil não há alvo: a primeira coisa que a aba pede. */
@Composable
internal fun EscolhaDePerfil(
    atual: Perfil?,
    aoEscolher: (Perfil) -> Unit,
) {
    CartaoSuave {
        Text("QUAL É O SEU PERFIL?", style = Castanha.tipografia.rotuloForte, color = Castanha.cores.textMedium)
        Text(
            "O perfil define a faixa de cada classe na carteira; o veredicto, as realocações e as sugestões partem dela.",
            style = Castanha.tipografia.corpo,
            color = Castanha.cores.textIntense,
        )
        Perfil.entries.forEach { perfil ->
            CartaoContorno(modifier = Modifier.fillMaxWidth().clickable(role = Role.Button) { aoEscolher(perfil) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(perfil.rotulo, style = Castanha.tipografia.corpoForte, color = Castanha.cores.textIntense)
                        Text(perfil.descricao, style = Castanha.tipografia.legenda, color = Castanha.cores.textMedium)
                    }
                    if (perfil == atual) Etiqueta("Atual", Tom.POSITIVO)
                }
            }
        }
    }
}
