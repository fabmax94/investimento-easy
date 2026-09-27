package com.investimentoeasy.parser.xlsx

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class LeitorXlsxTest {
    private val leitor = LeitorXlsx()

    @Test
    fun `le textos compartilhados e em linha, pela coluna`() {
        for (compartilhados in listOf(true, false)) {
            val aba =
                leitor.primeiraAba(
                    EscritorXlsx.gerar(listOf(listOf("a", "", "c & <d>"), emptyList(), listOf("x")), "Aba 1", compartilhados),
                )
            aba.nome shouldBe "Aba 1"
            aba.linhas.map { it.numero } shouldBe listOf(1, 2, 3)
            aba.linhas[0].celulas shouldBe mapOf(0 to "a", 2 to "c & <d>")
            aba.linhas[1].celulas shouldBe emptyMap()
            aba.linhas[2][0] shouldBe "x"
        }
    }

    @Test
    fun `preenchidas ignora celulas so com espacos`() {
        val aba = leitor.primeiraAba(EscritorXlsx.gerar(listOf(listOf(" ", " b "))))
        aba.linhas[0].preenchidas shouldBe mapOf(1 to "b")
    }

    @Test
    fun `numero e texto rico com trechos formatados`() {
        val folha =
            """<worksheet xmlns="$NS"><sheetData><row r="1">""" +
                """<c r="B1"><v>42.5</v></c><c r="C1" t="s"><v>0</v></c></row></sheetData></worksheet>"""
        val compartilhados = """<sst xmlns="$NS"><si><r><t>Ren</t></r><r><t>da</t></r><rPh><t>x</t></rPh></si></sst>"""
        val aba = leitor.primeiraAba(xlsx(folha, compartilhados))
        aba.linhas[0].celulas shouldBe mapOf(1 to "42.5", 2 to "Renda")
    }

    @Test
    fun `colunas de duas letras`() {
        LeitorXlsx.coluna("A1") shouldBe 0
        LeitorXlsx.coluna("M43") shouldBe 12
        LeitorXlsx.coluna("Z9") shouldBe 25
        LeitorXlsx.coluna("AA7") shouldBe 26
    }

    @Test
    fun `sem relacoes usa a primeira folha padrao`() {
        val partes =
            mapOf(
                "xl/workbook.xml" to """<workbook xmlns="$NS"><sheets><sheet name="S" sheetId="1"/></sheets></workbook>""",
                "xl/worksheets/sheet1.xml" to
                    """<worksheet xmlns="$NS"><sheetData><row r="1">""" +
                    """<c r="A1" t="inlineStr"><is><t>ok</t></is></c></row></sheetData></worksheet>""",
            )
        leitor.primeiraAba(EscritorXlsx.zip(partes.mapValues { it.value.toByteArray() })).linhas[0][0] shouldBe "ok"
    }

    @Test
    fun `arquivo que nao e xlsx`() {
        shouldThrow<PlanilhaIlegivelException> { leitor.primeiraAba("%PDF-1.7 nada".toByteArray()) }
        shouldThrow<PlanilhaIlegivelException> { leitor.primeiraAba(EscritorXlsx.zip(mapOf("a.txt" to "x".toByteArray()))) }
    }

    @Test
    fun `xml invalido ou sem abas`() {
        shouldThrow<PlanilhaIlegivelException> { leitor.primeiraAba(xlsx("<worksheet", null)) }
        val semAbas = EscritorXlsx.zip(mapOf("xl/workbook.xml" to """<workbook xmlns="$NS"><sheets/></workbook>""".toByteArray()))
        shouldThrow<PlanilhaIlegivelException> { leitor.primeiraAba(semAbas) }
    }

    @Test
    fun `recusa DTD para nao expandir entidades`() {
        val folha =
            """<?xml version="1.0"?><!DOCTYPE x [<!ENTITY e SYSTEM "file:///etc/passwd">]>""" +
                """<worksheet xmlns="$NS"><sheetData/></worksheet>"""
        shouldThrow<PlanilhaIlegivelException> { leitor.primeiraAba(xlsx(folha, null)) }
    }

    @Test
    fun `limita o tamanho descompactado`() {
        val grande = FixturesPlanilhaXp.bytes()
        shouldThrow<PlanilhaIlegivelException> { LeitorXlsx(limiteEntradaBytes = 100).primeiraAba(grande) }
        shouldThrow<PlanilhaIlegivelException> { LeitorXlsx(limiteTotalBytes = 1_000).primeiraAba(grande) }
    }

    private fun xlsx(
        folha: String,
        compartilhados: String?,
    ): ByteArray {
        val partes =
            buildMap {
                put(
                    "xl/workbook.xml",
                    """<workbook xmlns="$NS" xmlns:r="$NS_R"><sheets><sheet name="S" sheetId="1" r:id="rId1"/></sheets></workbook>""",
                )
                put(
                    "xl/_rels/workbook.xml.rels",
                    """<Relationships xmlns="$NS_RELS">""" +
                        """<Relationship Id="rId1" Type="w" Target="/xl/worksheets/sheet1.xml"/></Relationships>""",
                )
                put("xl/worksheets/sheet1.xml", folha)
                compartilhados?.let { put("xl/sharedStrings.xml", it) }
            }
        return EscritorXlsx.zip(partes.mapValues { it.value.toByteArray() })
    }

    private companion object {
        const val NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
        const val NS_R = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
        const val NS_RELS = "http://schemas.openxmlformats.org/package/2006/relationships"
    }
}
