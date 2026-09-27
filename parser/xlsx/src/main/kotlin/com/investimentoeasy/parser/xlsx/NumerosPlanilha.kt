package com.investimentoeasy.parser.xlsx

import com.investimentoeasy.core.model.Money
import com.investimentoeasy.core.model.Percent
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * A planilha da XP traz tudo como texto formatado em pt-BR (`R$ 1.234,56`, `12,34%`, `21/06/2026`).
 * `-`, `Indefinido` e vazio significam ausência e viram `null` (regra zero: nunca zero implícito).
 */
internal object NumerosPlanilha {
    private val AUSENTE = setOf("", "-", "indefinido", "n/a", "--")
    private val MILHAR = Regex("""\d{1,3}(\.\d{3})+""")
    private val DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun ausente(texto: String?): Boolean = texto == null || texto.trim().lowercase() in AUSENTE

    fun dinheiro(texto: String?): Money? {
        if (ausente(texto)) return null
        val limpo = texto!!.trim().replace(' ', ' ')
        val negativo = limpo.startsWith("-") || limpo.contains("R$ -") || limpo.contains("R$-")
        val numero = limpo.replace("R$", "").replace("-", "").trim()
        return decimalBr(numero)?.let { Money.of(if (negativo) it.negate() else it) }
    }

    fun percentual(texto: String?): Percent? {
        if (ausente(texto)) return null
        return decimalBr(texto!!.trim().removeSuffix("%").trim())?.let(Percent::of)
    }

    fun quantidade(texto: String?): BigDecimal? {
        if (ausente(texto)) return null
        val limpo = texto!!.trim()
        return when {
            limpo.contains(',') -> decimalBr(limpo)
            MILHAR.matches(limpo) -> limpo.replace(".", "").toBigDecimalOrNull()
            else -> limpo.toBigDecimalOrNull()
        }
    }

    fun data(texto: String?): LocalDate? {
        if (ausente(texto)) return null
        return try {
            LocalDate.parse(texto!!.trim(), DATA)
        } catch (_: DateTimeParseException) {
            null
        }
    }

    private fun decimalBr(texto: String): BigDecimal? = texto.replace(".", "").replace(",", ".").toBigDecimalOrNull()
}
