package com.investimentoeasy.parser.xlsx

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Gera um .xlsx mínimo em memória para os testes: nenhuma planilha real (com conta e assessor)
 * precisa ser versionada (R7). As linhas vazias da lista viram linhas sem células.
 */
public object EscritorXlsx {
    public fun gerar(
        linhas: List<List<String>>,
        nomeAba: String = "Sua carteira",
        textosCompartilhados: Boolean = true,
    ): ByteArray {
        val compartilhados = linkedMapOf<String, Int>()
        val folha =
            buildString {
                append("""<?xml version="1.0" encoding="UTF-8"?><worksheet xmlns="$NS"><sheetData>""")
                linhas.forEachIndexed { i, celulas ->
                    append("""<row r="${i + 1}">""")
                    celulas.forEachIndexed { coluna, texto ->
                        if (texto.isEmpty()) return@forEachIndexed
                        val ref = "${letra(coluna)}${i + 1}"
                        if (textosCompartilhados) {
                            val indice = compartilhados.getOrPut(texto) { compartilhados.size }
                            append("""<c r="$ref" t="s"><v>$indice</v></c>""")
                        } else {
                            append("""<c r="$ref" t="inlineStr"><is><t>${escapar(texto)}</t></is></c>""")
                        }
                    }
                    append("</row>")
                }
                append("</sheetData></worksheet>")
            }
        val partes =
            buildMap {
                put("[Content_Types].xml", """<?xml version="1.0" encoding="UTF-8"?><Types xmlns="$NS_TIPOS"/>""")
                put(
                    "xl/workbook.xml",
                    """<?xml version="1.0" encoding="UTF-8"?><workbook xmlns="$NS" xmlns:r="$NS_R">""" +
                        """<sheets><sheet name="${escapar(nomeAba)}" sheetId="1" r:id="rId4"/></sheets></workbook>""",
                )
                put(
                    "xl/_rels/workbook.xml.rels",
                    """<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="$NS_RELS">""" +
                        """<Relationship Id="rId4" Type="worksheet" Target="worksheets/sheet1.xml"/></Relationships>""",
                )
                put("xl/worksheets/sheet1.xml", folha)
                if (compartilhados.isNotEmpty()) {
                    put(
                        "xl/sharedStrings.xml",
                        """<?xml version="1.0" encoding="UTF-8"?><sst xmlns="$NS">""" +
                            compartilhados.keys.joinToString("") { "<si><t>${escapar(it)}</t></si>" } + "</sst>",
                    )
                }
            }
        return zip(partes.mapValues { it.value.toByteArray(Charsets.UTF_8) })
    }

    public fun zip(partes: Map<String, ByteArray>): ByteArray {
        val saida = ByteArrayOutputStream()
        ZipOutputStream(saida).use { zip ->
            partes.forEach { (nome, bytes) ->
                zip.putNextEntry(ZipEntry(nome))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return saida.toByteArray()
    }

    private fun letra(coluna: Int): String =
        if (coluna < LETRAS) ('A' + coluna).toString() else letra(coluna / LETRAS - 1) + letra(coluna % LETRAS)

    private fun escapar(texto: String): String =
        texto.replace(
            "&",
            "&amp;",
        ).replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private const val LETRAS = 26
    private const val NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
    private const val NS_R = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
    private const val NS_RELS = "http://schemas.openxmlformats.org/package/2006/relationships"
    private const val NS_TIPOS = "http://schemas.openxmlformats.org/package/2006/content-types"
}
