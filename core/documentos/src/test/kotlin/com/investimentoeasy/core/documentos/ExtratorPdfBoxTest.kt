package com.investimentoeasy.core.documentos

import androidx.test.core.app.ApplicationProvider
import com.investimentoeasy.core.importacao.ArquivoIlegivelException
import com.investimentoeasy.core.importacao.ArquivoRecebido
import com.investimentoeasy.core.importacao.ImportarRelatorio
import com.investimentoeasy.core.importacao.PdfProtegidoException
import com.investimentoeasy.core.importacao.ResultadoImportacao
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode

@RunWith(RobolectricTestRunner::class)
class ExtratorPdfBoxTest {
    private val extrator = ExtratorPdfBox(ApplicationProvider.getApplicationContext())

    @Test
    fun `extrai o texto de cada pagina na ordem`() {
        val paginas = extrator.paginas(pdf(listOf("Primeira pagina", "Segunda pagina")))
        paginas shouldHaveSize 2
        paginas[0] shouldContain "Primeira pagina"
        paginas[1] shouldContain "Segunda pagina"
    }

    @Test
    fun `PDF com senha vira PdfProtegidoException`() {
        shouldThrow<PdfProtegidoException> { extrator.paginas(pdf(listOf("segredo"), senha = "1234")) }
    }

    @Test
    fun `bytes que nao sao PDF viram ArquivoIlegivelException`() {
        shouldThrow<ArquivoIlegivelException> { extrator.paginas("%PDF-quebrado".toByteArray()) }
    }

    /**
     * Garante que o PdfBox-Android gera o mesmo texto que o PDFBox usado para escrever o
     * parser. Só roda com relatórios reais em `local-fixtures/` (fora do git, R7).
     */
    @Test
    fun `relatorios reais sao lidos de ponta a ponta no Android`() {
        val dir = File(System.getProperty("localFixturesDir") ?: "local-fixtures")
        val pdfs = dir.listFiles { f -> f.extension.equals("pdf", ignoreCase = true) }.orEmpty()
        assumeTrue("Sem PDFs em ${dir.absolutePath}", pdfs.isNotEmpty())

        for (arquivo in pdfs) {
            val lido =
                ImportarRelatorio(extrator)
                    .importar(ArquivoRecebido(arquivo.name, "application/pdf", arquivo.readBytes()))
                    .shouldBeInstanceOf<ResultadoImportacao.Lido>()
            val revisao = lido.revisao
            revisao.rascunho.shouldNotBeNull()
            revisao.extracao.posicoes.isNotEmpty() shouldBe true
            revisao.extracao.avisos.shouldBeEmpty()
            val conferencia = revisao.validacao.conferenciaSoma.shouldNotBeNull()
            conferencia.dentroDaTolerancia shouldBe true
            (conferencia.percentual.pontos.setScale(2, RoundingMode.HALF_EVEN) <= BigDecimal("0.50")) shouldBe true
        }
    }

    private fun pdf(
        textos: List<String>,
        senha: String? = null,
    ): ByteArray =
        PDDocument().use { documento ->
            textos.forEach { texto ->
                val pagina = PDPage()
                documento.addPage(pagina)
                PDPageContentStream(documento, pagina).use { conteudo ->
                    conteudo.beginText()
                    conteudo.setFont(PDType1Font.HELVETICA, 12f)
                    conteudo.newLineAtOffset(72f, 700f)
                    conteudo.showText(texto)
                    conteudo.endText()
                }
            }
            senha?.let { documento.protect(StandardProtectionPolicy(it, it, AccessPermission()).apply { encryptionKeyLength = 128 }) }
            ByteArrayOutputStream().also(documento::save).toByteArray()
        }
}
