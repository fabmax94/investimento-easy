package com.investimentoeasy.parser.xlsx

import com.investimentoeasy.core.model.soma
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Teste opcional contra planilhas reais, que NUNCA são versionadas (R7). Coloque .xlsx em
 * `local-fixtures/`; sem arquivos, o teste é ignorado. Só verifica invariantes: nenhum valor da
 * carteira real aparece aqui.
 */
class PlanilhaRealTest {
    private val diretorio = File(System.getProperty("localFixturesDir") ?: "local-fixtures")

    @Test
    fun `planilhas reais respeitam as invariantes do parser`() {
        val arquivos = diretorio.listFiles { f -> f.extension.equals("xlsx", ignoreCase = true) }.orEmpty()
        assumeTrue(arquivos.isNotEmpty(), "Sem .xlsx em ${diretorio.absolutePath}")

        for (arquivo in arquivos) {
            val bytes = arquivo.readBytes()
            val planilha = PosicaoDetalhadaXpParser().parse(bytes).shouldBeInstanceOf<ResultadoPlanilha.Sucesso>().planilha

            planilha.avisos.shouldBeEmpty()
            planilha.dataPosicao.shouldNotBeNull()
            planilha.itens.shouldNotBeEmpty()
            val patrimonio = planilha.patrimonio.shouldNotBeNull()
            val soma = planilha.itens.map { it.saldo }.soma()
            val diferenca = (patrimonio - soma).abs().valor.multiply(BigDecimal(100)).divide(patrimonio.valor, 4, RoundingMode.HALF_EVEN)
            (diferenca <= BigDecimal(1)) shouldBe true

            val texto = planilha.toString()
            identificadores(bytes).forEach { texto shouldNotContain it }
        }
    }

    /** Conta, titular e assessor lidos do próprio cabeçalho, para conferir que não vazaram. */
    private fun identificadores(bytes: ByteArray): List<String> {
        val cabecalho = LeitorXlsx().primeiraAba(bytes).linhas.take(5).flatMap { it.preenchidas.values }
        val conta = cabecalho.firstNotNullOfOrNull { Regex("""Conta:\s*(\d+)""").find(it)?.groupValues?.get(1) }
        val titular = cabecalho.firstOrNull { it.contains("este é o seu patrimônio") }?.substringBefore(",")
        val assessor = LeitorXlsx().primeiraAba(bytes).linhas.getOrNull(3)?.preenchidas?.filterKeys { it >= 5 }?.values.orEmpty()
        return listOfNotNull(conta, titular) + assessor
    }
}
