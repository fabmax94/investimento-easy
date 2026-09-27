package com.investimentoeasy.parser.xlsx

import com.investimentoeasy.core.model.Money
import com.investimentoeasy.core.model.Percent
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class NumerosPlanilhaTest {
    @Test
    fun dinheiro() {
        NumerosPlanilha.dinheiro("R$ 123.456,78") shouldBe Money.of("123456.78")
        NumerosPlanilha.dinheiro("R$ 1,00") shouldBe Money.of("1.00")
        NumerosPlanilha.dinheiro("-R$ 14,50") shouldBe Money.of("-14.50")
        NumerosPlanilha.dinheiro("R$ -14,50") shouldBe Money.of("-14.50")
        NumerosPlanilha.dinheiro("R$ 0,00") shouldBe Money.ZERO
        NumerosPlanilha.dinheiro("-").shouldBeNull()
        NumerosPlanilha.dinheiro("Indefinido").shouldBeNull()
        NumerosPlanilha.dinheiro(null).shouldBeNull()
        NumerosPlanilha.dinheiro("abc").shouldBeNull()
    }

    @Test
    fun percentual() {
        NumerosPlanilha.percentual("12,34%") shouldBe Percent.of("12.34")
        NumerosPlanilha.percentual("-0,07%") shouldBe Percent.of("-0.07")
        NumerosPlanilha.percentual("0%") shouldBe Percent.of("0")
        NumerosPlanilha.percentual("1.316,19%") shouldBe Percent.of("1316.19")
        NumerosPlanilha.percentual("-").shouldBeNull()
    }

    @Test
    fun quantidade() {
        NumerosPlanilha.quantidade("37") shouldBe BigDecimal("37")
        NumerosPlanilha.quantidade("6330,53") shouldBe BigDecimal("6330.53")
        NumerosPlanilha.quantidade("26.34") shouldBe BigDecimal("26.34")
        NumerosPlanilha.quantidade("1.234") shouldBe BigDecimal("1234")
        NumerosPlanilha.quantidade(" ").shouldBeNull()
    }

    @Test
    fun data() {
        NumerosPlanilha.data("07/08/2026") shouldBe LocalDate.of(2026, 8, 7)
        NumerosPlanilha.data("2026-08-07").shouldBeNull()
        NumerosPlanilha.data("-").shouldBeNull()
    }
}
