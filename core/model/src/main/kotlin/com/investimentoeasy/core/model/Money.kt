package com.investimentoeasy.core.model

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Valor monetário em reais com 2 casas decimais e arredondamento bancário.
 *
 * Nunca usamos `Double` para dinheiro: toda operação é feita em [BigDecimal].
 */
@JvmInline
public value class Money private constructor(
    public val valor: BigDecimal,
) : Comparable<Money> {
    public operator fun plus(outro: Money): Money = of(valor + outro.valor)

    public operator fun minus(outro: Money): Money = of(valor - outro.valor)

    public operator fun unaryMinus(): Money = of(valor.negate())

    public operator fun times(fator: BigDecimal): Money = of(valor * fator)

    public fun abs(): Money = of(valor.abs())

    public val isZero: Boolean get() = valor.signum() == 0

    public val isNegative: Boolean get() = valor.signum() < 0

    /** Fração (0..1, sem escala de %) deste valor sobre [total]; `null` se o total for zero. */
    public fun fracaoDe(total: Money): BigDecimal? =
        if (total.isZero) null else valor.divide(total.valor, FRACTION_SCALE, RoundingMode.HALF_EVEN)

    override fun compareTo(other: Money): Int = valor.compareTo(other.valor)

    override fun toString(): String = "R$ ${valor.toPlainString()}"

    public companion object {
        private const val SCALE = 2
        private const val FRACTION_SCALE = 10

        public val ZERO: Money = of(BigDecimal.ZERO)

        public fun of(valor: BigDecimal): Money = Money(valor.setScale(SCALE, RoundingMode.HALF_EVEN))

        public fun of(valor: String): Money = of(BigDecimal(valor))

        public fun of(valor: Long): Money = of(BigDecimal.valueOf(valor))
    }
}

public fun Iterable<Money>.soma(): Money = fold(Money.ZERO) { acc, m -> acc + m }
