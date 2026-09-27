package com.investimentoeasy.core.domain.recomendacao

import com.investimentoeasy.core.domain.analise.Severidade
import com.investimentoeasy.core.domain.mercado.CicloJuros
import com.investimentoeasy.core.domain.recomendacao.TextoBr.pct
import com.investimentoeasy.core.domain.recomendacao.TextoBr.taxa
import com.investimentoeasy.core.model.Percent
import java.math.MathContext
import java.math.RoundingMode

/** Até três frases: desempenho contra o CDI, o maior risco e o ciclo de juros. */
internal fun veredicto(
    ctx: Contexto,
    diagnosticos: List<Diagnostico>,
): String = listOfNotNull(desempenho(ctx), risco(ctx, diagnosticos), fraseDoCiclo(ctx)).joinToString(" ")

private fun desempenho(ctx: Contexto): String? {
    val c = ctx.snapshot.contexto ?: return null
    val carteira = c.referencia("Portfólio")?.vinteQuatroMeses ?: c.resumo?.rentabilidade24Meses
    val cdi = c.referencia("CDI")?.vinteQuatroMeses
    if (carteira == null || cdi == null || cdi.pontos.signum() <= 0) return null
    val doCdi = Percent.of(carteira.pontos.divide(cdi.pontos, MathContext.DECIMAL64).multiply(CEM).setScale(0, RoundingMode.HALF_EVEN))
    val leitura = if (doCdi >= Percent.of("100")) "a performance não é o problema" else "a carteira ficou abaixo do CDI"
    return "Em 24 meses a carteira rendeu ${pct(carteira)}, ${pct(doCdi, 0)} do CDI: $leitura."
}

private fun risco(
    ctx: Contexto,
    diagnosticos: List<Diagnostico>,
): String {
    val alerta = ctx.analise.alertas.firstOrNull { it.severidade == Severidade.URGENTE || it.severidade == Severidade.CRITICO }
    val critico = diagnosticos.firstOrNull { it.status == StatusDiagnostico.CRITICO }
    return when {
        alerta != null -> "O ponto que mais pede atenção: ${alerta.titulo.replaceFirstChar { it.lowercase() }}."
        critico != null -> "O ponto que mais pede atenção: ${critico.texto.replaceFirstChar { it.lowercase() }}"
        else -> "Nenhum alerta urgente ou crítico; os ajustes são de alocação."
    }
}

private fun fraseDoCiclo(ctx: Contexto): String? {
    val juros = ctx.juros ?: return null
    val direcao =
        when (juros.ciclo) {
            CicloJuros.CORTE -> "ciclo de corte"
            CicloJuros.ALTA -> "ciclo de alta"
            CicloJuros.ESTAVEL -> "juros estáveis"
        }
    return "O Focus projeta Selic de ${taxa(
        juros.selicAtual,
    )} hoje para ${taxa(juros.selicEsperada)} no fim de ${juros.anoEsperado}: $direcao."
}

private val CEM = java.math.BigDecimal(100)
