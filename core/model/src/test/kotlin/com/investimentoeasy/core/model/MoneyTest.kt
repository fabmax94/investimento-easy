package com.investimentoeasy.core.model

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class MoneyTest {
    private val centavos = Arb.long(-1_000_000_000_00L..1_000_000_000_00L).map { Money.of(BigDecimal.valueOf(it, 2)) }

    @Test
    fun `normaliza para duas casas com arredondamento bancario`() {
        Money.of("10") shouldBe Money.of("10.00")
        Money.of("0.125") shouldBe Money.of("0.12")
        Money.of("0.135") shouldBe Money.of("0.14")
        Money.of(5L).valor shouldBe BigDecimal("5.00")
    }

    @Test
    fun `soma e subtracao sao exatas - propriedade`() =
        runTest {
            checkAll(centavos, centavos) { a, b ->
                (a + b - b) shouldBe a
                (a + b) shouldBe (b + a)
                (a - a).isZero shouldBe true
                (-a + a) shouldBe Money.ZERO
            }
        }

    @Test
    fun `soma de uma lista independe da ordem - propriedade`() =
        runTest {
            checkAll(Arb.list(centavos, 0..30)) { valores ->
                valores.soma() shouldBe valores.reversed().soma()
            }
        }

    @Test
    fun `fracao sobre o total`() {
        Money.of("25.00").fracaoDe(Money.of("100.00"))!!.compareTo(BigDecimal("0.25")) shouldBe 0
        Money.of("25.00").fracaoDe(Money.ZERO).shouldBeNull()
    }

    @Test
    fun `sinal, valor absoluto, multiplicacao e ordem`() {
        Money.of("-3.50").isNegative shouldBe true
        Money.of("-3.50").abs() shouldBe Money.of("3.50")
        (Money.of("10.00") * BigDecimal("1.5")) shouldBe Money.of("15.00")
        (Money.of("1.00") < Money.of("2.00")) shouldBe true
        Money.of("1234.5").toString() shouldBe "R$ 1234.50"
    }
}
