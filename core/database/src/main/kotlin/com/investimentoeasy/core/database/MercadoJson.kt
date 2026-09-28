package com.investimentoeasy.core.database

import com.investimentoeasy.core.model.Cotacao
import com.investimentoeasy.core.model.ExpectativasFocus
import com.investimentoeasy.core.model.FalhaDeFonte
import com.investimentoeasy.core.model.FonteMercado
import com.investimentoeasy.core.model.InformeFii
import com.investimentoeasy.core.model.PanoramaMercado
import com.investimentoeasy.core.model.Percent
import com.investimentoeasy.core.model.ValorDeMercado
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/** Conteúdo da coluna `mercado.conteudo`. Números como texto, para não perder precisão. */
@Serializable
internal data class MercadoJson(
    val obtidoEm: Long,
    val selic: ValorJson? = null,
    val ipca: ValorJson? = null,
    val dolar: ValorJson? = null,
    val focus: FocusJson? = null,
    val ibovespa: CotacaoJson? = null,
    val ifix: CotacaoJson? = null,
    val cotacoes: Map<String, CotacaoJson> = emptyMap(),
    val fiis: Map<String, InformeJson> = emptyMap(),
    val falhas: List<FalhaJson> = emptyList(),
) {
    fun texto(): String = JSON.encodeToString(serializer(), this)

    fun paraModelo(): PanoramaMercado =
        PanoramaMercado(
            obtidoEm = Instant.ofEpochMilli(obtidoEm),
            selicMeta = selic?.paraModelo(),
            ipca12Meses = ipca?.paraModelo(),
            dolar = dolar?.paraModelo(),
            focus = focus?.paraModelo(),
            ibovespa = ibovespa?.paraModelo(),
            ifix = ifix?.paraModelo(),
            cotacoes = cotacoes.mapValues { it.value.paraModelo() },
            fiis = fiis.mapValues { it.value.paraModelo() },
            falhas = falhas.map { FalhaDeFonte(FonteMercado.valueOf(it.fonte), it.motivo) },
        )

    companion object {
        private val JSON = Json { ignoreUnknownKeys = true }

        fun de(texto: String): MercadoJson = JSON.decodeFromString(serializer(), texto)

        fun de(p: PanoramaMercado): MercadoJson =
            MercadoJson(
                obtidoEm = p.obtidoEm.toEpochMilli(),
                selic = p.selicMeta?.let(ValorJson::de),
                ipca = p.ipca12Meses?.let(ValorJson::de),
                dolar = p.dolar?.let(ValorJson::de),
                focus =
                    p.focus?.let { f ->
                        FocusJson(f.dataPesquisa.toString(), f.selicFimDeAno.texto(), f.ipcaDoAno.texto(), f.cambioFimDeAno.texto())
                    },
                ibovespa = p.ibovespa?.let(CotacaoJson::de),
                ifix = p.ifix?.let(CotacaoJson::de),
                cotacoes = p.cotacoes.mapValues { CotacaoJson.de(it.value) },
                fiis = p.fiis.mapValues { InformeJson.de(it.value) },
                falhas = p.falhas.map { FalhaJson(it.fonte.name, it.motivo) },
            )

        private fun Map<Int, BigDecimal>.texto() = entries.associate { it.key.toString() to it.value.toPlainString() }
    }
}

@Serializable
internal data class ValorJson(
    val valor: String,
    val data: String,
    val fonte: String,
) {
    fun paraModelo() = ValorDeMercado(BigDecimal(valor), LocalDate.parse(data), FonteMercado.valueOf(fonte))

    companion object {
        fun de(v: ValorDeMercado) = ValorJson(v.valor.toPlainString(), v.data.toString(), v.fonte.name)
    }
}

@Serializable
internal data class FocusJson(
    val dataPesquisa: String,
    val selic: Map<String, String> = emptyMap(),
    val ipca: Map<String, String> = emptyMap(),
    val cambio: Map<String, String> = emptyMap(),
) {
    fun paraModelo() = ExpectativasFocus(LocalDate.parse(dataPesquisa), selic.anos(), ipca.anos(), cambio.anos())

    private fun Map<String, String>.anos() = entries.associate { it.key.toInt() to BigDecimal(it.value) }.toSortedMap()
}

@Serializable
internal data class CotacaoJson(
    val simbolo: String,
    val preco: String,
    val data: String,
    val r3: String? = null,
    val r6: String? = null,
    val r12: String? = null,
    val maxima: String? = null,
) {
    fun paraModelo() =
        Cotacao(
            simbolo,
            BigDecimal(preco),
            LocalDate.parse(data),
            r3?.let(Percent::of),
            r6?.let(Percent::of),
            r12?.let(Percent::of),
            maxima?.let(::BigDecimal),
        )

    companion object {
        fun de(c: Cotacao) =
            CotacaoJson(
                c.simbolo,
                c.preco.toPlainString(),
                c.data.toString(),
                c.retorno3Meses?.pontos?.toPlainString(),
                c.retorno6Meses?.pontos?.toPlainString(),
                c.retorno12Meses?.pontos?.toPlainString(),
                c.maxima52Semanas?.toPlainString(),
            )
    }
}

@Serializable
internal data class InformeJson(
    val raiz: String,
    val mes: String,
    val vp: String,
    val dy12: String? = null,
    val dyUltimo: String? = null,
    val meses: Int = 0,
) {
    fun paraModelo() = InformeFii(raiz, LocalDate.parse(mes), BigDecimal(vp), dy12?.let(Percent::of), dyUltimo?.let(Percent::of), meses)

    companion object {
        fun de(i: InformeFii) =
            InformeJson(
                i.raizTicker,
                i.mesReferencia.toString(),
                i.valorPatrimonialCota.toPlainString(),
                i.dividendYield12Meses?.pontos?.toPlainString(),
                i.dividendYieldUltimoMes?.pontos?.toPlainString(),
                i.mesesNoDividendYield,
            )
    }
}

@Serializable
internal data class FalhaJson(
    val fonte: String,
    val motivo: String,
)
