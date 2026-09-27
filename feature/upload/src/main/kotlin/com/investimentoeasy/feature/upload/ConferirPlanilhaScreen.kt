package com.investimentoeasy.feature.upload

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.investimentoeasy.core.designsystem.Castanha
import com.investimentoeasy.core.designsystem.Formatacao
import com.investimentoeasy.core.designsystem.componentes.BotaoPrimario
import com.investimentoeasy.core.designsystem.componentes.BotaoTexto
import com.investimentoeasy.core.designsystem.componentes.CabecalhoTela
import com.investimentoeasy.core.designsystem.componentes.CartaoAviso
import com.investimentoeasy.core.designsystem.componentes.CartaoContorno
import com.investimentoeasy.core.designsystem.componentes.Etiqueta
import com.investimentoeasy.core.designsystem.componentes.TituloSecao
import com.investimentoeasy.core.designsystem.componentes.Tom

/** Conferência da planilha "Posição Detalhada" antes de guardar os dados ao lado da base (R8, R16). */
@Composable
fun ConferirPlanilhaScreen(
    conferencia: ConferenciaPlanilhaUi,
    guardando: Boolean,
    aoGuardar: () -> Unit,
    aoVoltar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(Castanha.cores.surfaceDefault)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        BotaoTexto("‹ Enviar carteira", onClick = aoVoltar, alinhadoAoTexto = true)
        CabecalhoTela(
            "Conferir planilha",
            subtitulo =
                "Posição de ${Formatacao.data(conferencia.dataPlanilha)} · base de ${Formatacao.data(conferencia.dataBase)}. " +
                    "A planilha só acrescenta dados à base; nenhum saldo muda.",
        )
        Avisos(conferencia)
        Casadas(conferencia.casadas)
        ListaSimples(
            "Só na planilha (${conferencia.soNaPlanilha.size})",
            "Não estão na base: vendidos ou vencidos entre as duas datas, ou com nome que não deu para casar. Ficam de fora.",
            conferencia.soNaPlanilha,
        )
        ListaSimples(
            "Sem dado da planilha (${conferencia.soNaBase.size})",
            "Estão na base, mas não na planilha: continuam só com os dados do PDF.",
            conferencia.soNaBase,
        )
        BotaoPrimario(
            if (guardando) "Guardando…" else "Guardar dados da planilha",
            onClick = aoGuardar,
            habilitado = conferencia.podeGuardar && !guardando,
        )
    }
}

@Composable
private fun Avisos(conferencia: ConferenciaPlanilhaUi) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (!conferencia.podeGuardar) {
            CartaoAviso(Tom.NEGATIVO, "● NADA CASOU", "Nenhuma posição da planilha corresponde à base atual. Confira se é a mesma conta.")
        }
        if (conferencia.planilhaMaisAntiga) {
            CartaoAviso(
                Tom.ATENCAO,
                "! PLANILHA MAIS ANTIGA QUE A BASE",
                "Preço médio e valor aplicado valem para ${Formatacao.data(conferencia.dataPlanilha)}. " +
                    "Para a análise mais fiel, exporte a planilha no mesmo dia do relatório.",
            )
        }
        if (conferencia.comQuantidadeDiferente > 0) {
            CartaoAviso(
                Tom.ATENCAO,
                "! QUANTIDADE DIFERENTE (${conferencia.comQuantidadeDiferente})",
                "Houve compra ou venda entre as datas: nessas posições o preço médio pode não valer para hoje.",
            )
        }
        if (conferencia.proventos > 0) {
            val texto = if (conferencia.proventos == 1) "1 provento previsto" else "${conferencia.proventos} proventos previstos"
            CartaoAviso(Tom.INFORMATIVO, "ⓘ PROVENTOS", "$texto na planilha também serão guardados.")
        }
    }
}

@Composable
private fun Casadas(casadas: List<LinhaConferida>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TituloSecao("Casadas com a base (${casadas.size})")
        if (casadas.isEmpty()) return
        CartaoContorno {
            casadas.forEachIndexed { i, linha ->
                if (i > 0) HorizontalDivider(color = Castanha.cores.borderSemiSoft, modifier = Modifier.padding(vertical = 6.dp))
                LinhaCasada(linha)
            }
        }
    }
}

@Composable
private fun LinhaCasada(linha: LinhaConferida) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
        modifier = Modifier.semantics(mergeDescendants = true) { },
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(linha.nome, style = Castanha.tipografia.corpoForte, color = Castanha.cores.textIntense)
            linha.nomeNaPlanilha?.let {
                Text("Na planilha: $it", style = Castanha.tipografia.legenda, color = Castanha.cores.textMedium)
            }
            linha.detalhe?.let { Text(it, style = Castanha.tipografia.legenda, color = Castanha.cores.textMedium) }
        }
        if (linha.quantidadeMudou) Etiqueta("Qtd. mudou", Tom.ATENCAO)
    }
}

@Composable
private fun ListaSimples(
    titulo: String,
    explicacao: String,
    nomes: List<String>,
) {
    if (nomes.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TituloSecao(titulo)
        Text(explicacao, style = Castanha.tipografia.legenda, color = Castanha.cores.textMedium)
        nomes.forEach { Text("• $it", style = Castanha.tipografia.corpo, color = Castanha.cores.textIntense) }
    }
}
