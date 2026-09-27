package com.investimentoeasy.core.ai

import com.anthropic.client.okhttp.AnthropicOkHttpClient
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

/** A requisição real que o SDK monta, contra um servidor falso: nenhuma chamada à API de verdade. */
class ClaudeModeloTest {
    private val servidor = MockWebServer().apply { start() }

    @AfterEach
    fun parar() = servidor.shutdown()

    private val modelo =
        ClaudeModelo(
            AnthropicOkHttpClient
                .builder()
                .apiKey("chave-de-teste")
                .baseUrl(servidor.url("/").toString().removeSuffix("/"))
                .maxRetries(0)
                .build(),
        )

    private val pedido = PedidoAoModelo("sistema estável", "dados da carteira", EsquemaSaida.raiz)

    private fun mensagem(
        texto: String,
        stopReason: String = "end_turn",
    ) = MockResponse()
        .setHeader("content-type", "application/json")
        .setBody(
            """{"id":"msg_1","type":"message","role":"assistant","model":"claude-opus-5",""" +
                """"content":[{"type":"text","text":${Json.encodeToString(String.serializer(), texto)}}],""" +
                """"stop_reason":"$stopReason","stop_sequence":null,"usage":{"input_tokens":10,"output_tokens":5}}""",
        )

    @Test
    fun `monta a requisicao com modelo, thinking adaptativo, esquema, cache e fallback`() {
        servidor.enqueue(mensagem("{\"ok\":true}"))
        modelo.gerar(pedido) shouldBe RespostaDoModelo.Texto("{\"ok\":true}", "claude-opus-5")

        val requisicao = servidor.takeRequest()
        requisicao.path shouldBe "/v1/messages"
        requisicao.getHeader("x-api-key") shouldBe "chave-de-teste"
        requisicao.getHeader("anthropic-beta") shouldBe ClaudeModelo.BETA_FALLBACK
        val corpo = Json.parseToJsonElement(requisicao.body.readUtf8()).jsonObject
        corpo["model"]!!.jsonPrimitive.content shouldBe "claude-opus-5"
        corpo["thinking"]!!.jsonObject["type"]!!.jsonPrimitive.content shouldBe "adaptive"
        corpo["fallbacks"]!!.jsonPrimitive.content shouldBe "default"
        val saida = corpo["output_config"]!!.jsonObject
        saida["effort"]!!.jsonPrimitive.content shouldBe "high"
        saida["format"]!!.jsonObject["type"]!!.jsonPrimitive.content shouldBe "json_schema"
        (saida["format"]!!.jsonObject["schema"] as JsonObject)["required"]!!.jsonArray.size shouldBe 8
        val sistema = corpo["system"]!!.jsonArray.single().jsonObject
        sistema["text"]!!.jsonPrimitive.content shouldBe "sistema estável"
        sistema["cache_control"]!!.jsonObject["type"]!!.jsonPrimitive.content shouldBe "ephemeral"
    }

    @Test
    fun `recusa e resposta cortada nao viram texto`() {
        servidor.enqueue(mensagem("", stopReason = "refusal"))
        modelo.gerar(pedido) shouldBe RespostaDoModelo.Recusa
        servidor.enqueue(mensagem("{\"parcial\":", stopReason = "max_tokens"))
        modelo.gerar(pedido) shouldBe RespostaDoModelo.Incompleta
    }

    @Test
    fun `erros HTTP viram motivos claros`() {
        fun erro(
            status: Int,
            tipo: String,
        ) = MockResponse()
            .setResponseCode(status)
            .setHeader("content-type", "application/json")
            .setBody("""{"type":"error","error":{"type":"$tipo","message":"x"}}""")

        servidor.enqueue(erro(401, "authentication_error"))
        modelo.gerar(pedido) shouldBe RespostaDoModelo.ChaveInvalida
        servidor.enqueue(erro(429, "rate_limit_error"))
        modelo.gerar(pedido) shouldBe RespostaDoModelo.LimiteDeUso
        servidor.enqueue(erro(500, "api_error"))
        modelo.gerar(pedido).shouldBeInstanceOf<RespostaDoModelo.Erro>()
    }

    @Test
    fun `sem conexao`() {
        servidor.shutdown()
        modelo.gerar(pedido) shouldBe RespostaDoModelo.SemConexao
    }
}
