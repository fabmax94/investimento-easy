package com.investimentoeasy.core.model

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class PercentTest {
    @Test
    fun `pontos percentuais e fracao`() {
        Percent.of("0.73").fracao.compareTo(BigDecimal("0.0073")) shouldBe 0
        Percent.daFracao(BigDecimal("0.005")) shouldBe Percent.of("0.5")
    }

    @Test
    fun `aritmetica, ordem e texto`() {
        (Percent.of("10") - Percent.of("3")) shouldBe Percent.of("7")
        (Percent.of("1.5") + Percent.of("1.5")) shouldBe Percent.of("3")
        Percent.of("-2").abs() shouldBe Percent.of("2")
        (Percent.of("0.49") < Percent.of("0.5")) shouldBe true
        Percent.of("16.950").toString() shouldBe "16.95%"
    }

    @Test
    fun `igualdade independe da escala de entrada`() {
        Percent.of("1") shouldBe Percent.of("1.000")
    }
}
