package com.investimentoeasy.parser.xperformance

import com.investimentoeasy.core.model.Money
import com.investimentoeasy.core.model.Percent
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class NumerosBrTest {
    @Test
    fun `dinheiro no padrao brasileiro, inclusive negativo e com milhar`() {
        NumerosBr.dinheiro("R$ 1.971,96") shouldBe Money.of("1971.96")
        NumerosBr.dinheiro("-R$ 14.293,46") shouldBe Money.of("-14293.46")
        NumerosBr.dinheiro("R$ 0,00") shouldBe Money.ZERO
        NumerosBr.dinheiro("R$ 1.234.567,89") shouldBe Money.of("1234567.89")
    }

    @Test
    fun `percentual com milhar, negativo e ausente`() {
        NumerosBr.percentual("22.160,52%") shouldBe Percent.of("22160.52")
        NumerosBr.percentual("-0,50%") shouldBe Percent.of("-0.5")
        NumerosBr.percentual("-0,00%") shouldBe Percent.ZERO
        NumerosBr.percentual("-").shouldBeNull()
    }

    @Test
    fun `quantidade usa ponto como separador decimal`() {
        NumerosBr.quantidade("6330.53") shouldBe BigDecimal("6330.53")
        NumerosBr.quantidade("4") shouldBe BigDecimal("4")
        NumerosBr.quantidade("-").shouldBeNull()
    }
}
