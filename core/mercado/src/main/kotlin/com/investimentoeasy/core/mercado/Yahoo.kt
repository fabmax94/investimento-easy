package com.investimentoeasy.core.mercado

import com.investimentoeasy.core.model.Cotacao
import com.investimentoeasy.core.model.Percent
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Cotações do Yahoo Finance (endpoint público, não oficial) com um ano de histórico diário.
 * Os retornos usam o fechamento ajustado (com proventos) quando existe.
 */
internal class Yahoo(
    private val http: OkHttpClient,
    private val base: HttpUrl = "https://query1.finance.yahoo.com".toHttpUrl(),
) {
    fun cotacao(simbolo: String): Cotacao? {
        val url =
            base
                .newBuilder()
                .addPathSegments("v8/finance/chart")
                .addPathSegment(simbolo)
                .addQueryParameter("range", "1y")
                .addQueryParameter("interval", "1d")
                .build()
        val resultado = http.json(url).jsonObject["chart"]?.jsonObject?.get("result")?.let { it as? JsonArray }?.firstOrNull()?.jsonObject
        return resultado?.let { cotacaoDe(simbolo, it) }
    }

    private fun cotacaoDe(
        simbolo: String,
        resultado: JsonObject,
    ): Cotacao? {
        val meta = resultado["meta"]?.jsonObject
        val preco = meta?.numero("regularMarketPrice")
        val data = meta?.numero("regularMarketTime")?.let { diaDe(it.toLong()) }
        if (preco == null || data == null) return null
        val serie = serie(resultado)
        val atual = serie.lastOrNull()?.second
        return Cotacao(
            simbolo = simbolo,
            preco = preco,
            data = data,
            retorno3Meses = retorno(serie, atual, data.minusMonths(TRES)),
            retorno6Meses = retorno(serie, atual, data.minusMonths(SEIS)),
            retorno12Meses = retorno(serie, atual, data.minusMonths(DOZE)),
            maxima52Semanas = meta.numero("fiftyTwoWeekHigh"),
        )
    }

    /** Pares (dia, fechamento), pulando dias sem valor. */
    private fun serie(resultado: JsonObject): List<Pair<LocalDate, BigDecimal>> {
        val tempos = resultado["timestamp"]?.jsonArray.orEmpty()
        val indicadores = resultado["indicators"]?.jsonObject
        val ajustado = indicadores?.get("adjclose")?.jsonArray?.firstOrNull()?.jsonObject?.get("adjclose")?.jsonArray
        val fechamento = indicadores?.get("quote")?.jsonArray?.firstOrNull()?.jsonObject?.get("close")?.jsonArray
        val valores = ajustado ?: fechamento ?: return emptyList()
        return tempos.indices.mapNotNull { i ->
            val valor = valores.getOrNull(i)?.takeIf { it !is JsonNull }?.jsonPrimitive?.content?.toBigDecimalOrNull()
            val dia = tempos[i].jsonPrimitive.content.toLongOrNull()?.let(::diaDe)
            if (valor == null || dia == null) null else dia to valor
        }
    }

    /** Retorno do último ponto sobre o último ponto até a [data]; `null` se o histórico não chega lá. */
    private fun retorno(
        serie: List<Pair<LocalDate, BigDecimal>>,
        atual: BigDecimal?,
        data: LocalDate,
    ): Percent? {
        if (atual == null || serie.isEmpty() || serie.first().first.isAfter(data.plusDays(TOLERANCIA_DIAS))) return null
        val antes = serie.lastOrNull { !it.first.isAfter(data) }?.second ?: serie.first().second
        if (antes.signum() == 0) return null
        val pontos = atual.divide(antes, MathContext.DECIMAL64).subtract(BigDecimal.ONE).multiply(CEM)
        return Percent.of(pontos.setScale(2, RoundingMode.HALF_EVEN))
    }

    private fun JsonObject.numero(chave: String): BigDecimal? =
        get(chave)?.takeIf { it !is JsonNull }?.jsonPrimitive?.content?.toBigDecimalOrNull()

    private fun diaDe(epochSegundos: Long): LocalDate = Instant.ofEpochSecond(epochSegundos).atZone(FUSO).toLocalDate()

    companion object {
        const val IBOVESPA = "^BVSP"
        const val IFIX_ETF = "XFIX11.SA"
        private const val TRES = 3L
        private const val SEIS = 6L
        private const val DOZE = 12L
        private const val TOLERANCIA_DIAS = 7L
        private val CEM = BigDecimal(100)
        private val FUSO = ZoneId.of("America/Sao_Paulo")

        fun simboloB3(ticker: String): String = "${ticker.uppercase()}.SA"
    }
}
