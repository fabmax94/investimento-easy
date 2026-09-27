package com.investimentoeasy.core.model

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Percentual expresso em pontos percentuais: `Percent.of("0.73")` significa 0,73%.
 */
@JvmInline
public value class Percent private constructor(
    public val pontos: BigDecimal,
) : Comparable<Percent> {
    /** Valor como fração (0,73% → 0,0073). */
    public val fracao: BigDecimal get() = pontos.divide(CEM, FRACTION_SCALE, RoundingMode.HALF_EVEN)

    public operator fun minus(outro: Percent): Percent = of(pontos - outro.pontos)

    public operator fun plus(outro: Percent): Percent = of(pontos + outro.pontos)

    public fun abs(): Percent = of(pontos.abs())

    override fun compareTo(other: Percent): Int = pontos.compareTo(other.pontos)

    override fun toString(): String = "${pontos.stripTrailingZeros().toPlainString()}%"

    public companion object {
        private const val SCALE = 6
        private const val FRACTION_SCALE = 10
        private val CEM = BigDecimal(100)

        public val ZERO: Percent = of(BigDecimal.ZERO)

        public fun of(pontos: BigDecimal): Percent = Percent(pontos.setScale(SCALE, RoundingMode.HALF_EVEN))

        public fun of(pontos: String): Percent = of(BigDecimal(pontos))

        public fun daFracao(fracao: BigDecimal): Percent = of(fracao * CEM)
    }
}
