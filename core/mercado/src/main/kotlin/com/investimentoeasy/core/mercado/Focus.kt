package com.investimentoeasy.core.mercado

import com.investimentoeasy.core.model.ExpectativasFocus
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import java.math.BigDecimal
import java.time.LocalDate

/** Expectativas anuais do Boletim Focus (API Olinda do Banco Central), mediana de todos os respondentes. */
internal class Focus(
    private val http: OkHttpClient,
    private val base: HttpUrl = "https://olinda.bcb.gov.br".toHttpUrl(),
) {
    fun expectativas(hoje: LocalDate): ExpectativasFocus? {
        val desde = hoje.minusDays(JANELA_DIAS)
        val url =
            base
                .newBuilder()
                .addPathSegments("olinda/servico/Expectativas/versao/v1/odata/ExpectativasMercadoAnuais")
                .addQueryParameter(
                    "\$filter",
                    "(Indicador eq 'Selic' or Indicador eq 'IPCA' or Indicador eq 'Câmbio') and baseCalculo eq 0 and Data ge '$desde'",
                ).addQueryParameter("\$select", "Indicador,Data,DataReferencia,Mediana")
                .addQueryParameter("\$format", "json")
                .build()
        val linhas =
            http.json(url).jsonObject["value"]?.jsonArray.orEmpty().mapNotNull { item ->
                val o = item.jsonObject
                Linha(
                    indicador = o["Indicador"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                    data = o["Data"]?.jsonPrimitive?.content?.let(LocalDate::parse) ?: return@mapNotNull null,
                    ano = o["DataReferencia"]?.jsonPrimitive?.content?.toIntOrNull() ?: return@mapNotNull null,
                    mediana = o["Mediana"]?.jsonPrimitive?.content?.toBigDecimalOrNull() ?: return@mapNotNull null,
                )
            }
        val pesquisa = linhas.maxOfOrNull { it.data } ?: return null
        val daPesquisa = linhas.filter { it.data == pesquisa }

        fun de(indicador: String) = daPesquisa.filter { it.indicador == indicador }.associate { it.ano to it.mediana }.toSortedMap()
        return ExpectativasFocus(pesquisa, de("Selic"), de("IPCA"), de("Câmbio"))
    }

    private data class Linha(
        val indicador: String,
        val data: LocalDate,
        val ano: Int,
        val mediana: BigDecimal,
    )

    private companion object {
        const val JANELA_DIAS = 21L
    }
}
