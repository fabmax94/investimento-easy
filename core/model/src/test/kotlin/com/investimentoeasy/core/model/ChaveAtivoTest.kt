package com.investimentoeasy.core.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import java.time.YearMonth

class ChaveAtivoTest {
    @Test
    fun `R4 - ids canonicos por tipo de ativo`() {
        ChaveAtivo.Ticker("HGLG11").id shouldBe "TICKER:HGLG11"
        ChaveAtivo.Cnpj("12345678000199").id shouldBe "CNPJ:12345678000199"
        ChaveAtivo.FundoPorNome("TREND OURO FIF MULTI RL").id shouldBe "FUNDO:TREND OURO FIF MULTI RL"
        ChaveAtivo.Tesouro("TESOURO IPCA+ 2035", YearMonth.of(2035, 5)).id shouldBe "TESOURO:TESOURO IPCA+ 2035:2035-05"
        ChaveAtivo.Tesouro("TESOURO SELIC 2029", null).id shouldBe "TESOURO:TESOURO SELIC 2029:?"
        ChaveAtivo
            .CreditoPrivado(TipoAtivo.CDB, "BANCO X", Indexador.CDI, Percent.of("118"), YearMonth.of(2027, 12))
            .id shouldBe "CREDITO:CDB:BANCO X:CDI:118%:2027-12"
        ChaveAtivo.CreditoPrivado(TipoAtivo.LCA, "BANCO X", null, null, YearMonth.of(2027, 12)).id shouldBe
            "CREDITO:LCA:BANCO X:?:?:2027-12"
    }

    @Test
    fun `R4 - mesma aplicacao gera a mesma chave, taxa diferente gera outra`() {
        val a = ChaveAtivo.CreditoPrivado(TipoAtivo.CDB, "BANCO X", Indexador.CDI, Percent.of("118.00"), YearMonth.of(2027, 12))
        val b = ChaveAtivo.CreditoPrivado(TipoAtivo.CDB, "BANCO X", Indexador.CDI, Percent.of("118"), YearMonth.of(2027, 12))
        val c = ChaveAtivo.CreditoPrivado(TipoAtivo.CDB, "BANCO X", Indexador.CDI, Percent.of("120"), YearMonth.of(2027, 12))
        a shouldBe b
        a shouldNotBe c
    }

    @Test
    fun `CNPJ invalido e rejeitado`() {
        shouldThrow<IllegalArgumentException> { ChaveAtivo.Cnpj("12.345.678/0001-99") }
        shouldThrow<IllegalArgumentException> { ChaveAtivo.Cnpj("123") }
    }

    @Test
    fun `Sourced carrega a origem`() {
        Money.of("1.00").doRelatorio().origem shouldBe Origem.RELATORIO
        Money.of("1.00").calculado().origem shouldBe Origem.CALCULADO
    }
}
