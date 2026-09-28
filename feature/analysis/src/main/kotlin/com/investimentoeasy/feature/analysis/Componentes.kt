package com.investimentoeasy.feature.analysis

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.investimentoeasy.core.designsystem.Castanha
import com.investimentoeasy.core.designsystem.componentes.CartaoAviso
import com.investimentoeasy.core.designsystem.componentes.TituloSecao
import com.investimentoeasy.core.designsystem.componentes.Tom
import com.investimentoeasy.core.domain.analise.AtivoAnalisado

/** Tabela Ativo / Ritmo / Cenário do protótipo, com uma linha de apoio sob o nome. */
@Composable
fun TabelaRitmoCenario(
    ativos: List<AtivoAnalisado>,
    apoio: (AtivoAnalisado) -> String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = Castanha.cores.borderSemiSoft)
        Row(modifier = Modifier.padding(vertical = 8.dp)) {
            listOf("Ativo" to 1.3f, "Ritmo" to 1f, "Cenário" to 1f).forEach { (titulo, peso) ->
                Text(titulo, style = Castanha.tipografia.rotulo, color = Castanha.cores.textMedium, modifier = Modifier.weight(peso))
            }
        }
        HorizontalDivider(color = Castanha.cores.borderSemiSoft)
        ativos.forEach { a ->
            Row(modifier = Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Celula(a.posicao.ativo.nome, apoio(a), Modifier.weight(1.3f), forte = true)
                if (a.dadoAConferir) {
                    Celula("A conferir", "dado fora do plausível", Modifier.weight(1f))
                } else {
                    Celula(rotuloRitmo(a.ritmo), detalheRitmo(a.ritmo), Modifier.weight(1f))
                }
                Celula(a.cenario.cenario.rotulo, a.cenario.driver, Modifier.weight(1f))
            }
            HorizontalDivider(color = Castanha.cores.borderSemiSoft)
        }
    }
}

@Composable
private fun Celula(
    titulo: String,
    apoio: String,
    modifier: Modifier,
    forte: Boolean = false,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            titulo,
            style = if (forte) Castanha.tipografia.corpoForte else Castanha.tipografia.rotulo,
            color = Castanha.cores.textIntense,
        )
        Text(apoio, style = Castanha.tipografia.legenda, color = Castanha.cores.textMedium)
    }
}

@Composable
fun NotaRitmoCenario() {
    Text(
        "Ritmo compara o retorno do ano com o de 24 meses (anualizados). Cenário é a sensibilidade ao ciclo de juros que o " +
            "Focus indica (sem dados de mercado, supõe que o corte continue). Nenhum dos dois é previsão de preço.",
        style = Castanha.tipografia.legenda,
        color = Castanha.cores.textMedium,
    )
}

/** Leitura gerada pelas regras do app, marcada como tal. */
@Composable
fun NotasDoApp(
    titulo: String,
    notas: List<String>,
) {
    if (notas.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TituloSecao(titulo)
        notas.forEach { CartaoAviso(Tom.NEUTRO, "LEITURA DO APP", it) }
    }
}

@Composable
fun Secao(
    titulo: String,
    conteudo: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TituloSecao(titulo)
        conteudo()
    }
}

/** Sem a planilha, não há preço médio: diz de onde vem esse dado. */
@Composable
internal fun SemPlanilha(estado: EstadoAnalise) {
    if (estado.complemento != null) return
    CartaoAviso(
        Tom.INFORMATIVO,
        "ⓘ PREÇO MÉDIO E RESULTADO",
        "Envie a planilha Posição Detalhada da XP na aba Enviar para ver o custo e o resultado de cada posição.",
    )
}
