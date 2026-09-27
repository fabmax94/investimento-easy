package com.investimentoeasy.core.mercado

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.ZoneId
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Servidor falso com respostas no formato real de cada fonte (dados inventados). */
internal class ServidorDeMercado : Dispatcher() {
    val servidor = MockWebServer().apply { dispatcher = this@ServidorDeMercado }
    val pedidos = mutableListOf<RecordedRequest>()

    /** Caminhos que devem falhar, com o código HTTP. */
    val falhar = mutableMapOf<String, Int>()

    fun url() = servidor.url("/")

    override fun dispatch(request: RecordedRequest): MockResponse {
        pedidos += request
        val caminho = request.requestUrl!!.encodedPath
        falhar.entries.firstOrNull { caminho.contains(it.key) }?.let { return MockResponse().setResponseCode(it.value) }
        return when {
            caminho.contains(
                "bcdata.sgs.432",
            ) ->
                json(
                    SELIC,
                )
            caminho.contains("bcdata.sgs.13522") -> json("""[{"data":"01/07/2026","valor":"5.10"},{"data":"01/08/2026","valor":"4.85"}]""")
            caminho.contains("bcdata.sgs.1/") -> json("""[{"data":"25/09/2026","valor":"5.3120"}]""")
            caminho.contains("ExpectativasMercadoAnuais") -> json(FOCUS)
            caminho.contains("/v8/finance/chart/") -> yahoo(request.requestUrl!!.pathSegments.last())
            caminho.contains("inf_mensal_fii_2025.zip") -> zip(2025)
            caminho.contains("inf_mensal_fii_2026.zip") -> zip(2026)
            else -> MockResponse().setResponseCode(404)
        }
    }

    private fun json(corpo: String) = MockResponse().setHeader("content-type", "application/json").setBody(corpo)

    /** Um ano de pregões: preço sobe de 100 a 120 em linha reta; o XYZW11 não tem histórico. */
    private fun yahoo(simbolo: String): MockResponse {
        if (simbolo == "XYZW11.SA") return json("""{"chart":{"result":null,"error":{"code":"Not Found"}}}""")
        val fim = LocalDate.of(2026, 9, 25)
        val dias = (0L..365L).map { fim.minusDays(365 - it) }.filter { it.dayOfWeek.value <= 5 }
        val tempos = dias.map { it.atTime(18, 0).atZone(ZoneId.of("America/Sao_Paulo")).toEpochSecond() }
        val precos = dias.indices.map { i -> 100.0 + 20.0 * i / (dias.size - 1) }
        val fechamentos = precos.mapIndexed { i, p -> if (i == 5) "null" else "%.4f".format(java.util.Locale.ROOT, p) }
        return json(
            """{"chart":{"result":[{"meta":{"currency":"BRL","symbol":"$simbolo","regularMarketPrice":120.5,""" +
                """"regularMarketTime":${tempos.last()},"fiftyTwoWeekHigh":125.0},"timestamp":[${tempos.joinToString(",")}],""" +
                """"indicators":{"quote":[{"close":[${fechamentos.joinToString(",")}]}],""" +
                """"adjclose":[{"adjclose":[${fechamentos.joinToString(",")}]}]}}],"error":null}}""",
        )
    }

    private fun zip(ano: Int): MockResponse {
        val geral =
            "Tipo_Fundo_Classe;CNPJ_Fundo_Classe;Data_Referencia;Versao;Nome_Fundo_Classe;Codigo_ISIN\n" +
                "Classe;11.111.111/0001-11;$ano-01-01;1;FUNDO EXEMPLO FII;BRKNCRCTF000\n" +
                "Classe;22.222.222/0001-22;$ano-01-01;1;OUTRO FUNDO FII;BROUTRCTF006\n"
        val meses = if (ano == 2025) (1..12) else (1..8)
        val complemento =
            buildString {
                append("CNPJ_Fundo_Classe;Data_Referencia;Versao;Valor_Patrimonial_Cotas;Percentual_Dividend_Yield_Mes\n")
                meses.forEach { m ->
                    val mes = "%d-%02d-01".format(ano, m)
                    append("11.111.111/0001-11;$mes;1;100.1234;0.0100\n")
                    if (ano == 2026 && m == 8) append("11.111.111/0001-11;$mes;2;102.5000;0.0110\n")
                    append("22.222.222/0001-22;$mes;1;50.0;0.0080\n")
                }
            }
        val saida = ByteArrayOutputStream()
        ZipOutputStream(saida).use { z ->
            listOf("inf_mensal_fii_geral_$ano.csv" to geral, "inf_mensal_fii_complemento_$ano.csv" to complemento).forEach {
                    (nome, texto) ->
                z.putNextEntry(ZipEntry(nome))
                z.write(texto.toByteArray(Charsets.ISO_8859_1))
                z.closeEntry()
            }
        }
        return MockResponse().setBody(Buffer().write(saida.toByteArray()))
    }

    private companion object {
        const val SELIC =
            """[{"data":"10/09/2026","valor":"14.25"},{"data":"18/09/2026","valor":"13.75"},""" +
                """{"data":"04/11/2026","valor":"13.75"}]"""
        const val FOCUS =
            """{"value":[
            {"Indicador":"Selic","Data":"2026-09-18","DataReferencia":"2026","Mediana":13.25},
            {"Indicador":"Selic","Data":"2026-09-18","DataReferencia":"2027","Mediana":11.50},
            {"Indicador":"IPCA","Data":"2026-09-18","DataReferencia":"2026","Mediana":4.9205},
            {"Indicador":"IPCA","Data":"2026-09-18","DataReferencia":"2027","Mediana":4.30},
            {"Indicador":"Câmbio","Data":"2026-09-18","DataReferencia":"2026","Mediana":5.20},
            {"Indicador":"Selic","Data":"2026-09-11","DataReferencia":"2027","Mediana":12.00}
            ]}"""
    }
}
