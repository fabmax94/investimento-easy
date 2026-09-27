package com.investimentoeasy.core.importacao

import com.investimentoeasy.core.domain.validacao.Problema
import com.investimentoeasy.core.model.Money
import com.investimentoeasy.parser.xlsx.EscritorXlsx
import com.investimentoeasy.parser.xlsx.FixturesPlanilhaXp
import com.investimentoeasy.parser.xperformance.FixturesXPerformance
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ImportarRelatorioTest {
    private val pdf = "%PDF-1.7 conteúdo".toByteArray()

    private fun importar(
        extrator: ExtratorDeTextoPdf = ExtratorDeTextoPdf { FixturesXPerformance.sintetico() },
        conteudo: ByteArray = pdf,
    ) = ImportarRelatorio(extrator).importar(ArquivoRecebido("XPerformance.pdf", "application/pdf", conteudo))

    @Test
    fun `relatorio XPerformance vira revisao pronta para confirmar`() {
        val lido = importar().shouldBeInstanceOf<ResultadoImportacao.Lido>()
        lido.nomeArquivo shouldBe "XPerformance.pdf"
        lido.paginas shouldBe FixturesXPerformance.PAGINAS
        lido.tamanhoBytes shouldBe pdf.size
        lido.revisao.rascunho!!.dataReferencia shouldBe LocalDate.of(2026, 9, 3)
        lido.revisao.validacao.conferenciaSoma!!.somaPosicoes shouldBe Money.of("150000.00")
        lido.revisao.validacao.impeditivo shouldBe false
    }

    @Test
    fun `R2 - PDF que nao e XPerformance nao e reconhecido`() {
        importar(extrator = { listOf("Extrato de outro banco") }) shouldBe ResultadoImportacao.Falha(MotivoFalha.RELATORIO_NAO_RECONHECIDO)
    }

    @Test
    fun `arquivo que nao e PDF nem xlsx pelo conteudo e recusado sem tentar extrair`() {
        val extratorQueNaoPodeSerChamado = ExtratorDeTextoPdf { error("não deveria extrair") }
        importar(extratorQueNaoPodeSerChamado, "GIF89a imagem".toByteArray()) shouldBe
            ResultadoImportacao.Falha(MotivoFalha.FORMATO_NAO_SUPORTADO)
    }

    @Test
    fun `planilha Posicao Detalhada e lida sem passar pelo extrator de PDF`() {
        val extratorQueNaoPodeSerChamado = ExtratorDeTextoPdf { error("não deveria extrair") }
        val bytes = FixturesPlanilhaXp.bytes()
        val lida = importar(extratorQueNaoPodeSerChamado, bytes).shouldBeInstanceOf<ResultadoImportacao.PlanilhaLida>()
        lida.tamanhoBytes shouldBe bytes.size
        lida.planilha.dataPosicao shouldBe LocalDate.of(2026, 6, 21)
        lida.planilha.itens.size shouldBe 12
    }

    @Test
    fun `outra planilha ou zip corrompido viram falhas explicitas`() {
        val outra = EscritorXlsx.gerar(listOf(listOf("Ativo", "Valor")))
        importar(conteudo = outra) shouldBe ResultadoImportacao.Falha(MotivoFalha.PLANILHA_NAO_RECONHECIDA)
        importar(conteudo = "PK\u0003\u0004 quebrado".toByteArray()) shouldBe ResultadoImportacao.Falha(MotivoFalha.ARQUIVO_ILEGIVEL)
    }

    @Test
    fun `arquivo vazio e grande demais`() {
        importar(conteudo = ByteArray(0)) shouldBe ResultadoImportacao.Falha(MotivoFalha.ARQUIVO_VAZIO)
        val grande = ByteArray(ImportarRelatorio.TAMANHO_MAXIMO_BYTES + 1).also { "%PDF".toByteArray().copyInto(it) }
        importar(conteudo = grande) shouldBe ResultadoImportacao.Falha(MotivoFalha.ARQUIVO_GRANDE_DEMAIS)
    }

    @Test
    fun `PDF protegido e ilegivel viram falhas explicitas`() {
        importar(extrator = { throw PdfProtegidoException() }) shouldBe ResultadoImportacao.Falha(MotivoFalha.PDF_PROTEGIDO)
        importar(extrator = { throw ArquivoIlegivelException() }) shouldBe ResultadoImportacao.Falha(MotivoFalha.ARQUIVO_ILEGIVEL)
    }

    @Test
    fun `R1 - relatorio sem data exige data informada`() {
        val semData = FixturesXPerformance.sintetico().map { it.replace(Regex("Data de referência: \\d{2}/\\d{2}/\\d{4}"), "") }
        val lido = importar(extrator = { semData }).shouldBeInstanceOf<ResultadoImportacao.Lido>()
        lido.revisao.rascunho shouldBe null
        lido.revisao.validacao.problemas.first() shouldBe Problema.DataReferenciaAusente
    }
}
