package com.investimentoeasy.core.mercado

import com.investimentoeasy.core.model.InformeFii
import com.investimentoeasy.core.model.Percent
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import java.io.InputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.zip.ZipInputStream

/**
 * Informe mensal de FIIs dos dados abertos da CVM: um zip por ano (cerca de 1 MB) com três CSVs.
 * Do `geral` sai o ISIN (e dele a raiz do ticker); do `complemento`, o valor patrimonial da cota e
 * o dividend yield do mês (fração: 0,0117 = 1,17%).
 */
internal class Cvm(
    private val http: OkHttpClient,
    private val base: HttpUrl = "https://dados.cvm.gov.br".toHttpUrl(),
) {
    fun informes(
        raizes: Set<String>,
        hoje: LocalDate,
    ): Map<String, InformeFii> {
        if (raizes.isEmpty()) return emptyMap()
        val linhas = listOf(hoje.year - 1, hoje.year).flatMap { ano -> baixar(ano, raizes) }
        return linhas.groupBy { it.raiz }.mapNotNull { (raiz, doFundo) -> informe(raiz, doFundo) }.associateBy { it.raizTicker }
    }

    private fun baixar(
        ano: Int,
        raizes: Set<String>,
    ): List<Mes> {
        val url = base.newBuilder().addPathSegments("dados/FII/DOC/INF_MENSAL/DADOS/inf_mensal_fii_$ano.zip").build()
        val arquivos = http.abrir(url, ::lerZip)
        val raizPorCnpj =
            arquivos[GERAL].orEmpty().mapNotNull { campos ->
                val raiz = campos["Codigo_ISIN"]?.let(::raizDoIsin)?.takeIf { it in raizes } ?: return@mapNotNull null
                campos["CNPJ_Fundo_Classe"]?.let { it to raiz }
            }.toMap()
        return arquivos[COMPLEMENTO].orEmpty().mapNotNull { campos ->
            val raiz = raizPorCnpj[campos["CNPJ_Fundo_Classe"]] ?: return@mapNotNull null
            Mes(
                raiz = raiz,
                mes = campos["Data_Referencia"]?.let(LocalDate::parse) ?: return@mapNotNull null,
                versao = campos["Versao"]?.toIntOrNull() ?: 1,
                vp = campos["Valor_Patrimonial_Cotas"]?.toBigDecimalOrNull()?.takeIf { it.signum() > 0 },
                dy = campos["Percentual_Dividend_Yield_Mes"]?.toBigDecimalOrNull()?.takeIf { it.signum() >= 0 && it <= DY_MENSAL_MAXIMO },
            )
        }
    }

    /** Só lê os dois CSVs usados, linha a linha e só com as colunas pedidas. */
    private fun lerZip(entrada: InputStream): Map<String, List<Map<String, String>>> {
        val lidos = mutableMapOf<String, List<Map<String, String>>>()
        ZipInputStream(entrada).use { zip ->
            generateSequence { zip.nextEntry }.forEach { e ->
                val tipo = listOf(GERAL, COMPLEMENTO).firstOrNull { e.name.contains("_${it}_") } ?: return@forEach
                val leitor = zip.bufferedReader(Charsets.ISO_8859_1)
                val cabecalho = leitor.readLine()?.split(';') ?: return@forEach
                val indices = COLUNAS.associateWith(cabecalho::indexOf).filterValues { it >= 0 }
                lidos[tipo] =
                    leitor.lineSequence().map { linha -> linha.split(';') }
                        .map { campos -> indices.mapValues { (_, i) -> campos.getOrElse(i) { "" } } }
                        .toList()
            }
        }
        return lidos
    }

    private fun informe(
        raiz: String,
        meses: List<Mes>,
    ): InformeFii? {
        val porMes = meses.groupBy { it.mes }.mapValues { (_, v) -> v.maxBy { it.versao } }.toSortedMap()
        val ultimo = porMes.values.lastOrNull { it.vp != null } ?: return null
        val doze = porMes.values.filter { it.mes.isAfter(ultimo.mes.minusMonths(DOZE)) && !it.mes.isAfter(ultimo.mes) }.mapNotNull { it.dy }
        return InformeFii(
            raizTicker = raiz,
            mesReferencia = ultimo.mes,
            valorPatrimonialCota = ultimo.vp!!.setScale(2, RoundingMode.HALF_EVEN),
            dividendYield12Meses = doze.takeIf { it.size == DOZE.toInt() }?.let { pct(it.fold(BigDecimal.ZERO, BigDecimal::add)) },
            dividendYieldUltimoMes = ultimo.dy?.let(::pct),
            mesesNoDividendYield = doze.size,
        )
    }

    private fun pct(fracao: BigDecimal) = Percent.of(fracao.multiply(CEM).setScale(2, RoundingMode.HALF_EVEN))

    private data class Mes(
        val raiz: String,
        val mes: LocalDate,
        val versao: Int,
        val vp: BigDecimal?,
        val dy: BigDecimal?,
    )

    companion object {
        private const val GERAL = "geral"
        private const val COMPLEMENTO = "complemento"
        private const val DOZE = 12L
        private val CEM = BigDecimal(100)

        /** Acima de 20% ao mês é erro de preenchimento (ex.: % no lugar de fração): fica sem dado. */
        private val DY_MENSAL_MAXIMO = BigDecimal("0.2")
        private val COLUNAS =
            listOf(
                "CNPJ_Fundo_Classe",
                "Codigo_ISIN",
                "Data_Referencia",
                "Versao",
                "Valor_Patrimonial_Cotas",
                "Percentual_Dividend_Yield_Mes",
            )
        private val ISIN_DE_COTA = Regex("""^BR([A-Z]{4})CTF""")

        /** "BRKNCRCTF000" -> "KNCR". */
        fun raizDoIsin(isin: String): String? = ISIN_DE_COTA.find(isin.trim())?.groupValues?.get(1)
    }
}
