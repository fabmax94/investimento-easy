package com.investimentoeasy.core.mercado

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.io.InputStream

/** Falha esperada de uma fonte (HTTP fora de 2xx, corpo inesperado); vira aviso, não crash. */
internal class FonteIndisponivelException(
    mensagem: String,
    causa: Throwable? = null,
) : IOException(mensagem, causa)

internal val JSON = Json { ignoreUnknownKeys = true }

/** Alguns serviços (Yahoo) recusam requisições sem User-Agent de navegador. */
private const val USER_AGENT = "Mozilla/5.0 (Android) MonitorDeCarteira"

internal fun OkHttpClient.json(url: HttpUrl): JsonElement = abrir(url) { JSON.parseToJsonElement(it.readBytes().decodeToString()) }

internal fun <T> OkHttpClient.abrir(
    url: HttpUrl,
    ler: (InputStream) -> T,
): T {
    val pedido = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
    newCall(pedido).execute().use { resposta ->
        if (!resposta.isSuccessful) throw FonteIndisponivelException("HTTP ${resposta.code}")
        val corpo = resposta.body ?: throw FonteIndisponivelException("resposta vazia")
        return corpo.byteStream().use(ler)
    }
}
