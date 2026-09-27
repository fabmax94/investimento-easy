package com.investimentoeasy.core.documentos

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.investimentoeasy.core.importacao.ImportarRelatorio
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class LeitorDeArquivoTest {
    @get:Rule
    val pasta = TemporaryFolder()

    private val leitor = LeitorDeArquivo(ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver)

    @Test
    fun `le nome e conteudo do arquivo escolhido`() {
        val arquivo = pasta.newFile("XPerformance.pdf").apply { writeBytes("%PDF-1.7 teste".toByteArray()) }
        val lido = leitor.ler(Uri.fromFile(arquivo))
        lido.nome shouldBe "XPerformance.pdf"
        lido.conteudo.decodeToString() shouldBe "%PDF-1.7 teste"
    }

    @Test
    fun `arquivo grande e lido so ate o limite mais um byte`() {
        val limite = ImportarRelatorio.TAMANHO_MAXIMO_BYTES
        val arquivo = pasta.newFile("grande.pdf").apply { writeBytes(ByteArray(limite + 5_000)) }
        leitor.ler(Uri.fromFile(arquivo)).conteudo.size shouldBe limite + 1
    }

    @Test
    fun `arquivo inexistente vira IOException`() {
        shouldThrow<IOException> { leitor.ler(Uri.fromFile(File(pasta.root, "nao-existe.pdf"))) }
    }
}
