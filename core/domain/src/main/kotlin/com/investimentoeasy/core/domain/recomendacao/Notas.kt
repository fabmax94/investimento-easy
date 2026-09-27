package com.investimentoeasy.core.domain.recomendacao

import com.investimentoeasy.core.domain.classificacao.ClassificadorAtivos
import com.investimentoeasy.core.domain.mercado.FiiNoMercado
import com.investimentoeasy.core.domain.mercado.MotivoQueda
import com.investimentoeasy.core.domain.mercado.distanciaDaMaxima
import com.investimentoeasy.core.domain.recomendacao.TextoBr.decimal
import com.investimentoeasy.core.domain.recomendacao.TextoBr.mesAno
import com.investimentoeasy.core.domain.recomendacao.TextoBr.pct
import com.investimentoeasy.core.domain.recomendacao.TextoBr.pctComSinal
import com.investimentoeasy.core.model.ChaveAtivo
import com.investimentoeasy.core.model.ClasseAtivo
import com.investimentoeasy.core.model.Percent
import com.investimentoeasy.core.model.TipoAtivo
import java.math.BigDecimal

internal fun notasDeFundos(ctx: Contexto): List<String> {
    val fundos = ctx.snapshot.posicoes.filter { it.ativo.tipo == TipoAtivo.FUNDO }
    if (fundos.isEmpty()) return emptyList()
    val abaixo =
        ctx.analise.fundosAbaixoDoCdi.map { a ->
            "${a.posicao.ativo.nome} está em ${a.posicao.percentualCdiAno?.valor?.let { pct(it, 0) }} do CDI no ano: " +
                "confira se a taxa de administração ainda se justifica."
        }
    val isentos =
        fundos.filter { ClassificadorAtivos.normalizar(it.ativo.nome).let { n -> "INCENTIVAD" in n || "INFRA" in n } }.map {
            "${it.ativo.nome} é de infraestrutura (debêntures incentivadas): rendimento isento de IR para pessoa física."
        }
    val comeCotas =
        "Fundos de renda fixa e multimercado têm come-cotas em maio e novembro (IR antecipado sobre o rendimento); " +
            "fundos de ações não."
    return abaixo + isentos + comeCotas
}

internal fun notasDeFiis(ctx: Contexto): List<String> {
    if (ctx.snapshot.posicoes.none { it.ativo.tipo == TipoAtivo.FII }) return emptyList()
    if (ctx.panorama == null) return emptyList()
    val porFii = ctx.fiis.map { nota(ctx, it) }
    val tributacao =
        "Tributação: ganho de capital em FII paga 20% sem a isenção de R$ 20 mil (ela vale só para ações); " +
            "prejuízo em FII compensa ganho futuro em FII. Rendimentos mensais são isentos."
    return porFii + tributacao
}

private fun nota(
    ctx: Contexto,
    fii: FiiNoMercado,
): String {
    val desconto =
        when {
            fii.pvp < BigDecimal.ONE -> "desconto de ${pct(fii.ateOValorPatrimonial)} até o valor patrimonial"
            fii.pvp > BigDecimal.ONE -> "prêmio sobre o valor patrimonial"
            else -> "no valor patrimonial"
        }
    val dy =
        fii.dividendYield12Meses?.let { dy ->
            val cdi = ctx.cdi12Meses?.let { " (CDI 12m: ${pct(it)})" }.orEmpty()
            "DY 12m de ${pct(dy)}$cdi"
        }
    val queda =
        when (fii.motivoQueda) {
            MotivoQueda.ESPECIFICA -> "caiu bem mais que o IFIX em 12 meses: investigue antes de decidir"
            MotivoQueda.CICLICA -> "caiu junto com o setor (efeito de juros): vender agora realiza uma queda que costuma reverter"
            else -> null
        }
    val renda = "a renda do último mês está bem abaixo da média de 12 meses".takeIf { fii.distribuicaoEncolhendo }
    val partes =
        listOfNotNull(
            "P/VP ${decimal(fii.pvp)} (VP de ${mesAno(fii.mesReferenciaVp)}), $desconto",
            dy,
            queda,
            renda,
        )
    return "${fii.ticker}: ${partes.joinToString("; ")}."
}

internal fun notasDeAcoesEtfs(ctx: Contexto): List<String> {
    val p = ctx.panorama ?: return emptyList()
    val ibov = p.ibovespa?.retorno12Meses
    val renda = ctx.snapshot.posicoes.filter { it.ativo.tipo in setOf(TipoAtivo.ACAO, TipoAtivo.ETF, TipoAtivo.BDR) }
    val porPapel =
        renda.mapNotNull { pos ->
            val ticker = (pos.ativo.chave as? ChaveAtivo.Ticker)?.codigo ?: return@mapNotNull null
            val c = p.cotacao(ticker) ?: return@mapNotNull null
            val retornos =
                listOfNotNull(
                    c.retorno3Meses?.let { "3m ${pctComSinal(it)}" },
                    c.retorno6Meses?.let { "6m ${pctComSinal(it)}" },
                    c.retorno12Meses?.let { "12m ${pctComSinal(it)}" },
                ).joinToString(", ")
            val maxima =
                distanciaDaMaxima(
                    c,
                )?.takeIf { it < LONGE_DA_MAXIMA }?.let { "; ${pct(it.abs())} abaixo da máxima de 52 semanas" }.orEmpty()
            val contraIbov =
                if (ibov != null && c.retorno12Meses != null && pos.ativo.classe in RENDA_VARIAVEL_BRASIL) {
                    val diferenca = c.retorno12Meses!! - ibov
                    "; ${pctComSinal(diferenca)} contra o Ibovespa em 12 meses"
                } else {
                    ""
                }
            "$ticker: $retornos$contraIbov$maxima."
        }
    val taxa =
        "BOVA11 cobra 0,10% ao ano e PIBB11 0,059% (confira as taxas atuais): para a mesma exposição, a taxa menor rende mais.".takeIf {
            ctx.temTicker("BOVA11")
        }
    return porPapel + listOfNotNull(taxa)
}

private val LONGE_DA_MAXIMA = Percent.of("-10")

/** Só faz sentido comparar com o Ibovespa o que é bolsa brasileira (ETF de renda fixa não). */
private val RENDA_VARIAVEL_BRASIL = setOf(ClasseAtivo.ACAO, ClasseAtivo.ETF, ClasseAtivo.FUNDO_ACOES)
