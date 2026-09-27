package com.investimentoeasy.parser.xlsx

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.ZipException
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException

/** Uma linha da planilha: texto de cada célula preenchida, pelo índice da coluna (A = 0). */
public data class LinhaPlanilha(
    val numero: Int,
    val celulas: Map<Int, String>,
) {
    public operator fun get(coluna: Int): String? = celulas[coluna]

    /** Células não vazias depois de tirar os espaços. */
    val preenchidas: Map<Int, String> get() = celulas.mapValues { it.value.trim() }.filterValues(String::isNotEmpty)
}

public data class Aba(
    val nome: String,
    val linhas: List<LinhaPlanilha>,
)

public class PlanilhaIlegivelException(
    mensagem: String,
    causa: Throwable? = null,
) : Exception(mensagem, causa)

/**
 * Leitor mínimo de .xlsx (um zip de XMLs): só o texto das células da primeira aba, sem fórmulas nem
 * estilos. Apache POI seria pesado demais para o app.
 *
 * O arquivo vem de fora, então o leitor se defende: recusa DTD (XXE), limita o número de entradas e
 * o tamanho descompactado (zip bomb).
 */
public class LeitorXlsx(
    private val limiteEntradaBytes: Int = LIMITE_ENTRADA_BYTES,
    private val limiteTotalBytes: Long = LIMITE_TOTAL_BYTES,
) {
    /** @throws PlanilhaIlegivelException se não for um .xlsx legível. */
    public fun primeiraAba(conteudo: ByteArray): Aba {
        val entradas = descompactar(conteudo)
        val workbook = xml(entradas, WORKBOOK)
        val folha = workbook.elementos("sheet").firstOrNull() ?: ilegivel("Planilha sem abas")
        val caminho = caminhoDaFolha(entradas, folha)
        val compartilhados = entradas[SHARED_STRINGS]?.let { textosCompartilhados(documento(it)) }.orEmpty()
        return Aba(folha.getAttribute("name"), linhas(xml(entradas, caminho), compartilhados))
    }

    private fun caminhoDaFolha(
        entradas: Map<String, ByteArray>,
        folha: Element,
    ): String {
        val id = folha.getAttributeNS(NS_RELACOES, "id").ifEmpty { folha.getAttribute("r:id") }
        val alvo =
            entradas[WORKBOOK_RELS]
                ?.let(::documento)
                ?.elementos("Relationship")
                ?.firstOrNull { it.getAttribute("Id") == id }
                ?.getAttribute("Target")
        return when {
            alvo == null -> FOLHA_PADRAO
            alvo.startsWith("/") -> alvo.removePrefix("/")
            else -> "xl/$alvo"
        }
    }

    private fun linhas(
        folha: Document,
        compartilhados: List<String>,
    ): List<LinhaPlanilha> =
        folha.elementos("row").map { linha ->
            val celulas =
                linha
                    .filhos("c")
                    .mapNotNull { celula -> texto(celula, compartilhados)?.let { coluna(celula.getAttribute("r")) to it } }
                    .toMap()
            LinhaPlanilha(linha.getAttribute("r").toIntOrNull() ?: 0, celulas)
        }

    private fun texto(
        celula: Element,
        compartilhados: List<String>,
    ): String? {
        val valor = celula.filhos("v").firstOrNull()?.textContent
        return when (celula.getAttribute("t")) {
            "s" -> valor?.toIntOrNull()?.let(compartilhados::getOrNull)
            "inlineStr" -> celula.filhos("is").firstOrNull()?.let(::textoRico)
            else -> valor
        }
    }

    private fun textosCompartilhados(documento: Document): List<String> = documento.elementos("si").map(::textoRico)

    /** Texto de `<si>`/`<is>`: junta os `<t>`, inclusive os de trechos formatados (`<r>`), e ignora a fonética. */
    private fun textoRico(elemento: Element): String =
        elemento
            .elementos("t")
            .filterNot { it.parentNode?.localName == "rPh" }
            .joinToString("") { it.textContent }

    private fun xml(
        entradas: Map<String, ByteArray>,
        caminho: String,
    ): Document = documento(entradas[caminho] ?: ilegivel("Parte ausente: $caminho"))

    private fun descompactar(conteudo: ByteArray): Map<String, ByteArray> {
        val entradas =
            ZipSeguro(limiteEntradaBytes, limiteTotalBytes, LIMITE_ENTRADAS).descompactar(conteudo) { nome ->
                nome in PARTES_LIDAS || nome.startsWith(PASTA_FOLHAS)
            }
        if (WORKBOOK !in entradas) ilegivel("Não é um arquivo .xlsx")
        return entradas
    }

    public companion object {
        public const val LIMITE_ENTRADA_BYTES: Int = 10 * 1024 * 1024
        public const val LIMITE_TOTAL_BYTES: Long = 30L * 1024 * 1024
        private const val LIMITE_ENTRADAS = 500
        private const val LETRAS = 26

        private const val WORKBOOK = "xl/workbook.xml"
        private const val WORKBOOK_RELS = "xl/_rels/workbook.xml.rels"
        private const val SHARED_STRINGS = "xl/sharedStrings.xml"
        private const val PASTA_FOLHAS = "xl/worksheets/sheet"
        private const val FOLHA_PADRAO = "xl/worksheets/sheet1.xml"
        private val PARTES_LIDAS = setOf(WORKBOOK, WORKBOOK_RELS, SHARED_STRINGS)
        private const val NS_RELACOES = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"

        /** "A1" -> 0, "M43" -> 12, "AA7" -> 26. */
        internal fun coluna(referencia: String): Int =
            referencia.takeWhile(Char::isLetter).uppercase().fold(0) { acc, letra -> acc * LETRAS + (letra - 'A' + 1) } - 1
    }
}

private fun Document.elementos(nome: String): List<Element> = documentElement.elementos(nome)

private fun Element.elementos(nome: String): List<Element> {
    val nos = getElementsByTagNameNS("*", nome)
    return (0 until nos.length).map { nos.item(it) as Element }
}

private fun Element.filhos(nome: String): List<Element> {
    val nos = childNodes
    return (0 until nos.length)
        .map(nos::item)
        .filter { it.nodeType == Node.ELEMENT_NODE && it.localName == nome }
        .map { it as Element }
}

internal fun ilegivel(
    mensagem: String,
    causa: Throwable? = null,
): Nothing = throw PlanilhaIlegivelException(mensagem, causa)

/** XML de fora: sem DTD (XXE) nem expansão de entidades. */
internal fun documento(bytes: ByteArray): Document {
    if (String(bytes, Charsets.UTF_8).contains("<!DOCTYPE", ignoreCase = true)) ilegivel("XML com DTD recusado")
    return try {
        fabricaSegura().newDocumentBuilder().parse(InputSource(ByteArrayInputStream(bytes)))
    } catch (e: SAXException) {
        ilegivel("XML inválido", e)
    } catch (e: IOException) {
        ilegivel("XML inválido", e)
    }
}

private fun fabricaSegura(): DocumentBuilderFactory =
    DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
        isExpandEntityReferences = false
        // Nem todo parser (ex.: o do Android) conhece a feature; a recusa de DTD acima já cobre.
        try {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        } catch (_: ParserConfigurationException) {
        }
    }

/** Descompacta só as partes pedidas, com limite por parte, no total e no número de entradas (zip bomb). */
internal class ZipSeguro(
    private val limiteEntradaBytes: Int,
    private val limiteTotalBytes: Long,
    private val limiteEntradas: Int,
) {
    fun descompactar(
        conteudo: ByteArray,
        quer: (String) -> Boolean,
    ): Map<String, ByteArray> =
        try {
            ZipInputStream(ByteArrayInputStream(conteudo)).use { lerEntradas(it, quer) }
        } catch (e: ZipException) {
            ilegivel("Não é um arquivo .xlsx", e)
        } catch (e: IOException) {
            ilegivel("Não é um arquivo .xlsx", e)
        }

    private fun lerEntradas(
        zip: ZipInputStream,
        quer: (String) -> Boolean,
    ): Map<String, ByteArray> {
        val entradas = mutableMapOf<String, ByteArray>()
        var total = 0L
        var contadas = 0
        generateSequence { zip.nextEntry }.filterNot { it.isDirectory }.forEach { entrada ->
            contadas++
            if (contadas > limiteEntradas) ilegivel("Entradas demais no arquivo")
            if (!quer(entrada.name)) return@forEach
            val bytes = lerComLimite(zip)
            total += bytes.size
            if (total > limiteTotalBytes) ilegivel("Arquivo descompactado grande demais")
            entradas[entrada.name] = bytes
        }
        return entradas
    }

    private fun lerComLimite(zip: ZipInputStream): ByteArray {
        val saida = ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER)
        var lidos = zip.read(buffer)
        while (lidos >= 0) {
            saida.write(buffer, 0, lidos)
            if (saida.size() > limiteEntradaBytes) ilegivel("Parte grande demais no arquivo")
            lidos = zip.read(buffer)
        }
        return saida.toByteArray()
    }

    private companion object {
        const val BUFFER = 8 * 1024
    }
}
