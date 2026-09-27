package com.investimentoeasy.core.designsystem

import com.investimentoeasy.core.model.Money
import com.investimentoeasy.core.model.Percent
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formatação pt-BR de dinheiro, percentual e data. Ausência de dado vira "—", nunca zero
 * (regra zero).
 */
public object Formatacao {
    public const val AUSENTE: String = "—"

    private val LOCALE = Locale.forLanguageTag("pt-BR")
    private val SIMBOLOS = DecimalFormatSymbols(LOCALE)
    private val DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy", LOCALE)
    private val DATA_CURTA = DateTimeFormatter.ofPattern("dd/MM", LOCALE)

    /** "R$ 1.234,56"; com `centavos = false`, "R$ 1.235". Negativo: "−R$ 1.234,56". */
    public fun reais(
        valor: Money?,
        centavos: Boolean = true,
        comSinal: Boolean = false,
    ): String {
        valor ?: return AUSENTE
        val casas = if (centavos) 2 else 0
        val numero = numero(valor.valor.abs().setScale(casas, RoundingMode.HALF_EVEN), casas)
        val sinal =
            when {
                valor.isNegative -> MENOS
                comSinal && !valor.isZero -> "+"
                else -> ""
            }
        return "${sinal}R$ $numero"
    }

    /** "1,80%"; com `comSinal`, "+1,80%". */
    public fun percentual(
        valor: Percent?,
        casas: Int = 2,
        comSinal: Boolean = false,
    ): String {
        valor ?: return AUSENTE
        val arredondado = valor.pontos.setScale(casas, RoundingMode.HALF_EVEN)
        val sinal =
            when {
                arredondado.signum() < 0 -> MENOS
                comSinal && arredondado.signum() > 0 -> "+"
                else -> ""
            }
        return "$sinal${numero(arredondado.abs(), casas)}%"
    }

    public fun data(valor: LocalDate?): String = valor?.format(DATA) ?: AUSENTE

    public fun dataCurta(valor: LocalDate?): String = valor?.format(DATA_CURTA) ?: AUSENTE

    private fun numero(
        valor: java.math.BigDecimal,
        casas: Int,
    ): String {
        val padrao = if (casas == 0) "#,##0" else "#,##0." + "0".repeat(casas)
        return DecimalFormat(padrao, SIMBOLOS).format(valor)
    }

    /** Sinal de menos tipográfico (U+2212), como no protótipo. */
    private const val MENOS = "−"
}
