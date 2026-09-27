package com.investimentoeasy.parser.xperformance

import com.investimentoeasy.core.model.Money
import com.investimentoeasy.core.model.Percent
import java.math.BigDecimal

/**
 * Conversão dos formatos numéricos do XPerformance.
 *
 * - Dinheiro e percentuais usam o padrão brasileiro: `R$ 1.971,96`, `-R$ 14.293,46`, `22.160,52%`.
 * - Quantidades usam ponto como separador decimal: `6330.53`, `26.34`, `4`.
 * - `-` significa ausência de dado e vira `null` (regra zero: nunca zero implícito).
 */
internal object NumerosBr {
    const val DINHEIRO = """-?R\$ [\d.]+,\d{2}"""
    const val PERCENTUAL = """(?:-?[\d.]+,\d+%|-)"""
    const val QUANTIDADE = """(?:[\d]+(?:\.\d+)?|-)"""

    fun dinheiro(texto: String): Money {
        val limpo = texto.trim()
        val negativo = limpo.startsWith("-")
        val numero = limpo.removePrefix("-").removePrefix("R$").trim()
        val valor = decimalBr(numero)
        return Money.of(if (negativo) valor.negate() else valor)
    }

    fun percentual(texto: String): Percent? {
        val limpo = texto.trim()
        if (limpo == "-") return null
        return Percent.of(decimalBr(limpo.removeSuffix("%")))
    }

    fun quantidade(texto: String): BigDecimal? {
        val limpo = texto.trim()
        if (limpo == "-") return null
        return BigDecimal(limpo)
    }

    private fun decimalBr(texto: String): BigDecimal = BigDecimal(texto.replace(".", "").replace(",", "."))
}
