package com.investimentoeasy.feature.portfolio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.investimentoeasy.core.designsystem.Castanha
import com.investimentoeasy.core.designsystem.Formatacao
import com.investimentoeasy.core.designsystem.Icones
import com.investimentoeasy.core.designsystem.componentes.BotaoPrimario
import com.investimentoeasy.core.designsystem.componentes.BotaoTexto
import com.investimentoeasy.core.designsystem.componentes.CartaoAviso
import com.investimentoeasy.core.designsystem.componentes.CartaoContorno
import com.investimentoeasy.core.designsystem.componentes.CartaoSuave
import com.investimentoeasy.core.designsystem.componentes.ChipOrigem
import com.investimentoeasy.core.designsystem.componentes.TituloSecao
import com.investimentoeasy.core.designsystem.componentes.Tom
import com.investimentoeasy.core.ui.ListaAlocacao

/** Tela 1 do protótipo, versão da Fase 1: a base confirmada, sem estimativa diária (que é a Fase 2). */
@Composable
fun CarteiraScreen(
    estado: EstadoCarteira,
    aoEnviar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(Castanha.cores.surfaceDefault)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Minha carteira",
                style = Castanha.tipografia.titulo,
                color = Castanha.cores.textIntense,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            IconButton(
                onClick = aoEnviar,
                modifier = Modifier.size(44.dp).border(1.dp, Castanha.cores.borderSemiSoft, CircleShape),
            ) {
                Icon(Icones.enviar, contentDescription = "Enviar nova carteira", tint = Castanha.cores.iconsIntense)
            }
        }
        when (estado) {
            EstadoCarteira.Carregando ->
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Castanha.cores.accentSolidMedium)
                }
            EstadoCarteira.SemCarteira -> SemCarteira(aoEnviar)
            is EstadoCarteira.ComBase -> ComBase(estado, aoEnviar)
        }
    }
}

@Composable
private fun SemCarteira(aoEnviar: () -> Unit) {
    CartaoSuave {
        Text("Nenhuma carteira ainda", style = Castanha.tipografia.secao, color = Castanha.cores.textIntense)
        Text(
            "Envie o PDF XPerformance da XP. O app confere os números com você antes de salvar.",
            style = Castanha.tipografia.corpo,
            color = Castanha.cores.textMedium,
        )
    }
    BotaoPrimario("Enviar carteira", onClick = aoEnviar)
}

@Composable
private fun ComBase(
    estado: EstadoCarteira.ComBase,
    aoEnviar: () -> Unit,
) {
    CartaoSuave {
        Text("Patrimônio no último upload", style = Castanha.tipografia.corpo, color = Castanha.cores.textMedium)
        Text(
            Formatacao.reais(estado.patrimonio, centavos = false),
            style = Castanha.tipografia.destaque,
            color = Castanha.cores.textIntense,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            ChipOrigem(estado.origemPatrimonio)
            Text(
                "Posição de ${Formatacao.data(estado.dataReferencia)}",
                style = Castanha.tipografia.legenda,
                color = Castanha.cores.textMedium,
            )
        }
    }

    CartaoContorno {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Último upload: ${Formatacao.data(estado.dataReferencia)}",
                    style = Castanha.tipografia.corpoForte,
                    color = Castanha.cores.textIntense,
                )
                Text(textoAtualidade(estado), style = Castanha.tipografia.legenda, color = Castanha.cores.textMedium)
            }
            BotaoTexto("Atualizar", onClick = aoEnviar)
        }
    }
    if (estado.atualidade == Atualidade.DESATUALIZADA) {
        CartaoAviso(
            Tom.ATENCAO,
            "▲ CARTEIRA DESATUALIZADA",
            "A última posição tem mais de ${CarteiraViewModel.DIAS_DESATUALIZADA} dias. Envie um relatório novo para a análise valer.",
        )
    }
    if (estado.divergenciaAceita) {
        CartaoAviso(
            Tom.INFORMATIVO,
            "ⓘ DIVERGÊNCIA ACEITA",
            "No upload, a soma das posições não bateu com o patrimônio informado e você confirmou mesmo assim.",
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        TituloSecao("Alocação")
        ListaAlocacao(estado.alocacao)
    }
}

fun textoAtualidade(estado: EstadoCarteira.ComBase): String {
    val dias =
        when (estado.diasDesdeReferencia) {
            0L -> "Posição de hoje"
            1L -> "Há 1 dia"
            else -> "Há ${estado.diasDesdeReferencia} dias"
        }
    val versao = if (estado.versao > 1) " · versão ${estado.versao}" else ""
    return when (estado.atualidade) {
        Atualidade.EM_DIA -> "$dias · sugerimos atualizar a partir de ${CarteiraViewModel.DIAS_SUGERIR_UPLOAD} dias$versao"
        Atualidade.SUGERIR_UPLOAD, Atualidade.DESATUALIZADA -> "$dias · hora de enviar um relatório novo$versao"
    }
}
