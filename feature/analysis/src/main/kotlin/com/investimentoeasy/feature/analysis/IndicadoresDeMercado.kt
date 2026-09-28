package com.investimentoeasy.feature.analysis

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.investimentoeasy.core.designsystem.Castanha
import com.investimentoeasy.core.designsystem.Formatacao
import com.investimentoeasy.core.designsystem.componentes.CartaoContorno
import com.investimentoeasy.core.designsystem.componentes.ChipOrigem
import com.investimentoeasy.core.designsystem.componentes.LinhaValor
import com.investimentoeasy.core.model.Cotacao
import com.investimentoeasy.core.model.Origem
import com.investimentoeasy.core.model.PanoramaMercado
import com.investimentoeasy.core.model.Percent
import com.investimentoeasy.core.model.ValorDeMercado
import java.math.BigDecimal

/** Cards de mercado da skill: Selic, IPCA, dólar, Ibovespa e IFIX, com data e fonte, e o Focus por ano. */
@Composable
fun IndicadoresDeMercado(panorama: PanoramaMercado) {
    Secao("Indicadores de hoje") {
        CartaoContorno {
            panorama.selicMeta?.let { LinhaIndicador("Selic meta", taxa(it.valor), it) }
            panorama.ipca12Meses?.let { LinhaIndicador("IPCA em 12 meses", taxa(it.valor), it) }
            panorama.dolar?.let { LinhaIndicador("Dólar (PTAX)", "R$ ${Formatacao.numero(it.valor, 2)}", it) }
            panorama.ibovespa?.let { LinhaCotacao("Ibovespa", Formatacao.numero(it.preco, 0), it) }
            panorama.ifix?.let { LinhaCotacao("IFIX (XFIX11)", "R$ ${Formatacao.numero(it.preco, 2)}", it) }
        }
        ChipOrigem(Origem.MERCADO)
    }
    val focus = panorama.focus ?: return
    val anos = (focus.selicFimDeAno.keys + focus.ipcaDoAno.keys + focus.cambioFimDeAno.keys).sorted().take(ANOS_DO_FOCUS)
    Secao("Boletim Focus (${Formatacao.data(focus.dataPesquisa)})") {
        Column {
            Row(modifier = Modifier.padding(bottom = 6.dp)) {
                Celula("", PESO_ROTULO)
                anos.forEach { Celula(it.toString(), 1f) }
            }
            HorizontalDivider(color = Castanha.cores.borderSemiSoft)
            listOf(
                "Selic (fim do ano)" to focus.selicFimDeAno.mapValues { taxa(it.value) },
                "IPCA do ano" to focus.ipcaDoAno.mapValues { taxa(it.value) },
                "Dólar (fim do ano)" to focus.cambioFimDeAno.mapValues { Formatacao.numero(it.value, 2) },
            ).forEach { (nome, valores) ->
                Row(modifier = Modifier.padding(vertical = 8.dp)) {
                    Celula(nome, PESO_ROTULO, forte = true)
                    anos.forEach { Celula(valores[it] ?: Formatacao.AUSENTE, 1f) }
                }
                HorizontalDivider(color = Castanha.cores.borderSemiSoft)
            }
        }
        Text(
            "Mediana das projeções do mercado. Expectativa, não previsão.",
            style = Castanha.tipografia.legenda,
            color = Castanha.cores.textMedium,
        )
    }
}

@Composable
private fun LinhaIndicador(
    nome: String,
    valor: String,
    fonte: ValorDeMercado,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(vertical = 4.dp)) {
        LinhaValor(nome, valor)
        Text(
            "${fonte.fonte.rotulo} · ${Formatacao.data(fonte.data)}",
            style = Castanha.tipografia.legenda,
            color = Castanha.cores.textMedium,
        )
    }
}

@Composable
private fun LinhaCotacao(
    nome: String,
    valor: String,
    c: Cotacao,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(vertical = 4.dp)) {
        LinhaValor(nome, valor)
        val retornos =
            listOfNotNull(
                c.retorno3Meses?.let { "3m ${sinal(it)}" },
                c.retorno12Meses?.let { "12m ${sinal(it)}" },
            ).joinToString(" · ")
        Text(
            "$retornos · Yahoo Finance · ${Formatacao.data(c.data)}",
            style = Castanha.tipografia.legenda,
            color = Castanha.cores.textMedium,
        )
    }
}

@Composable
private fun RowScope.Celula(
    texto: String,
    peso: Float,
    forte: Boolean = false,
) {
    Text(
        texto,
        style = if (forte) Castanha.tipografia.rotuloForte else Castanha.tipografia.legenda,
        color = Castanha.cores.textIntense,
        modifier = Modifier.weight(peso),
    )
}

private fun taxa(valor: BigDecimal): String = Formatacao.percentual(Percent.of(valor), casas = 2)

private fun sinal(p: Percent): String = Formatacao.percentual(p, casas = 1, comSinal = true)

private const val ANOS_DO_FOCUS = 3
private const val PESO_ROTULO = 1.6f
