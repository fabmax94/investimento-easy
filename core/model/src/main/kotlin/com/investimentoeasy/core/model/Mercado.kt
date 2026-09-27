package com.investimentoeasy.core.model

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/** De onde veio um dado de mercado; aparece na tela junto com a data. */
public enum class FonteMercado(
    public val rotulo: String,
) {
    BANCO_CENTRAL("Banco Central (SGS)"),
    FOCUS("Boletim Focus (Banco Central)"),
    YAHOO("Yahoo Finance"),
    CVM("CVM, informe mensal de FIIs"),
}

/** Um número de mercado com a data a que se refere. */
public data class ValorDeMercado(
    val valor: BigDecimal,
    val data: LocalDate,
    val fonte: FonteMercado,
)

/** Mediana do Focus para o fim de cada ano, na data da pesquisa. */
public data class ExpectativasFocus(
    val dataPesquisa: LocalDate,
    val selicFimDeAno: Map<Int, BigDecimal>,
    val ipcaDoAno: Map<Int, BigDecimal>,
    val cambioFimDeAno: Map<Int, BigDecimal>,
)

/** Cotação e retornos de um papel negociado em bolsa (ou índice). */
public data class Cotacao(
    val simbolo: String,
    val preco: BigDecimal,
    val data: LocalDate,
    /** Retornos em %, com proventos quando a fonte ajusta o histórico. */
    val retorno3Meses: Percent?,
    val retorno6Meses: Percent?,
    val retorno12Meses: Percent?,
    val maxima52Semanas: BigDecimal?,
)

/** Dados de um FII no informe mensal da CVM (último mês entregue). */
public data class InformeFii(
    val raizTicker: String,
    val mesReferencia: LocalDate,
    val valorPatrimonialCota: BigDecimal,
    /** Soma do dividend yield mensal dos últimos 12 meses informados, em %. */
    val dividendYield12Meses: Percent?,
    val dividendYieldUltimoMes: Percent?,
    val mesesNoDividendYield: Int,
)

public data class FalhaDeFonte(
    val fonte: FonteMercado,
    val motivo: String,
)

/**
 * Tudo o que o app buscou do mercado numa atualização. Cada parte pode faltar: uma fonte fora
 * do ar não impede as outras (regra zero: ausência é `null`, nunca zero).
 */
public data class PanoramaMercado(
    val obtidoEm: Instant,
    val selicMeta: ValorDeMercado?,
    val ipca12Meses: ValorDeMercado?,
    val dolar: ValorDeMercado?,
    val focus: ExpectativasFocus?,
    val ibovespa: Cotacao?,
    /** IFIX pelo ETF XFIX11 (o índice não tem cotação pública gratuita). */
    val ifix: Cotacao?,
    val cotacoes: Map<String, Cotacao>,
    val fiis: Map<String, InformeFii>,
    val falhas: List<FalhaDeFonte>,
) {
    public fun cotacao(ticker: String): Cotacao? = cotacoes[ticker.uppercase()]

    public fun informe(ticker: String): InformeFii? = fiis[ticker.uppercase().take(RAIZ)]

    private companion object {
        const val RAIZ = 4
    }
}
