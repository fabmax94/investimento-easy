package com.investimentoeasy.core.documentos

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import com.investimentoeasy.core.importacao.ArquivoRecebido
import com.investimentoeasy.core.importacao.ImportarRelatorio
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.inject.Inject

/** Lê o arquivo escolhido no seletor do sistema (Storage Access Framework). */
public class LeitorDeArquivo
    @Inject
    constructor(
        private val resolver: ContentResolver,
    ) {
        /**
         * Lê no máximo [ImportarRelatorio.TAMANHO_MAXIMO_BYTES] + 1 bytes: o suficiente para
         * a importação recusar arquivos grandes sem carregá-los inteiros na memória.
         *
         * @throws IOException se o arquivo não puder ser aberto.
         */
        public fun ler(uri: Uri): ArquivoRecebido {
            val nome =
                resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                } ?: uri.lastPathSegment ?: "arquivo"
            val conteudo =
                resolver.openInputStream(uri)?.use {
                        entrada ->
                    lerComLimite(entrada)
                } ?: throw IOException("Sem conteúdo: $uri")
            return ArquivoRecebido(nome, resolver.getType(uri), conteudo)
        }

        private fun lerComLimite(entrada: java.io.InputStream): ByteArray {
            val limite = ImportarRelatorio.TAMANHO_MAXIMO_BYTES + 1
            val saida = ByteArrayOutputStream()
            val buffer = ByteArray(BUFFER)
            while (saida.size() < limite) {
                val lidos = entrada.read(buffer, 0, minOf(buffer.size, limite - saida.size()))
                if (lidos < 0) break
                saida.write(buffer, 0, lidos)
            }
            return saida.toByteArray()
        }

        private companion object {
            const val BUFFER = 64 * 1024
        }
    }
