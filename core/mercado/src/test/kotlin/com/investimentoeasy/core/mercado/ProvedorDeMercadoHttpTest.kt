package com.investimentoeasy.core.mercado

import com.investimentoeasy.core.model.FonteMercado
import com.investimentoeasy.core.model.Percent
import com.investimentoeasy.core.testing.relogioFixo
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

class ProvedorDeMercadoHttpTest {
    private val falso = ServidorDeMercado()
    private val relogio = relogioFixo(Instant.parse("2026-09-26T15:00:00Z"))

    @AfterEach
    fun parar() = falso.servidor.shutdown()

    private fun provedor() =
        ProvedorDeMercadoHttp(
            relogio,
            enderecos = EnderecosDeMercado(falso.url(), falso.url(), falso.url(), falso.url()),
        )

    @Test
    fun `junta as quatro fontes`() =
        runTest {
            val p = provedor().atualizar(setOf("BOVA11", "KNCR11"))
            p.falhas.shouldBeEmpty()
            p.obtidoEm shouldBe relogio.instant()

            // Selic: a série vem preenchida até a próxima reunião; vale a última data até hoje.
            p.selicMeta!!.valor shouldBe BigDecimal("13.75")
            p.selicMeta!!.data shouldBe LocalDate.of(2026, 9, 18)
            p.ipca12Meses!!.valor shouldBe BigDecimal("4.85")
            p.dolar!!.fonte shouldBe FonteMercado.BANCO_CENTRAL

            with(p.focus.shouldNotBeNull()) {
                dataPesquisa shouldBe LocalDate.of(2026, 9, 18)
                selicFimDeAno shouldBe mapOf(2026 to BigDecimal("13.25"), 2027 to BigDecimal("11.50"))
                ipcaDoAno[2026] shouldBe BigDecimal("4.9205")
                cambioFimDeAno[2026] shouldBe BigDecimal("5.20")
            }

            with(p.cotacao("bova11").shouldNotBeNull()) {
                preco shouldBe BigDecimal("120.5")
                data shouldBe LocalDate.of(2026, 9, 25)
                maxima52Semanas shouldBe BigDecimal("125.0")
                (retorno12Meses!! > Percent.of("19")) shouldBe true
                (retorno3Meses!! < retorno6Meses!!) shouldBe true
            }
            p.ibovespa.shouldNotBeNull()
            p.ifix!!.simbolo shouldBe "XFIX11.SA"
        }

    @Test
    fun `FII pela raiz do ISIN, com a versao mais nova do mes e DY de 12 meses`() =
        runTest {
            val informe = provedor().atualizar(setOf("KNCR11")).informe("KNCR11").shouldNotBeNull()
            informe.mesReferencia shouldBe LocalDate.of(2026, 8, 1)
            informe.valorPatrimonialCota shouldBe BigDecimal("102.50")
            informe.dividendYieldUltimoMes shouldBe Percent.of("1.10")
            // set/25..ago/26: 11 meses de 1,00% + 1,10% = 12,10%
            informe.dividendYield12Meses shouldBe Percent.of("12.10")
            informe.mesesNoDividendYield shouldBe 12
        }

    @Test
    fun `fonte fora do ar vira falha e as outras seguem`() =
        runTest {
            falso.falhar["ExpectativasMercadoAnuais"] = 502
            falso.falhar["inf_mensal_fii_2026"] = 500
            val p = provedor().atualizar(setOf("BOVA11", "KNCR11", "XYZW11"))
            p.focus.shouldBeNull()
            p.fiis shouldBe emptyMap()
            p.selicMeta.shouldNotBeNull()
            p.cotacao("BOVA11").shouldNotBeNull()
            p.cotacao("XYZW11").shouldBeNull()
            p.falhas.map { it.fonte } shouldContainExactly listOf(FonteMercado.FOCUS, FonteMercado.CVM)
            p.falhas.first().motivo shouldContain "HTTP 502"
        }

    @Test
    fun `so os tickers vao para o Yahoo e so FIIs pedem informe`() =
        runTest {
            provedor().atualizar(setOf("BOVA11"))
            val caminhos = falso.pedidos.map { it.requestUrl!!.encodedPath }
            caminhos.filter { it.contains("chart") }.map { it.substringAfterLast('/') }.toSet() shouldBe
                setOf("%5EBVSP", "XFIX11.SA", "BOVA11.SA")
            falso.pedidos.forEach { it.requestUrl.toString() shouldNotContain "saldo" }
        }

    @Test
    fun `sem FII na carteira nao baixa o informe da CVM`() =
        runTest {
            provedor().atualizar(setOf("ITSA4"))
            falso.pedidos.none { it.requestUrl!!.encodedPath.contains("inf_mensal") } shouldBe true
        }

    @Test
    fun `raiz do ticker a partir do ISIN`() {
        Cvm.raizDoIsin("BRKNCRCTF000") shouldBe "KNCR"
        Cvm.raizDoIsin("BRPETRACNPR6").shouldBeNull()
    }
}
