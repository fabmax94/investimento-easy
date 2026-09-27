package com.investimentoeasy.core.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.az
import io.kotest.property.arbitrary.bigDecimal
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal
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
    fun `id reconstroi a mesma chave - ida e volta`() {
        val chaves =
            listOf(
                ChaveAtivo.Ticker("HGLG11"),
                ChaveAtivo.Cnpj("12345678000199"),
                ChaveAtivo.FundoPorNome("FUNDO: COM DOIS PONTOS"),
                ChaveAtivo.Tesouro("TESOURO IPCA+ 2035", YearMonth.of(2035, 5)),
                ChaveAtivo.Tesouro("TESOURO SELIC 2029", null),
                ChaveAtivo.CreditoPrivado(
                    TipoAtivo.CDB,
                    "LOJAS EXEMPLO - FINANCEIRA S.A.",
                    Indexador.CDI,
                    Percent.of("118.00"),
                    YearMonth.of(2027, 12),
                ),
                ChaveAtivo.CreditoPrivado(
                    TipoAtivo.CRI,
                    "EMISSOR:COM:DOIS PONTOS",
                    Indexador.IPCA,
                    Percent.of("6.5"),
                    YearMonth.of(2031, 4),
                ),
                ChaveAtivo.CreditoPrivado(TipoAtivo.LCA, "BANCO X", null, null, YearMonth.of(2027, 12)),
            )
        chaves.forEach { chave -> ChaveAtivo.deId(chave.id) shouldBe chave }
    }

    @Test
    fun `id reconstroi a mesma chave - propriedade`() =
        runTest {
            checkAll(
                Arb.string(1..20, Codepoint.az()),
                Arb.bigDecimal(BigDecimal("0.01"), BigDecimal("300")),
                Arb.enum<Indexador>(),
                Arb.int(2025..2060),
                Arb.int(1..12),
            ) { emissor, taxa, indexador, ano, mes ->
                val chave =
                    ChaveAtivo.CreditoPrivado(
                        TipoAtivo.CDB,
                        emissor.uppercase(),
                        indexador,
                        Percent.of(taxa.setScale(2, java.math.RoundingMode.HALF_EVEN)),
                        YearMonth.of(ano, mes),
                    )
                ChaveAtivo.deId(chave.id) shouldBe chave
            }
        }

    @Test
    fun `id invalido e rejeitado`() {
        shouldThrow<IllegalArgumentException> { ChaveAtivo.deId("TICKER") }
        shouldThrow<IllegalArgumentException> { ChaveAtivo.deId("OUTRO:X") }
        shouldThrow<IllegalArgumentException> { ChaveAtivo.deId("CREDITO:CDB:X:CDI") }
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
