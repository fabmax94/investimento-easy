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
    acoes: AcoesAnalise,
) {
    ContagemAlertas(d)
    val r = estado.recomendacao
    if (r == null || estado.escolhendoPerfil) {
        EscolhaDePerfil(estado.perfil, acoes.aoEscolherPerfil)
        Secao("Principais alertas") {
            d.alertas.take(PRINCIPAIS).forEach { CartaoAviso(it.severidade.estilo.tom, it.severidade.estilo.rotulo, textoAlerta(it)) }
            if (d.alertas.isEmpty()) CartaoAviso(Tom.POSITIVO, "✓ NENHUM ALERTA", "As regras de alerta não dispararam para esta carteira.")
        }
        return
    }
    CartaoSuave {
        Text("VEREDICTO", style = Castanha.tipografia.rotuloForte, color = Castanha.cores.textMedium)
        Text(r.veredicto, style = Castanha.tipografia.destaqueTexto, color = Castanha.cores.textIntense)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Gerado no aparelho · ${r.perfil.rotuloCurto}",
                style = Castanha.tipografia.legenda,
                color = Castanha.cores.textMedium,
                modifier = Modifier.weight(1f),
            )
            BotaoTexto("Trocar perfil", onClick = acoes.aoTrocarPerfil)
        }
    }
    Secao("Próximos 30 dias") {
        r.acoes30Dias.forEachIndexed { i, acao ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${i + 1}.", style = Castanha.tipografia.corpoForte, color = Castanha.cores.textIntense)
                Column {
                    Text(acao.titulo, style = Castanha.tipografia.corpoForte, color = Castanha.cores.textIntense)
                    Text(acao.detalhe, style = Castanha.tipografia.corpo, color = Castanha.cores.textIntense)
                }
            }
        }
    }
    if (r.realocacoes.isNotEmpty()) {
        Secao("Realocações") {
            r.realocacoes.forEach { re ->
                CartaoContorno {
                    LinhaValor("${re.vender} → ${re.destino}", Formatacao.reais(re.valor, centavos = false))
                    Text(re.motivo, style = Castanha.tipografia.legenda, color = Castanha.cores.textMedium)
                }
            }
        }
    }
    if (r.novosAtivos.isNotEmpty()) {
        Secao("Novos ativos para lacunas") {
            r.novosAtivos.forEach { n ->
                CartaoContorno {
                    LinhaValor(n.sugestao, Formatacao.reais(n.valor, centavos = false))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Etiqueta(n.prioridade.estilo.rotulo, n.prioridade.estilo.tom)
                        Etiqueta(n.motivo.rotulo, Tom.NEUTRO)
                    }
                    Text(n.justificativa, style = Castanha.tipografia.legenda, color = Castanha.cores.textMedium)
                }
            }
        }
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
    estado.panorama?.let { IndicadoresDeMercado(it) }
    estado.recomendacao?.mercado?.takeIf { it.isNotEmpty() }?.let { NotasDoApp("O mercado e a sua carteira", it) }
    if (d.mercado.isEmpty()) {
        CartaoAviso(
            Tom.INFORMATIVO,
            "ⓘ SEM ÍNDICES NESTA BASE",
            "Esta carteira foi enviada antes de o app guardar os índices do relatório. " +
                "Envie o relatório de novo para ver CDI, Ibovespa, IPCA e Dólar do período.",
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
    aoTrocarPerfil: () -> Unit,
) {
    Secao("Por classe") { ListaAlocacao(d.alocacao) }
    estado.recomendacao?.let { r ->
        Secao("Contra o ${r.perfil.rotuloCurto.lowercase()}") {
            r.alocacao.forEach { CartaoAviso(it.status.estilo.tom, it.status.estilo.rotulo, it.texto) }
            BotaoTexto("Trocar perfil", onClick = aoTrocarPerfil, alinhadoAoTexto = true)
        }
    }
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
        TabelaRitmoCenario(fundos, apoio = {
            apoioComResultado(
                estado.complemento,
                it.posicao,
                "${Formatacao.percentual(it.posicao.percentualCdiAno?.valor, casas = 0)} do CDI no ano",
            )
        })
        NotaRitmoCenario()
    }
    estado.recomendacao?.fundos?.let { NotasDoApp("Leitura do app", it) }
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
        TabelaRitmoCenario(fiis, apoio = {
            apoioComResultado(
                estado.complemento,
                it.posicao,
                "${Formatacao.percentual(it.posicao.rentabilidadeAno?.valor, casas = 2)} no ano",
            )
        })
        NotaRitmoCenario()
        SemPlanilha(estado)
        if (estado.panorama == null) {
            CartaoAviso(Tom.INFORMATIVO, "ⓘ DADOS DE MERCADO", "Atualize os dados de mercado para ver P/VP e dividend yield de cada FII.")
        }
    }
    estado.recomendacao?.fiis?.let { NotasDoApp("Leitura do app", it) }
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
        TabelaRitmoCenario(renda, apoio = {
            apoioComResultado(estado.complemento, it.posicao, Formatacao.reais(it.posicao.saldo.valor, centavos = false))
        })
        NotaRitmoCenario()
        SemPlanilha(estado)
    }
    if (d.simbolicas.isNotEmpty()) {
        Secao("Posições simbólicas") {
            d.simbolicas.forEach { LinhaValor(it.ativo.nome, Formatacao.reais(it.saldo.valor)) }
        }
    }
    estado.recomendacao?.acoesEtfs?.let { NotasDoApp("Leitura do app", it) }
}

@Composable
fun AbaAlertas(
    estado: EstadoAnalise,
    d: AnaliseDeterministica,
) {
    if (d.alertas.isEmpty()) {
        CartaoAviso(Tom.POSITIVO, "✓ NENHUM ALERTA", "As regras de alerta não dispararam para esta carteira.")
    }
    val comentarios = estado.recomendacao?.comentariosAlertas.orEmpty()
    d.alertas.forEach { alerta ->
        CartaoAviso(alerta.severidade.estilo.tom, alerta.severidade.estilo.rotulo, textoAlerta(alerta)) {
            comentarios[alerta.regra]?.let {
                Text(it, style = Castanha.tipografia.legenda.copy(fontWeight = FontWeight.Normal), color = Castanha.cores.textMedium)
            }
        }
    }
}

private const val PRINCIPAIS = 3
