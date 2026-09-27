package com.investimentoeasy.feature.analysis

import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.investimentoeasy.core.designsystem.Castanha
import com.investimentoeasy.core.designsystem.Formatacao
import com.investimentoeasy.core.designsystem.componentes.BotaoPrimario
import com.investimentoeasy.core.designsystem.componentes.BotaoTexto
import com.investimentoeasy.core.designsystem.componentes.CartaoAviso
import com.investimentoeasy.core.designsystem.componentes.CartaoContorno
import com.investimentoeasy.core.designsystem.componentes.CartaoSuave
import com.investimentoeasy.core.designsystem.componentes.ChipOrigem
import com.investimentoeasy.core.designsystem.componentes.Etiqueta
import com.investimentoeasy.core.designsystem.componentes.LinhaValor
import com.investimentoeasy.core.designsystem.componentes.Raios
import com.investimentoeasy.core.designsystem.componentes.Tom
import com.investimentoeasy.core.domain.analise.AnaliseDeterministica
import com.investimentoeasy.core.domain.analise.Concentracao
import com.investimentoeasy.core.domain.analise.Severidade
import com.investimentoeasy.core.model.ClasseAtivo
import com.investimentoeasy.core.model.Origem
import com.investimentoeasy.core.model.TipoAtivo
import com.investimentoeasy.core.model.soma
import com.investimentoeasy.core.ui.ListaAlocacao
import com.investimentoeasy.core.ui.ativos

@Composable
fun AbaOQueFazer(
    estado: EstadoAnalise,
    d: AnaliseDeterministica,
    aoGerar: () -> Unit,
    aoConfigurarChave: () -> Unit,
) {
    val ia = estado.ia
    ContagemAlertas(d)
    if (ia == null) {
        CartaoSuave {
            Text("VEREDICTO E PLANO", style = Castanha.tipografia.rotuloForte, color = Castanha.cores.textMedium)
            Text(
                "O Claude lê os números que o app calculou e escreve o veredicto, o plano de 30 dias e as realocações. " +
                    "Cada número que ele citar é conferido com a sua carteira antes de aparecer aqui.",
                style = Castanha.tipografia.corpo,
                color = Castanha.cores.textIntense,
            )
            Text(
                "Custa uma chamada à API, paga pela sua chave. A carteira vai sem número de conta e sem nome do assessor.",
                style = Castanha.tipografia.legenda,
                color = Castanha.cores.textMedium,
            )
        }
        estado.erro?.let { CartaoAviso(Tom.NEGATIVO, "● NÃO FOI POSSÍVEL GERAR", it) }
        if (estado.temChave) {
            BotaoPrimario(
                if (estado.gerando) "Gerando análise…" else "Gerar análise com o Claude",
                onClick = aoGerar,
                habilitado = !estado.gerando,
            )
        } else {
            BotaoPrimario("Configurar chave da API", onClick = aoConfigurarChave)
        }
        Secao("Principais alertas") {
            d.alertas.take(PRINCIPAIS).forEach { CartaoAviso(it.severidade.estilo.tom, it.severidade.estilo.rotulo, textoAlerta(it)) }
            if (d.alertas.isEmpty()) CartaoAviso(Tom.POSITIVO, "✓ NENHUM ALERTA", "As regras de alerta não dispararam para esta carteira.")
        }
        return
    }

    CartaoSuave {
        Text("VEREDICTO", style = Castanha.tipografia.rotuloForte, color = Castanha.cores.textMedium)
        Text(ia.saida.veredicto, style = Castanha.tipografia.destaqueTexto, color = Castanha.cores.textIntense)
        Text(
            "Interpretação do Claude · ${Formatacao.data(ia.geradaEm.atZone(java.time.ZoneId.systemDefault()).toLocalDate())}",
            style = Castanha.tipografia.legenda,
            color = Castanha.cores.textMedium,
        )
    }
    Secao("Próximos 30 dias") {
        ia.saida.oQueFazer.acoes30Dias.forEachIndexed { i, acao ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${i + 1}.", style = Castanha.tipografia.corpoForte, color = Castanha.cores.textIntense)
                Column {
                    Text(acao.titulo, style = Castanha.tipografia.corpoForte, color = Castanha.cores.textIntense)
                    Text(acao.detalhe, style = Castanha.tipografia.corpo, color = Castanha.cores.textIntense)
                }
            }
        }
    }
    if (ia.saida.oQueFazer.realocacoes.isNotEmpty()) {
        Secao("Realocações") {
            ia.saida.oQueFazer.realocacoes.forEach { r ->
                CartaoContorno {
                    LinhaValor("${r.vender} → ${r.destino}", valorSugerido(r.valor))
                    Text(r.motivo, style = Castanha.tipografia.legenda, color = Castanha.cores.textMedium)
                }
            }
        }
    }
    if (ia.saida.oQueFazer.novosAtivos.isNotEmpty()) {
        Secao("Novos ativos para lacunas") {
            ia.saida.oQueFazer.novosAtivos.forEach { n ->
                CartaoContorno {
                    LinhaValor(n.sugestao, valorSugerido(n.valor))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Etiqueta(n.prioridade.estilo.rotulo, n.prioridade.estilo.tom)
                        Etiqueta(n.lacuna.rotulo, Tom.NEUTRO)
                    }
                    Text(n.justificativa, style = Castanha.tipografia.legenda, color = Castanha.cores.textMedium)
                }
            }
        }
    }
    estado.erro?.let { CartaoAviso(Tom.NEGATIVO, "● NÃO FOI POSSÍVEL GERAR", it) }
    if (estado.temChave) {
        BotaoTexto(if (estado.gerando) "Gerando…" else "Gerar a análise de novo", onClick = aoGerar, alinhadoAoTexto = true)
    }
}

@Composable
private fun ContagemAlertas(d: AnaliseDeterministica) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            "urgentes" to d.alertas.count { it.severidade == Severidade.URGENTE },
            "críticos" to d.alertas.count { it.severidade == Severidade.CRITICO },
            "atenção" to d.alertas.count { it.severidade == Severidade.ATENCAO },
        ).forEach { (rotulo, n) ->
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .border(1.dp, Castanha.cores.borderSemiSoft, Raios.medio)
                        .padding(12.dp),
            ) {
                Text("$n", style = Castanha.tipografia.indicador, color = Castanha.cores.textIntense)
                Text(rotulo, style = Castanha.tipografia.legenda, color = Castanha.cores.textMedium)
            }
        }
    }
}

@Composable
fun AbaMercado(
    estado: EstadoAnalise,
    d: AnaliseDeterministica,
) {
    if (d.mercado.isEmpty()) {
        CartaoAviso(
            Tom.INFORMATIVO,
            "ⓘ SEM ÍNDICES NESTA BASE",
            "Esta carteira foi enviada antes de o app guardar os índices do relatório. " +
                "Envie o relatório de novo para ver CDI, Ibovespa, IPCA e Dólar.",
        )
    } else {
        Secao("Índices do relatório") {
            Row { listOf("", "Mês", "Ano", "12M", "24M").forEachIndexed { i, t -> CabecalhoIndice(t, if (i == 0) 1.4f else 1f) } }
            HorizontalDivider(color = Castanha.cores.borderSemiSoft)
            d.mercado.forEach { indice ->
                Row(modifier = Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        indice.nome,
                        style = Castanha.tipografia.corpoForte,
                        color = Castanha.cores.textIntense,
                        modifier = Modifier.weight(1.4f),
                    )
                    listOf(indice.mes, indice.ano, indice.dozeMeses, indice.vinteQuatroMeses).forEach { v ->
                        Text(
                            Formatacao.percentual(v, casas = 2),
                            style = Castanha.tipografia.legenda,
                            color = Castanha.cores.textIntense,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                HorizontalDivider(color = Castanha.cores.borderSemiSoft)
            }
            ChipOrigem(Origem.RELATORIO)
        }
    }
    CartaoAviso(
        Tom.INFORMATIVO,
        "ⓘ DADOS DE MERCADO",
        "Selic atual e expectativas de mercado ainda não entram no app: a análise usa só os índices do relatório.",
    )
    estado.ia?.let { NotasDoClaude(listOf(it.saida.mercado.analise)) }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.CabecalhoIndice(
    texto: String,
    peso: Float,
) {
    Text(texto, style = Castanha.tipografia.rotulo, color = Castanha.cores.textMedium, modifier = Modifier.weight(peso))
}

@Composable
fun AbaAlocacao(
    estado: EstadoAnalise,
    d: AnaliseDeterministica,
) {
    Secao("Por classe") { ListaAlocacao(d.alocacao) }
    Secao("Concentração por gestora") {
        if (d.gestoras.isEmpty()) Text("Sem fundos na carteira.", style = Castanha.tipografia.corpo, color = Castanha.cores.textMedium)
        d.gestoras.forEach { ListaConcentracao(it, "fundo") }
    }
    d.exposicaoGlobal?.let {
        CartaoContorno {
            LinhaValor("Renda variável global", Formatacao.percentual(it, casas = 1))
            Text(
                "Sem a carteira interna dos fundos, a sobreposição exata entre fundos e ETFs não é medida.",
                style = Castanha.tipografia.legenda,
                color = Castanha.cores.textMedium,
            )
        }
    }
    if (d.emissoresGarantidos.isNotEmpty()) {
        Secao("Emissores com garantia do FGC") {
            d.emissoresGarantidos.forEach { ListaConcentracao(it, "título") }
            Text(
                "Limite do FGC: R$ 250 mil por CPF e por instituição.",
                style = Castanha.tipografia.legenda,
                color = Castanha.cores.textMedium,
            )
        }
    }
    estado.ia?.saida?.alocacao?.diagnostico?.takeIf { it.isNotEmpty() }?.let { diagnosticos ->
        Secao("Diagnóstico do Claude") {
            diagnosticos.forEach { CartaoAviso(it.status.estilo.tom, it.status.estilo.rotulo, it.texto) }
        }
    }
}

@Composable
private fun ListaConcentracao(
    c: Concentracao,
    unidade: String,
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(c.nome, style = Castanha.tipografia.corpo, color = Castanha.cores.textIntense)
            Text(
                "${c.quantidadeAtivos} ${if (c.quantidadeAtivos == 1) unidade else "${unidade}s"}",
                style = Castanha.tipografia.legenda,
                color = Castanha.cores.textMedium,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(Formatacao.reais(c.valor, centavos = false), style = Castanha.tipografia.corpoForte, color = Castanha.cores.textIntense)
            Text(Formatacao.percentual(c.percentual, casas = 1), style = Castanha.tipografia.legenda, color = Castanha.cores.textMedium)
        }
    }
}

@Composable
fun AbaFundos(
    estado: EstadoAnalise,
    d: AnaliseDeterministica,
) {
    val fundos = d.ativos.filter { it.posicao.ativo.tipo == TipoAtivo.FUNDO }
    if (fundos.isEmpty()) {
        Text("Sem fundos na carteira.", style = Castanha.tipografia.corpo, color = Castanha.cores.textMedium)
    } else {
        TabelaRitmoCenario(fundos, apoio = { "${Formatacao.percentual(it.posicao.percentualCdiAno?.valor, casas = 0)} do CDI no ano" })
        NotaRitmoCenario()
    }
    estado.ia?.let { NotasDoClaude(it.saida.fundos.notas) }
}

@Composable
fun AbaFiis(
    estado: EstadoAnalise,
    d: AnaliseDeterministica,
) {
    val fiis = d.ativos.filter { it.posicao.ativo.tipo == TipoAtivo.FII }
    if (fiis.isEmpty()) {
        Text("Sem FIIs na carteira.", style = Castanha.tipografia.corpo, color = Castanha.cores.textMedium)
    } else {
        Secao("Composição") {
            val total = fiis.map { it.posicao.saldo.valor }.soma()
            listOf(
                "Tijolo" to ClasseAtivo.FII_TIJOLO,
                "Papel" to ClasseAtivo.FII_PAPEL,
                "Fundo de fundos" to ClasseAtivo.FII_FOF,
                "Segmento a confirmar" to ClasseAtivo.FII_NAO_CLASSIFICADO,
            ).forEach { (nome, classe) ->
                val doSegmento = fiis.filter { it.posicao.ativo.classe == classe }
                if (doSegmento.isNotEmpty()) {
                    val valor = doSegmento.map { it.posicao.saldo.valor }.soma()
                    LinhaValor(
                        "$nome · ${ativos(doSegmento.size)}",
                        "${Formatacao.reais(
                            valor,
                            centavos = false,
                        )} · ${Formatacao.percentual(
                            valor.fracaoDe(total)?.let(com.investimentoeasy.core.model.Percent::daFracao),
                            casas = 0,
                        )}",
                    )
                }
            }
        }
        TabelaRitmoCenario(fiis, apoio = { "${Formatacao.percentual(it.posicao.rentabilidadeAno?.valor, casas = 2)} no ano" })
        NotaRitmoCenario()
        CartaoAviso(Tom.INFORMATIVO, "ⓘ DADOS DE MERCADO", "P/VP, dividend yield e vacância ainda não entram no app.")
    }
    estado.ia?.let { NotasDoClaude(it.saida.fiis.notas) }
}

@Composable
fun AbaAcoesEtfs(
    estado: EstadoAnalise,
    d: AnaliseDeterministica,
) {
    val renda = d.ativos.filter { it.posicao.ativo.tipo in setOf(TipoAtivo.ACAO, TipoAtivo.ETF, TipoAtivo.BDR) }
    if (renda.isEmpty()) {
        Text("Sem ações, ETFs ou BDRs na carteira.", style = Castanha.tipografia.corpo, color = Castanha.cores.textMedium)
    } else {
        TabelaRitmoCenario(renda, apoio = { Formatacao.reais(it.posicao.saldo.valor, centavos = false) })
        NotaRitmoCenario()
    }
    if (d.simbolicas.isNotEmpty()) {
        Secao("Posições simbólicas") {
            d.simbolicas.forEach { LinhaValor(it.ativo.nome, Formatacao.reais(it.saldo.valor)) }
        }
    }
    estado.ia?.let { NotasDoClaude(it.saida.acoesEtfs.notas) }
}

@Composable
fun AbaAlertas(
    estado: EstadoAnalise,
    d: AnaliseDeterministica,
) {
    if (d.alertas.isEmpty()) {
        CartaoAviso(Tom.POSITIVO, "✓ NENHUM ALERTA", "As regras de alerta não dispararam para esta carteira.")
    }
    val comentarios = estado.ia?.saida?.alertas?.comentarios.orEmpty().groupBy { it.regra }
    d.alertas.forEach { alerta ->
        CartaoAviso(alerta.severidade.estilo.tom, alerta.severidade.estilo.rotulo, textoAlerta(alerta)) {
            comentarios[alerta.regra.name]?.firstOrNull()?.let {
                Text(it.texto, style = Castanha.tipografia.legenda.copy(fontWeight = FontWeight.Normal), color = Castanha.cores.textMedium)
            }
        }
    }
}

private const val PRINCIPAIS = 3
