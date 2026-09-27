package com.investimentoeasy.core.domain.complemento

import com.investimentoeasy.core.model.ChaveAtivo
import com.investimentoeasy.core.model.Money
import com.investimentoeasy.core.model.Percent
import com.investimentoeasy.core.testing.posicao
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ResultadoPosicaoTest {
    private fun dados(
        precoMedio: String? = null,
        valorAplicado: String? = null,
        quantidadeMudou: Boolean = false,
    ) = DadosDaPlanilha(
        ChaveAtivo.Ticker("BOVA11"), "BOVA11", Money.of("900.00"), null, valorAplicado?.let(Money::of), precoMedio?.let(Money::of),
        null, null, null, null, null, null, null, quantidadeMudou,
    )

    @Test
    fun `preco medio vezes a quantidade da base`() {
        val r = resultadoDe(posicao(saldo = "1100.00", quantidade = "10"), dados(precoMedio = "100.00", valorAplicado = "1.00"))!!
        r.custo shouldBe Money.of("1000.00")
        r.resultado shouldBe Money.of("100.00")
        r.percentual shouldBe Percent.of("10.00")
        r.baseDoCusto shouldBe BaseDoCusto.PRECO_MEDIO
    }

    @Test
    fun `sem preco medio usa o valor aplicado, e prejuizo sai negativo`() {
        val r = resultadoDe(posicao(saldo = "750.00", quantidade = null), dados(valorAplicado = "1000.00"))!!
        r.resultado shouldBe Money.of("-250.00")
        r.percentual shouldBe Percent.of("-25.00")
        r.baseDoCusto shouldBe BaseDoCusto.VALOR_APLICADO
    }

    @Test
    fun `sem dado, sem custo ou com quantidade diferente nao ha resultado`() {
        resultadoDe(posicao(), null).shouldBeNull()
        resultadoDe(posicao(), dados()).shouldBeNull()
        resultadoDe(posicao(), dados(precoMedio = "10.00", quantidadeMudou = true)).shouldBeNull()
        resultadoDe(posicao(), dados(valorAplicado = "0.00")).shouldBeNull()
        resultadoDe(posicao(quantidade = null), dados(precoMedio = "10.00")).shouldBeNull()
    }
}
