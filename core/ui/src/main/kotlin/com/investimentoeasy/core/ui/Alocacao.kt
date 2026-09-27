package com.investimentoeasy.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.investimentoeasy.core.designsystem.Castanha
import com.investimentoeasy.core.designsystem.Formatacao
import com.investimentoeasy.core.domain.alocacao.FatiaAlocacao
import com.investimentoeasy.core.domain.alocacao.GrupoAlocacao

public val GrupoAlocacao.rotulo: String
    get() =
        when (this) {
            GrupoAlocacao.RENDA_FIXA_POS -> "Renda fixa pós-fixada"
            GrupoAlocacao.RENDA_FIXA_IPCA -> "Renda fixa IPCA+"
            GrupoAlocacao.RENDA_FIXA_PRE -> "Renda fixa prefixada"
            GrupoAlocacao.MULTIMERCADO -> "Multimercado"
            GrupoAlocacao.FIIS -> "FIIs"
            GrupoAlocacao.ACOES_BRASIL -> "Ações Brasil"
            GrupoAlocacao.EXTERIOR -> "Exterior"
            GrupoAlocacao.CAIXA -> "Caixa"
            GrupoAlocacao.A_CLASSIFICAR -> "A classificar"
        }

/** Lista "Posições por classe": nome, quantidade de ativos, valor e percentual. */
@Composable
public fun ListaAlocacao(
    fatias: List<FatiaAlocacao>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        fatias.forEach { fatia ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(fatia.grupo.rotulo, style = Castanha.tipografia.corpo, color = Castanha.cores.textIntense)
                    Text(ativos(fatia.quantidadeAtivos), style = Castanha.tipografia.legenda, color = Castanha.cores.textMedium)
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        Formatacao.reais(fatia.valor, centavos = false),
                        style = Castanha.tipografia.corpoForte,
                        color = Castanha.cores.textIntense,
                        softWrap = false,
                    )
                    Text(
                        Formatacao.percentual(fatia.percentual, casas = 0),
                        style = Castanha.tipografia.legenda,
                        color = Castanha.cores.textMedium,
                        softWrap = false,
                    )
                }
            }
            HorizontalDivider(color = Castanha.cores.borderSemiSoft)
        }
    }
}

public fun ativos(quantidade: Int): String = if (quantidade == 1) "1 ativo" else "$quantidade ativos"
