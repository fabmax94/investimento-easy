package com.investimentoeasy.parser.xlsx

import com.investimentoeasy.core.model.CodigoAviso
import com.investimentoeasy.core.model.Money
import com.investimentoeasy.core.model.Percent
import com.investimentoeasy.core.model.PlanilhaPosicao
import com.investimentoeasy.core.model.SecaoPlanilha
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class PosicaoDetalhadaXpParserTest {
    private val parser = PosicaoDetalhadaXpParser()

    private fun ler(linhas: List<List<String>> = FixturesPlanilhaXp.linhas()): PlanilhaPosicao =
        parser
            .parse(LeitorXlsx().primeiraAba(EscritorXlsx.gerar(linhas)))
            .shouldBeInstanceOf<ResultadoPlanilha.Sucesso>()
            .planilha

    private fun PlanilhaPosicao.item(nome: String) = itens.single { it.nome == nome }

    @Test
    fun `cabecalho so fornece a data e o patrimonio`() {
        val planilha = ler()
        planilha.dataPosicao shouldBe LocalDate.of(2026, 6, 21)
        planilha.patrimonio shouldBe Money.of("143000.00")
        planilha.avisos.shouldBeEmpty()
    }

    @Test
    fun `nada de conta, titular ou assessor no resultado`() {
        val texto = ler().toString()
        listOf("0000000", "Titular Exemplo", "A00000", "Assessor Exemplo", "Conta").forEach { texto shouldNotContain it }
    }

    @Test
    fun `le todas as posicoes com a secao e o subgrupo`() {
        val planilha = ler()
        planilha.itens.map { it.nome } shouldContainExactly
            listOf(
                "Trend Nasdaq 100 FIM RL", "Trend Ouro FIF Multi RL", "Fundo Exemplo Debêntures Incentivadas FIC FIF-Infra RF RL",
                "BOVA11", "ITSA4", "IVVB11", "CDB BANCO EXEMPLO S.A. - MAR/2028", "CDB OUTRO BANCO S.A. - AGO/2026",
                "HGLG11", "KNCR11", "XYZW11", "RECR12",
            )
        planilha.item("IVVB11").secao shouldBe SecaoPlanilha.ACOES
        planilha.item("IVVB11").subgrupo shouldBe "Renda Variável Global"
        planilha.item("KNCR11").secao shouldBe SecaoPlanilha.FUNDOS_IMOBILIARIOS
    }

    @Test
    fun `fundos trazem valor aplicado e rentabilidade liquida`() {
        val nasdaq = ler().item("Trend Nasdaq 100 FIM RL")
        nasdaq.saldo shouldBe Money.of("38000.00")
        nasdaq.valorAplicado shouldBe Money.of("27000.00")
        nasdaq.rentabilidadeDesdeInicio shouldBe Percent.of("40.00")
        nasdaq.valorLiquido shouldBe Money.of("36200.00")
        nasdaq.precoMedio.shouldBeNull()
        nasdaq.quantidade.shouldBeNull()
    }

    @Test
    fun `acoes e FIIs trazem preco medio e quantidade`() {
        val planilha = ler()
        with(planilha.item("BOVA11")) {
            precoMedio shouldBe Money.of("151.23")
            quantidade shouldBe BigDecimal("60")
            rentabilidadeDesdeInicio shouldBe Percent.of("8.00")
        }
        with(planilha.item("KNCR11")) {
            precoMedio shouldBe Money.of("95.00")
            quantidade shouldBe BigDecimal("200")
            rentabilidadeDesdeInicio shouldBe Percent.of("30.00")
        }
    }

    @Test
    fun `renda fixa traz taxa, datas, IR e IOF`() {
        val cdb = ler().item("CDB BANCO EXEMPLO S.A. - MAR/2028")
        cdb.taxa shouldBe "112,00% CDI"
        cdb.dataAplicacao shouldBe LocalDate.of(2023, 3, 10)
        cdb.vencimento shouldBe LocalDate.of(2028, 3, 10)
        cdb.valorAplicado shouldBe Money.of("8000.00")
        cdb.ir shouldBe Money.of("285.00")
        cdb.iof shouldBe Money.ZERO
        cdb.valorLiquido shouldBe Money.of("9615.00")
        cdb.quantidade shouldBe BigDecimal("10")
    }

    @Test
    fun `ausencia vira nulo, nunca zero`() {
        with(ler().item("RECR12")) {
            saldo shouldBe Money.ZERO
            precoMedio.shouldBeNull()
            rentabilidadeDesdeInicio.shouldBeNull()
        }
    }

    @Test
    fun `proventos previstos e custodia remunerada ignorada`() {
        val planilha = ler()
        planilha.proventos.map { it.ativo to it.evento } shouldContainExactly listOf("KNCR11" to "RENDIMENTO", "ITSA4" to "DIVIDENDO")
        with(planilha.proventos.first()) {
            valorLiquido shouldBe Money.of("200.00")
            quantidade shouldBe BigDecimal("200")
            dataPagamento shouldBe LocalDate.of(2026, 6, 25)
        }
        planilha.itens.count { it.nome == "BOVA11" } shouldBe 1
    }

    @Test
    fun `avisa quando as linhas nao somam o total da secao`() {
        val linhas =
            FixturesPlanilhaXp.linhas().map {
                val posicaoDoItsa4 = it.firstOrNull() == "ITSA4" && it[1].startsWith("R$")
                if (posicaoDoItsa4) it.toMutableList().apply { this[1] = "R$ 1.000,00" } else it
            }
        ler(linhas).avisos.map { it.codigo } shouldContainExactly listOf(CodigoAviso.SUBTOTAL_ESTRATEGIA_DIVERGENTE)
    }

    @Test
    fun `avisa linha sem posicao, provento incompleto e secao desconhecida`() {
        val desconhecida =
            listOf(
                listOf("Previdência", "", "", "", "", "", "R$ 100,00"),
                listOf("0,1% | Previdência", "Posição"),
                listOf("Plano Exemplo", "R$ 100,00"),
            )
        val linhas =
            FixturesPlanilhaXp.linhas().flatMap { linha ->
                when {
                    linha.firstOrNull() == "XYZW11" -> listOf(linha.toMutableList().apply { this[1] = "-" })
                    linha.firstOrNull() == "ITSA4" && linha.getOrNull(5) == "DIVIDENDO" ->
                        listOf(
                            linha.toMutableList().apply { this[5] = "" },
                        )
                    linha.firstOrNull()?.startsWith("Dividendos") == true -> desconhecida + listOf(linha)
                    else -> listOf(linha)
                }
            }
        val planilha = ler(linhas)
        planilha.avisos.map { it.codigo } shouldContainExactly
            listOf(
                CodigoAviso.LINHA_NAO_INTERPRETADA,
                CodigoAviso.ESTRATEGIA_DESCONHECIDA,
                CodigoAviso.LINHA_NAO_INTERPRETADA,
                CodigoAviso.SUBTOTAL_ESTRATEGIA_DIVERGENTE,
            )
        planilha.item("Plano Exemplo").secao shouldBe SecaoPlanilha.OUTRA
    }

    @Test
    fun `nao reconhece outra planilha`() {
        val outra = LeitorXlsx().primeiraAba(EscritorXlsx.gerar(listOf(listOf("Ativo", "Valor"), listOf("BOVA11", "10"))))
        parser.parse(outra) shouldBe ResultadoPlanilha.FormatoNaoReconhecido
    }
}
