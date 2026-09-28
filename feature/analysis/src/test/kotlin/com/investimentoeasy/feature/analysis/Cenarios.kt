package com.investimentoeasy.feature.analysis

import com.investimentoeasy.core.domain.analise.AnalisarCarteira
import com.investimentoeasy.core.domain.complemento.CasarPlanilha
import com.investimentoeasy.core.domain.complemento.ComplementoPlanilha
import com.investimentoeasy.core.domain.mercado.Perfil
import com.investimentoeasy.core.domain.mercado.leituraDeCambio
import com.investimentoeasy.core.domain.mercado.leituraDeJuros
import com.investimentoeasy.core.domain.recomendacao.EntradaDoMotor
import com.investimentoeasy.core.domain.recomendacao.MotorDeRecomendacao
import com.investimentoeasy.core.importacao.ArquivoRecebido
import com.investimentoeasy.core.importacao.ExtratorDeTextoPdf
import com.investimentoeasy.core.importacao.ImportarRelatorio
import com.investimentoeasy.core.importacao.ResultadoImportacao
import com.investimentoeasy.core.model.Cotacao
import com.investimentoeasy.core.model.ExpectativasFocus
import com.investimentoeasy.core.model.FalhaDeFonte
import com.investimentoeasy.core.model.FonteMercado
import com.investimentoeasy.core.model.InformeFii
import com.investimentoeasy.core.model.PanoramaMercado
import com.investimentoeasy.core.model.Percent
import com.investimentoeasy.core.model.Snapshot
import com.investimentoeasy.core.model.StatusSnapshot
import com.investimentoeasy.core.model.ValorDeMercado
import com.investimentoeasy.parser.xlsx.FixturesPlanilhaXp
import com.investimentoeasy.parser.xperformance.FixturesXPerformance
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/** Carteira sintética do parser, confirmada, e um mercado sintético no formato das fontes reais. */
internal object Cenarios {
    val hoje: LocalDate = LocalDate.of(2026, 9, 13)
    val instante: Instant = Instant.parse("2026-09-13T12:00:00Z")

    val snapshot: Snapshot =
        (
            ImportarRelatorio(ExtratorDeTextoPdf { FixturesXPerformance.sintetico() })
                .importar(ArquivoRecebido("x.pdf", null, "%PDF".toByteArray())) as ResultadoImportacao.Lido
        ).revisao.rascunho!!.copy(status = StatusSnapshot.CONFIRMADO)

    val complemento: ComplementoPlanilha =
        (
            ImportarRelatorio(ExtratorDeTextoPdf { emptyList() })
                .importar(ArquivoRecebido("p.xlsx", null, FixturesPlanilhaXp.bytes())) as ResultadoImportacao.PlanilhaLida
        ).let { CasarPlanilha()(snapshot, it.planilha, instante).complemento }

    private fun cotacao(
        simbolo: String,
        preco: String,
        r12: String,
    ) = Cotacao(simbolo, BigDecimal(preco), hoje, Percent.of("2.1"), Percent.of("4.0"), Percent.of(r12), BigDecimal(preco) + BigDecimal(5))

    private fun informe(
        raiz: String,
        vp: String,
        dy12: String,
        dyMes: String,
    ) = InformeFii(raiz, LocalDate.of(2026, 8, 1), BigDecimal(vp), Percent.of(dy12), Percent.of(dyMes), 12)

    val panorama: PanoramaMercado =
        PanoramaMercado(
            obtidoEm = instante,
            selicMeta = ValorDeMercado(BigDecimal("13.75"), hoje, FonteMercado.BANCO_CENTRAL),
            ipca12Meses = ValorDeMercado(BigDecimal("4.22"), LocalDate.of(2026, 8, 1), FonteMercado.BANCO_CENTRAL),
            dolar = ValorDeMercado(BigDecimal("5.20"), hoje, FonteMercado.BANCO_CENTRAL),
            focus =
                ExpectativasFocus(
                    LocalDate.of(2026, 9, 11),
                    sortedMapOf(2026 to BigDecimal("13.50"), 2027 to BigDecimal("12.00"), 2028 to BigDecimal("10.50")),
                    sortedMapOf(2026 to BigDecimal("4.92"), 2027 to BigDecimal("4.30"), 2028 to BigDecimal("3.80")),
                    sortedMapOf(2026 to BigDecimal("5.20"), 2027 to BigDecimal("5.28"), 2028 to BigDecimal("5.30")),
                ),
            ibovespa = cotacao("^BVSP", "183477", "26.3"),
            ifix = cotacao("XFIX11.SA", "13.17", "4.2"),
            cotacoes =
                mapOf(
                    "BOVA11" to cotacao("BOVA11.SA", "180.82", "27.1"),
                    "HGLG11" to cotacao("HGLG11.SA", "147.86", "-12.0"),
                    "KNCR11" to cotacao("KNCR11.SA", "106.38", "15.6"),
                ),
            fiis =
                mapOf(
                    "HGLG" to informe("HGLG", "165.95", "7.97", "0.70"),
                    "KNCR" to informe("KNCR", "102.64", "13.70", "1.12"),
                ),
            falhas = emptyList(),
        )

    fun estado(
        aba: AbaAnalise = AbaAnalise.O_QUE_FAZER,
        perfil: Perfil? = Perfil.MODERADO,
        comMercado: Boolean = true,
        comPlanilha: Boolean = false,
        falhas: List<FalhaDeFonte> = emptyList(),
    ): EstadoAnalise {
        val mercado = panorama.copy(falhas = falhas).takeIf { comMercado }
        val analise = AnalisarCarteira()(snapshot, mercado?.let { leituraDeJuros(it, hoje) }, mercado?.let { leituraDeCambio(it, hoje) })
        val complementoUsado = complemento.takeIf { comPlanilha }
        return EstadoAnalise(
            carregando = false,
            base = snapshot,
            deterministica = analise,
            recomendacao = perfil?.let { MotorDeRecomendacao()(EntradaDoMotor(snapshot, analise, it, mercado, complementoUsado, hoje)) },
            perfil = perfil,
            panorama = mercado,
            aba = aba,
            complemento = complementoUsado,
        )
    }
}
