package com.investimentoeasy.core.documentos

import android.content.Context
import com.investimentoeasy.core.importacao.ArquivoIlegivelException
import com.investimentoeasy.core.importacao.ExtratorDeTextoPdf
import com.investimentoeasy.core.importacao.PdfProtegidoException
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.text.PDFTextStripper
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Texto de cada página com PdfBox-Android, ordenado pela posição na página: o mesmo modo
 * usado para escrever o parser (T1.8).
 */
@Singleton
internal class ExtratorPdfBox
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : ExtratorDeTextoPdf {
        init {
            PDFBoxResourceLoader.init(context)
        }

        override fun paginas(conteudo: ByteArray): List<String> =
            try {
                PDDocument.load(conteudo).use { documento ->
                    val stripper = PDFTextStripper().apply { sortByPosition = true }
                    (1..documento.numberOfPages).map { pagina ->
                        stripper.startPage = pagina
                        stripper.endPage = pagina
                        stripper.getText(documento)
                    }
                }
            } catch (e: InvalidPasswordException) {
                throw PdfProtegidoException(e)
            } catch (e: IOException) {
                throw ArquivoIlegivelException(e)
            }
    }
