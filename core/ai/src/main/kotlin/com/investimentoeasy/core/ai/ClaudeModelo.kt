package com.investimentoeasy.core.ai

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.JsonValue
import com.anthropic.errors.AnthropicIoException
import com.anthropic.errors.AnthropicServiceException
import com.anthropic.errors.PermissionDeniedException
import com.anthropic.errors.RateLimitException
import com.anthropic.errors.UnauthorizedException
import com.anthropic.models.messages.CacheControlEphemeral
import com.anthropic.models.messages.JsonOutputFormat
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.OutputConfig
import com.anthropic.models.messages.StopReason
import com.anthropic.models.messages.TextBlockParam
import com.anthropic.models.messages.ThinkingConfigAdaptive
import java.time.Duration

/**
 * Camada 3 pela API do Claude (SDK Java oficial): adaptive thinking, esforço alto, saída
 * estruturada no esquema da análise e fallback de servidor em caso de recusa.
 *
 * A chave é do próprio usuário (app pessoal, ADR-0002) e não sai do aparelho para outro lugar.
 */
public class ClaudeModelo(
    private val cliente: AnthropicClient,
    private val modelo: String = MODELO_PADRAO,
) : ModeloDeLinguagem {
    override fun gerar(pedido: PedidoAoModelo): RespostaDoModelo =
        try {
            val resposta = cliente.messages().create(parametros(pedido))
            when (resposta.stopReason().orElse(null)) {
                StopReason.REFUSAL -> RespostaDoModelo.Recusa
                StopReason.MAX_TOKENS -> RespostaDoModelo.Incompleta
                else -> {
                    val texto = resposta.content().flatMap { bloco -> bloco.text().map { listOf(it.text()) }.orElse(emptyList()) }
                    texto.joinToString("").takeIf { it.isNotBlank() }?.let { RespostaDoModelo.Texto(it, resposta.model().asString()) }
                        ?: RespostaDoModelo.Erro("Resposta sem texto")
                }
            }
        } catch (_: UnauthorizedException) {
            RespostaDoModelo.ChaveInvalida
        } catch (_: PermissionDeniedException) {
            RespostaDoModelo.ChaveInvalida
        } catch (_: RateLimitException) {
            RespostaDoModelo.LimiteDeUso
        } catch (e: AnthropicServiceException) {
            RespostaDoModelo.Erro("Erro da API (${e.statusCode()})")
        } catch (_: AnthropicIoException) {
            RespostaDoModelo.SemConexao
        }

    private fun parametros(pedido: PedidoAoModelo): MessageCreateParams =
        MessageCreateParams
            .builder()
            .model(modelo)
            .maxTokens(MAX_TOKENS)
            .thinking(ThinkingConfigAdaptive.builder().build())
            .outputConfig(
                OutputConfig
                    .builder()
                    .effort(OutputConfig.Effort.HIGH)
                    .format(
                        JsonOutputFormat
                            .builder()
                            .schema(
                                JsonOutputFormat.Schema
                                    .builder()
                                    .apply { pedido.esquema.forEach { (k, v) -> putAdditionalProperty(k, JsonValue.from(v)) } }
                                    .build(),
                            ).build(),
                    ).build(),
            )
            // O prompt de sistema é estável entre análises: fica em cache.
            .systemOfTextBlockParams(
                listOf(TextBlockParam.builder().text(pedido.sistema).cacheControl(CacheControlEphemeral.builder().build()).build()),
            ).addUserMessage(pedido.mensagem)
            // Fallback de servidor quando o modelo recusa: o roteamento por categoria fica com a API.
            .putAdditionalHeader("anthropic-beta", BETA_FALLBACK)
            .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
            .build()

    public companion object {
        public const val MODELO_PADRAO: String = "claude-opus-5"
        internal const val BETA_FALLBACK = "server-side-fallback-2026-07-01"
        private const val MAX_TOKENS = 16_000L
        private val TIMEOUT: Duration = Duration.ofMinutes(5)

        public fun comChave(
            chave: String,
            baseUrl: String? = null,
        ): ClaudeModelo =
            ClaudeModelo(
                AnthropicOkHttpClient
                    .builder()
                    .apiKey(chave)
                    .timeout(TIMEOUT)
                    .apply { baseUrl?.let(::baseUrl) }
                    .build(),
            )
    }
}
