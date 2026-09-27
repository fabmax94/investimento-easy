package com.investimentoeasy.parser.xperformance

import com.investimentoeasy.core.model.CodigoAviso
import com.investimentoeasy.core.model.soma
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Teste opcional contra relatórios reais, que NUNCA são versionados (R7).
 *
 * Coloque um ou mais PDFs XPerformance em `local-fixtures/` na raiz do projeto. Sem arquivos,
 * o teste é ignorado. Ele só verifica invariantes (nada de valores fixos da carteira real).
 */
class XPerformanceRelatorioRealTest {
    private val diretorio = File(System.getProperty("localFixturesDir") ?: "local-fixtures")

    @Test
    fun `relatorios reais respeitam as invariantes do parser`() {
        val pdfs = diretorio.listFiles { f -> f.extension.equals("pdf", ignoreCase = true) }.orEmpty()
        assumeTrue(pdfs.isNotEmpty(), "Sem PDFs em ${diretorio.absolutePath}")

        for (pdf in pdfs) {
            val resultado = XPerformanceParser().parse(extrairPaginas(pdf))

            val extracao = resultado.shouldBeInstanceOf<ResultadoParse.Sucesso>().extracao
            extracao.dataReferencia.shouldNotBeNull()
            val patrimonio = extracao.resumo.shouldNotBeNull().patrimonioTotalBruto
            val avisosDeLeitura =
                extracao.avisos.filter {
                    it.codigo == CodigoAviso.LINHA_NAO_INTERPRETADA || it.codigo == CodigoAviso.SUBTOTAL_ESTRATEGIA_DIVERGENTE
                }
            avisosDeLeitura.shouldBeEmpty()

            val somaPosicoes = extracao.posicoes.map { it.saldo }.soma()
            val diferenca = (patrimonio - somaPosicoes).abs().valor
            val percentual = diferenca.multiply(BigDecimal(100)).divide(patrimonio.valor, 4, RoundingMode.HALF_EVEN)
            (percentual <= BigDecimal("0.5")) shouldBe true
        }
    }

    private fun extrairPaginas(pdf: File): List<String> =
        Loader.loadPDF(pdf).use { documento ->
            val stripper = PDFTextStripper().apply { sortByPosition = true }
            (1..documento.numberOfPages).map { pagina ->
                stripper.startPage = pagina
                stripper.endPage = pagina
                stripper.getText(documento)
            }
        }
}
