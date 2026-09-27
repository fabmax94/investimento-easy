package com.investimentoeasy.parser.xperformance

import com.investimentoeasy.core.model.CodigoAviso
import com.investimentoeasy.core.model.ExtracaoCarteira
import com.investimentoeasy.core.model.FormatoArquivo
import com.investimentoeasy.core.model.MetodoExtracao
import com.investimentoeasy.core.model.Money
import com.investimentoeasy.core.model.Movimentacoes
import com.investimentoeasy.core.model.Percent
import com.investimentoeasy.core.model.soma
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class XPerformanceParserTest {
    private val parser = XPerformanceParser()

    private fun extrair(paginas: List<String> = FixturesXPerformance.sintetico()): ExtracaoCarteira =
        parser.parse(paginas).shouldBeInstanceOf<ResultadoParse.Sucesso>().extracao

    @Test
    fun `reconhece o relatorio e marca a extracao como parser deterministico - R2`() {
        val extracao = extrair()
        extracao.formato shouldBe FormatoArquivo.XPERFORMANCE_PDF
        extracao.metodo shouldBe MetodoExtracao.PARSER
    }

    @Test
    fun `arquivo que nao e XPerformance nao e reconhecido`() {
        parser.parse(listOf("Extrato qualquer\nSaldo R$ 10,00")) shouldBe ResultadoParse.FormatoNaoReconhecido
        parser.parse(emptyList()) shouldBe ResultadoParse.FormatoNaoReconhecido
    }

    @Test
    fun `le a data de referencia do rodape - R1`() {
        extrair().dataReferencia shouldBe LocalDate.of(2026, 9, 3)
    }

    @Test
    fun `le o resumo da primeira pagina`() {
        val resumo = extrair().resumo.shouldNotBeNull()
        resumo.patrimonioTotalBruto shouldBe Money.of("150250.00")
        resumo.rentabilidadeMes shouldBe Percent.of("0.85")
        resumo.ganhoMes shouldBe Money.of("1266.00")
        resumo.rentabilidade24Meses shouldBe Percent.of("25.10")
        resumo.ganho24Meses shouldBe Money.of("30100.00")
    }

    @Test
    fun `le a tabela de referencias`() {
        val referencias = extrair().referencias
        referencias.map { it.nome } shouldContainExactly listOf("Portfólio", "CDI", "Ibovespa", "IPCA", "Dólar")
        referencias.first { it.nome == "Dólar" }.ano shouldBe Percent.of("-7.38")
    }

    @Test
    fun `le todas as posicoes com a estrategia correta, inclusive entre paginas`() {
        val posicoes = extrair().posicoes
        posicoes shouldHaveSize 13
        posicoes.map { it.estrategia to it.nome } shouldContainExactly
            listOf(
                "Pós Fixado" to "CDB BANCO EXEMPLO S.A. - MAR/2028 - 112,00% CDI",
                "Pós Fixado" to "Fundo Exemplo Debêntures Incentivadas FIC FIF-Infra RF RL",
                "Pós Fixado" to "LFTB11",
                "Inflação" to "IMAB11",
                "Renda Variável Brasil" to "BOVA11",
                "Renda Variável Brasil" to "ITSA4",
                "Alternativo" to "Trend Ouro FIF Multi RL",
                "Renda Variável Global" to "IVVB11",
                "Renda Variável Global" to "Trend Nasdaq 100 FIA",
                "Fundos Listados" to "HGLG11",
                "Fundos Listados" to "KNCR11",
                "Fundos Listados" to "RECR12",
                "Fundos Listados" to "XYZW11",
            )
    }

    @Test
    fun `le os campos numericos de uma posicao`() {
        val lftb = extrair().posicoes.first { it.nome == "LFTB11" }
        lftb.saldo shouldBe Money.of("2000.00")
        lftb.quantidade shouldBe BigDecimal("21")
        lftb.percentualAlocacao shouldBe Percent.of("1.33")
        lftb.rentabilidadeMes shouldBe Percent.of("34.36")
        lftb.percentualCdiMes shouldBe Percent.of("22160.52")
        lftb.rentabilidadeAno shouldBe Percent.of("37.78")
        lftb.percentualCdiAno shouldBe Percent.of("1316.19")
        lftb.rentabilidade24Meses shouldBe Percent.of("38.15")
        lftb.percentualCdi24Meses shouldBe Percent.of("1231.66")
    }

    @Test
    fun `quantidade fracionaria de cotas usa ponto decimal`() {
        extrair().posicoes.first { it.nome.startsWith("Fundo Exemplo") }.quantidade shouldBe BigDecimal("1234.56")
    }

    @Test
    fun `posicao fantasma e mantida com saldo zero para a validacao sinalizar - R6`() {
        val fantasma = extrair().posicoes.first { it.nome == "RECR12" }
        fantasma.saldo shouldBe Money.ZERO
        fantasma.quantidade shouldBe BigDecimal("9")
    }

    @Test
    fun `subtotais por estrategia batem com a soma das posicoes`() {
        val extracao = extrair()
        extracao.estrategias.map { it.nome to it.saldo } shouldContainExactly
            listOf(
                "Pós Fixado" to Money.of("20000.00"),
                "Inflação" to Money.of("5000.00"),
                "Renda Variável Brasil" to Money.of("15000.00"),
                "Alternativo" to Money.of("20000.00"),
                "Renda Variável Global" to Money.of("50000.00"),
                "Fundos Listados" to Money.of("40000.00"),
            )
        extracao.posicoes.map { it.saldo }.soma() shouldBe Money.of("150000.00")
        extracao.avisos.shouldBeEmpty()
    }

    @Test
    fun `le a serie de rentabilidade mensal ignorando meses sem dado`() {
        val serie = extrair().rentabilidadeMensal
        serie shouldHaveSize 21
        serie.first().mes shouldBe YearMonth.of(2025, 1)
        serie.last().mes shouldBe YearMonth.of(2026, 9)
        serie.last().portfolio shouldBe Percent.of("0.85")
        serie.last().percentualCdi shouldBe Percent.of("531.25")
        serie.first { it.mes == YearMonth.of(2026, 3) }.portfolio shouldBe Percent.of("-1.20")
    }

    @Test
    fun `le a evolucao patrimonial mensal com movimentacoes`() {
        val evolucao = extrair().evolucaoMensal
        evolucao.map { it.mes } shouldContainExactly listOf(YearMonth.of(2026, 7), YearMonth.of(2026, 8), YearMonth.of(2026, 9))
        val agosto = evolucao.first { it.mes == YearMonth.of(2026, 8) }
        agosto.movimentacoes shouldBe Money.of("-5000.00")
        agosto.ir shouldBe Money.of("-30.00")
        agosto.patrimonioFinal shouldBe Money.of("148984.00")
    }

    @Test
    fun `movimentacoes do mes declaradas como inexistentes`() {
        extrair().movimentacoes shouldBe Movimentacoes.Nenhuma
    }

    @Test
    fun `movimentacoes presentes mas em formato desconhecido nao sao descartadas em silencio`() {
        val paginas = FixturesXPerformance.sintetico().toMutableList()
        paginas[8] = paginas[8].replace("Não existem movimentações no mês referência selecionado.", "05/09/2026 Aporte R$ 1.000,00")
        val extracao = extrair(paginas)
        extracao.movimentacoes.shouldBeInstanceOf<Movimentacoes.NaoInterpretadas>().quantidadeLinhas shouldBe 1
        extracao.avisos.map { it.codigo } shouldContainExactly listOf(CodigoAviso.MOVIMENTACOES_NAO_INTERPRETADAS)
    }

    @Test
    fun `sem secao de movimentacoes o dado fica como nao informado`() {
        extrair(FixturesXPerformance.sintetico().take(8)).movimentacoes shouldBe Movimentacoes.NaoInformadas
    }

    @Test
    fun `linha logo apos uma posicao e tratada como continuacao do nome`() {
        val paginas = FixturesXPerformance.sintetico().toMutableList()
        paginas[7] = paginas[7].replace("Legenda", "RL\nLegenda")
        extrair(paginas).posicoes.last().nome shouldBe "XYZW11 RL"
    }

    @Test
    fun `linha desconhecida fora de continuacao vira aviso, nao e ignorada`() {
        val paginas = FixturesXPerformance.sintetico().toMutableList()
        paginas[7] = paginas[7].replace("XYZW11", "linha solta\nXYZW11")
        val extracao = extrair(paginas)
        extracao.avisos.map { it.codigo } shouldContainExactly listOf(CodigoAviso.LINHA_NAO_INTERPRETADA)
        extracao.avisos.single().pagina shouldBe 8
        extracao.posicoes.last().nome shouldBe "XYZW11"
    }

    @Test
    fun `subtotal de estrategia divergente gera aviso`() {
        val paginas = FixturesXPerformance.sintetico().toMutableList()
        paginas[5] = paginas[5].replace("ITSA4 R\$ 5.000,00", "ITSA4 R\$ 5.000,01")
        extrair(paginas).avisos.map { it.codigo } shouldContainExactly listOf(CodigoAviso.SUBTOTAL_ESTRATEGIA_DIVERGENTE)
    }

    @Test
    fun `estrategia desconhecida gera aviso mas e lida`() {
        val paginas = FixturesXPerformance.sintetico().toMutableList()
        paginas[7] = paginas[7].replace("XYZW11 R", "Cripto R\$ 0,00 - 0,00% - - - - - -\nXYZW11 R")
        val extracao = extrair(paginas)
        extracao.estrategias.map { it.nome } shouldContainExactly
            listOf("Pós Fixado", "Inflação", "Renda Variável Brasil", "Alternativo", "Renda Variável Global", "Fundos Listados", "Cripto")
        extracao.avisos.map { it.codigo } shouldContainExactly
            listOf(
                CodigoAviso.ESTRATEGIA_DESCONHECIDA,
                CodigoAviso.SUBTOTAL_ESTRATEGIA_DIVERGENTE,
                CodigoAviso.SUBTOTAL_ESTRATEGIA_DIVERGENTE,
            )
    }

    @Test
    fun `sem posicao detalhada usa a composicao por estrategia`() {
        val semPosicoes = FixturesXPerformance.sintetico().filterIndexed { i, _ -> i !in 4..7 }
        val extracao = extrair(semPosicoes)
        extracao.posicoes.shouldBeEmpty()
        extracao.estrategias.map { it.nome to it.percentualAlocacao } shouldContainExactly
            listOf(
                "Pós Fixado" to Percent.of("13.33"),
                "Inflação" to Percent.of("3.33"),
                "Renda Variável Brasil" to Percent.of("10.00"),
                "Alternativo" to Percent.of("13.33"),
                "Renda Variável Global" to Percent.of("33.33"),
                "Fundos Listados" to Percent.of("26.67"),
                "Caixa" to Percent.ZERO,
            )
    }

    @Test
    fun `sem resumo o patrimonio fica ausente e ha aviso`() {
        val paginas = FixturesXPerformance.sintetico().toMutableList()
        paginas[0] = paginas[0].replace("PATRIMÔNIO TOTAL BRUTO", "OUTRO TITULO")
        val extracao = extrair(paginas)
        extracao.resumo.shouldBeNull()
        extracao.avisos.map { it.codigo } shouldContainExactly listOf(CodigoAviso.SECAO_AUSENTE)
    }

    @Test
    fun `numero da conta e nome do assessor nunca entram na extracao - R7`() {
        val texto = extrair().toString()
        texto shouldNotContain "1234567"
        texto shouldNotContain "Assessor Exemplo"
    }
}
