package com.investimentoeasy.core.mercado

import com.investimentoeasy.core.model.FonteMercado
import com.investimentoeasy.core.model.ValorDeMercado
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Séries do SGS do Banco Central: `[{"data":"dd/MM/yyyy","valor":"13.75"}]`. */
internal class BancoCentral(
    private val http: OkHttpClient,
    private val base: HttpUrl = "https://api.bcb.gov.br".toHttpUrl(),
) {
    /**
     * Último valor com data até [hoje], pedindo uma janela de datas: a série da meta Selic vem
     * preenchida até a próxima reunião do Copom, então "últimos N" traria só datas futuras.
     */
    fun ultimo(
        serie: Int,
        hoje: LocalDate,
    ): ValorDeMercado? {
        val url =
            base
                .newBuilder()
                .addPathSegments("dados/serie/bcdata.sgs.$serie/dados")
                .addQueryParameter("formato", "json")
                .addQueryParameter("dataInicial", hoje.minusDays(JANELA_DIAS).format(DATA))
                .addQueryParameter("dataFinal", hoje.format(DATA))
                .build()
        return http
            .json(url)
            .jsonArray
            .mapNotNull { item ->
                val o = item.jsonObject
                val data = o["data"]?.jsonPrimitive?.content?.let { LocalDate.parse(it, DATA) } ?: return@mapNotNull null
                val valor = o["valor"]?.jsonPrimitive?.content?.toBigDecimalOrNull() ?: return@mapNotNull null
                ValorDeMercado(valor, data, FonteMercado.BANCO_CENTRAL)
            }.filter { !it.data.isAfter(hoje) }
            .maxByOrNull { it.data }
    }

    companion object {
        const val SELIC_META = 432
        const val IPCA_12_MESES = 13522
        const val DOLAR_PTAX_VENDA = 1

        /** Cobre séries mensais (IPCA 12 meses sai com ~45 dias de defasagem). */
        private const val JANELA_DIAS = 100L
        private val DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    }
}
