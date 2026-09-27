package com.investimentoeasy.core.domain.recomendacao

import com.investimentoeasy.core.domain.alocacao.GrupoAlocacao
import com.investimentoeasy.core.domain.recomendacao.TextoBr.pct
import com.investimentoeasy.core.model.Percent

/** Um grupo fora da faixa do perfil: quanto sobra ou falta, em pontos da carteira. */
internal data class Desvio(
    val grupo: GrupoAlocacao,
    val atual: Percent,
    val alvo: Percent,
    val minimo: Percent,
    val maximo: Percent,
) {
    val acimaDoMaximo: Boolean get() = atual > maximo
    val abaixoDoMinimo: Boolean get() = atual < minimo
    val excesso: Percent get() = atual - maximo
    val falta: Percent get() = alvo - atual
}

internal fun desvios(ctx: Contexto): List<Desvio> =
    ctx.alvos.map { (grupo, faixa) -> Desvio(grupo, ctx.percentualDo(grupo), faixa.alvo, faixa.minimo, faixa.maximo) }

/** Diagnóstico por grupo contra a faixa do perfil; muito acima do máximo é crítico. */
internal fun diagnosticos(ctx: Contexto): List<Diagnostico> =
    desvios(ctx).map { d ->
        val faixa = "faixa do perfil ${ctx.perfil.rotulo.lowercase()}: ${pct(d.minimo, 0)} a ${pct(d.maximo, 0)}"
        val base = "${d.grupo.rotulo} em ${pct(d.atual)} ($faixa)"
        when {
            d.acimaDoMaximo && d.excesso > MUITO_ACIMA -> Diagnostico(StatusDiagnostico.CRITICO, "$base: bem acima do máximo.")
            d.acimaDoMaximo -> Diagnostico(StatusDiagnostico.ATENCAO, "$base: acima do máximo.")
            d.abaixoDoMinimo -> Diagnostico(StatusDiagnostico.ATENCAO, "$base: abaixo do mínimo.")
            else -> Diagnostico(StatusDiagnostico.OK, "$base.")
        }
    }

private val MUITO_ACIMA = Percent.of("10")
