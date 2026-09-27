package com.investimentoeasy.core.domain.recomendacao

import com.investimentoeasy.core.model.Money
import com.investimentoeasy.core.model.Percent
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Números dos textos no padrão brasileiro. A UI tem o próprio formatador; este é o do domínio. */
internal object TextoBr {
    private val LOCALE = Locale.forLanguageTag("pt-BR")
    private val SIMBOLOS = DecimalFormatSymbols(LOCALE)
    private val DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy", LOCALE)
    private val MES_ANO = DateTimeFormatter.ofPattern("MM/yyyy", LOCALE)

    /** "R$ 12.345" (sem centavos: texto de análise, não extrato). */
    fun reais(valor: Money): String {
        val sinal = if (valor.isNegative) "−" else ""
        return "${sinal}R$ ${numero(valor.valor.abs(), 0)}"
    }

    /** "12,3%" com [casas] decimais. */
    fun pct(
        valor: Percent,
        casas: Int = 1,
    ): String = "${if (valor.pontos.signum() < 0) "−" else ""}${numero(valor.pontos.abs(), casas)}%"

    fun pctComSinal(
        valor: Percent,
        casas: Int = 1,
    ): String = if (valor.pontos.signum() > 0) "+${pct(valor, casas)}" else pct(valor, casas)

    fun taxa(valor: BigDecimal): String = "${numero(valor, 2)}%"

    fun decimal(
        valor: BigDecimal,
        casas: Int = 2,
    ): String = numero(valor, casas)

    fun data(valor: LocalDate): String = valor.format(DATA)

    fun mesAno(valor: LocalDate): String = valor.format(MES_ANO)

    private fun numero(
        valor: BigDecimal,
        casas: Int,
    ): String {
        val padrao = if (casas == 0) "#,##0" else "#,##0." + "0".repeat(casas)
        return DecimalFormat(padrao, SIMBOLOS).format(valor.setScale(casas, RoundingMode.HALF_EVEN))
    }
}
