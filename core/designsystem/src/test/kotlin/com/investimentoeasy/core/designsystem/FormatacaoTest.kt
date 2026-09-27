package com.investimentoeasy.core.designsystem

import com.investimentoeasy.core.model.Money
import com.investimentoeasy.core.model.Percent
import io.kotest.matchers.shouldBe
import org.junit.Test
import java.time.LocalDate

class FormatacaoTest {
    @Test
    fun `reais no padrao brasileiro`() {
        Formatacao.reais(Money.of("284912.40")) shouldBe "R$ 284.912,40"
        Formatacao.reais(Money.of("284912.40"), centavos = false) shouldBe "R$ 284.912"
        Formatacao.reais(Money.of("0.00")) shouldBe "R$ 0,00"
        Formatacao.reais(Money.of("-3100.00"), centavos = false) shouldBe "−R$ 3.100"
        Formatacao.reais(Money.of("5034.00"), centavos = false, comSinal = true) shouldBe "+R$ 5.034"
        Formatacao.reais(Money.ZERO, comSinal = true) shouldBe "R$ 0,00"
    }

    @Test
    fun `percentual com sinal e casas`() {
        Formatacao.percentual(Percent.of("1.8")) shouldBe "1,80%"
        Formatacao.percentual(Percent.of("1.8"), comSinal = true) shouldBe "+1,80%"
        Formatacao.percentual(Percent.of("-0.26"), comSinal = true) shouldBe "−0,26%"
        Formatacao.percentual(Percent.of("22160.52")) shouldBe "22.160,52%"
        Formatacao.percentual(Percent.of("16.95"), casas = 0) shouldBe "17%"
        Formatacao.percentual(Percent.of("-0.001"), comSinal = true) shouldBe "0,00%"
    }

    @Test
    fun `datas`() {
        Formatacao.data(LocalDate.of(2026, 9, 3)) shouldBe "03/09/2026"
        Formatacao.dataCurta(LocalDate.of(2026, 9, 3)) shouldBe "03/09"
    }

    @Test
    fun `regra zero - ausencia nunca vira zero`() {
        Formatacao.reais(null) shouldBe "—"
        Formatacao.percentual(null) shouldBe "—"
        Formatacao.data(null) shouldBe "—"
        Formatacao.dataCurta(null) shouldBe "—"
    }
}
