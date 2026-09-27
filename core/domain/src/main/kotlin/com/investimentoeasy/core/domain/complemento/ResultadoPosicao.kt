package com.investimentoeasy.core.domain.complemento

import com.investimentoeasy.core.model.Money
import com.investimentoeasy.core.model.Percent
import com.investimentoeasy.core.model.Posicao
import java.math.BigDecimal
import java.math.RoundingMode

/** De onde veio o custo usado no resultado. */
public enum class BaseDoCusto {
    /** Preço médio da planilha × quantidade da base (ações, ETFs, FIIs). */
    PRECO_MEDIO,

    /** Valor aplicado da planilha (fundos e renda fixa). */
    VALOR_APLICADO,
}

/**
 * Resultado não realizado estimado de uma posição: saldo da base menos o custo que a planilha
 * informa. É estimativa (origem CALCULADO): a planilha pode ser de outra data.
 */
public data class ResultadoPosicao(
    val custo: Money,
    val resultado: Money,
    val percentual: Percent,
    val baseDoCusto: BaseDoCusto,
)

/**
 * `null` quando falta dado (regra zero) ou quando a quantidade mudou entre a planilha e a base:
 * nesse caso o custo da planilha não vale para a posição de hoje.
 */
public fun resultadoDe(
    posicao: Posicao,
    dados: DadosDaPlanilha?,
): ResultadoPosicao? {
    if (dados == null || dados.quantidadeMudou) return null
    val quantidade = posicao.quantidade?.valor
    val (custo, base) =
        when {
            dados.precoMedio != null && quantidade != null -> dados.precoMedio * quantidade to BaseDoCusto.PRECO_MEDIO
            dados.valorAplicado != null -> dados.valorAplicado to BaseDoCusto.VALOR_APLICADO
            else -> return null
        }
    if (custo.isZero || custo.isNegative) return null
    val resultado = posicao.saldo.valor - custo
    val percentual = resultado.valor.multiply(CEM).divide(custo.valor, ESCALA, RoundingMode.HALF_EVEN)
    return ResultadoPosicao(custo, resultado, Percent.of(percentual), base)
}

private val CEM = BigDecimal(100)
private const val ESCALA = 2
