package com.investimentoeasy.core.ai

import com.investimentoeasy.core.domain.complemento.ComplementoPlanilha
import com.investimentoeasy.core.domain.complemento.DadosDaPlanilha
import com.investimentoeasy.core.model.ChaveAtivo
import com.investimentoeasy.core.model.Money
import com.investimentoeasy.core.model.ProventoPrevisto
import com.investimentoeasy.core.testing.INSTANTE_FIXO
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class EntradaComPlanilhaTest {
    private val bova =
        DadosDaPlanilha(
            ChaveAtivo.Ticker("BOVA11"), "BOVA11", Money.of("9800.00"), BigDecimal("10"), null, Money.of("700.00"),
            null, null, null, null, null, null, null, quantidadeMudou = false,
        )
    private val complemento =
        ComplementoPlanilha(
            Cenario.snapshot.id,
            LocalDate.of(2026, 6, 21),
            INSTANTE_FIXO,
            listOf(bova),
            listOf(ProventoPrevisto("BOVA11", "DIVIDENDO", null, Money.of("12.34"), LocalDate.of(2026, 6, 30))),
        )
    private val entrada = montarEntrada(Cenario.snapshot, Cenario.analise, complemento)

    @Test
    fun `posicao com planilha leva custo e resultado estimado`() {
        val planilha = entrada.posicoes.single { it.nome == "BOVA11" }.planilha.shouldNotBeNull()
        planilha.precoMedio shouldBe "700.00"
        planilha.custoEstimado shouldBe "7000.00"
        planilha.resultadoEstimado shouldBe "3000.00"
        planilha.resultadoEstimadoPercentual shouldBe "42.86"
        entrada.posicoes.single { it.nome == "Trend Global FIA" }.planilha.shouldBeNull()
    }

    @Test
    fun `data da planilha, proventos e o que ainda falta`() {
        entrada.dataPlanilha shouldBe "2026-06-21"
        entrada.proventosPrevistos.map { it.ativo to it.valorLiquido } shouldContainExactly listOf("BOVA11" to "12.34")
        entrada.dadosAusentes.first() shouldContain "para: Trend Global FIA"
    }

    @Test
    fun `numeros da planilha podem ser citados pelo Claude`() {
        val saida = Cenario.saida(veredicto = "BOVA11 tem resultado estimado de R$ 3.000, 42,86% sobre o custo.")
        ValidadorDeNumeros(entrada).validar(saida) shouldBe emptyList()
        ValidadorDeNumeros(Cenario.entrada).validar(saida).isEmpty() shouldBe false
    }
}
