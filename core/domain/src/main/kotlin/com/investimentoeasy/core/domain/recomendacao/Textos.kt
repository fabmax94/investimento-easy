package com.investimentoeasy.core.domain.recomendacao

import com.investimentoeasy.core.domain.alocacao.GrupoAlocacao
import com.investimentoeasy.core.domain.mercado.CicloJuros
import com.investimentoeasy.core.domain.mercado.DirecaoCambio
import com.investimentoeasy.core.domain.recomendacao.TextoBr.pct
import com.investimentoeasy.core.domain.recomendacao.TextoBr.pctComSinal
import com.investimentoeasy.core.domain.recomendacao.TextoBr.reais
import com.investimentoeasy.core.domain.recomendacao.TextoBr.taxa
import com.investimentoeasy.core.model.soma

/** Até cinco itens ligando cada indicador ao que ele faz com esta carteira, em R$. */
internal fun textosDeMercado(ctx: Contexto): List<String> {
    if (ctx.panorama == null) return emptyList()
    return listOfNotNull(textoDeJuros(ctx), textoDeInflacao(ctx), textoDeCambio(ctx), textoDeBolsa(ctx), textoDeFiis(ctx))
}

private fun Contexto.valorEm(vararg grupos: GrupoAlocacao) = grupos.map { valorDo(it) }.soma()

private fun textoDeJuros(ctx: Contexto): String? {
    val juros = ctx.juros ?: return null
    val pos = reais(ctx.valorEm(GrupoAlocacao.RENDA_FIXA_POS))
    val marcacao = reais(ctx.valorEm(GrupoAlocacao.RENDA_FIXA_IPCA, GrupoAlocacao.RENDA_FIXA_PRE))
    val selic = "Selic de ${taxa(juros.selicAtual)} com o Focus em ${taxa(juros.selicEsperada)} no fim de ${juros.anoEsperado}"
    return when (juros.ciclo) {
        CicloJuros.CORTE ->
            "Juros: $selic. Seus $pos em pós-fixado tendem a render menos; os $marcacao em IPCA+ e prefixado tendem a ganhar marcação."
        CicloJuros.ALTA ->
            "Juros: $selic. Seus $pos em pós-fixado ganham carrego; os $marcacao em IPCA+ e prefixado oscilam para baixo no curto prazo."
        CicloJuros.ESTAVEL -> "Juros: $selic, sem ciclo claro. Seus $pos em pós-fixado mantêm o carrego."
    }
}

private fun textoDeInflacao(ctx: Contexto): String? {
    val ipca = ctx.panorama?.ipca12Meses ?: return null
    val esperado = ctx.panorama.focus?.ipcaDoAno?.get(ctx.hoje.year)?.let { " e Focus em ${taxa(it)} para ${ctx.hoje.year}" }.orEmpty()
    val protecao = "${reais(ctx.valorEm(GrupoAlocacao.RENDA_FIXA_IPCA))} em IPCA+ (${pct(ctx.percentualDo(GrupoAlocacao.RENDA_FIXA_IPCA))})"
    return "Inflação: IPCA de ${taxa(ipca.valor)} em 12 meses$esperado. A proteção da carteira são $protecao."
}

private fun textoDeCambio(ctx: Contexto): String? {
    val cambio = ctx.cambio ?: return null
    val leitura =
        when (cambio.direcao) {
            DirecaoCambio.REAL_MAIS_FRACO -> "o que ajuda o retorno em reais"
            DirecaoCambio.REAL_MAIS_FORTE -> "o que corta o retorno em reais"
            DirecaoCambio.ESTAVEL -> "sem efeito relevante esperado do câmbio"
        }
    val dolar = "R$ ${TextoBr.decimal(
        cambio.dolarAtual,
    )} hoje e R$ ${TextoBr.decimal(cambio.dolarEsperado)} no Focus para o fim de ${cambio.ano}"
    return "Dólar: $dolar, $leitura dos seus ${reais(ctx.valorEm(GrupoAlocacao.EXTERIOR))} no exterior."
}

private fun textoDeBolsa(ctx: Contexto): String? {
    val ibov = ctx.panorama?.ibovespa?.retorno12Meses ?: return null
    val acoes = "${reais(ctx.valorEm(GrupoAlocacao.ACOES_BRASIL))} em ações Brasil (${pct(ctx.percentualDo(GrupoAlocacao.ACOES_BRASIL))})"
    return "Bolsa: Ibovespa ${pctComSinal(ibov)} em 12 meses; você tem $acoes."
}

private fun textoDeFiis(ctx: Contexto): String? {
    val ifix = ctx.panorama?.ifix?.retorno12Meses ?: return null
    return "FIIs: IFIX (pelo XFIX11) ${pctComSinal(ifix)} em 12 meses; seus ${reais(ctx.valorEm(GrupoAlocacao.FIIS))} em FIIs " +
        "estão detalhados na aba FIIs."
}
