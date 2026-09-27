package com.investimentoeasy.core.ai

import com.investimentoeasy.core.domain.analise.TipoLacuna
import kotlinx.serialization.Serializable

/**
 * Resposta do Claude nas 7 abas da skill. Por construção não há campo de tendência ou de
 * preço-alvo: Ritmo e Cenário vêm da Camada 1.
 */
@Serializable
public data class SaidaAnalise(
    val veredicto: String,
    val oQueFazer: OQueFazer,
    val mercado: TextoAba,
    val alocacao: Diagnosticos,
    val fundos: Notas,
    val fiis: Notas,
    val acoesEtfs: Notas,
    val alertas: Comentarios,
)

@Serializable
public data class OQueFazer(
    val acoes30Dias: List<Acao>,
    val realocacoes: List<Realocacao>,
    val novosAtivos: List<NovoAtivo>,
)

@Serializable
public data class Acao(
    val titulo: String,
    val detalhe: String,
)

/** Toda venda precisa de destino (skill, seção 7). */
@Serializable
public data class Realocacao(
    val vender: String,
    val motivo: String,
    val destino: String,
    /** Valor decidido pelo modelo, em reais ("20000.00"); validado contra o saldo do ativo. */
    val valor: String,
)

public enum class Prioridade { ALTA, MEDIA, ESPECULATIVO }

/** Toda sugestão de compra aponta para uma lacuna calculada na Camada 1 e diz o tamanho em R$. */
@Serializable
public data class NovoAtivo(
    val sugestao: String,
    val lacuna: TipoLacuna,
    val prioridade: Prioridade,
    val valor: String,
    val justificativa: String,
)

@Serializable
public data class TextoAba(
    val analise: String,
)

public enum class StatusDiagnostico { OK, ATENCAO, CRITICO }

@Serializable
public data class Diagnostico(
    val status: StatusDiagnostico,
    val texto: String,
)

@Serializable
public data class Diagnosticos(
    val diagnostico: List<Diagnostico>,
)

@Serializable
public data class Notas(
    val notas: List<String>,
)

@Serializable
public data class ComentarioAlerta(
    val regra: String,
    val texto: String,
)

@Serializable
public data class Comentarios(
    val comentarios: List<ComentarioAlerta>,
)

/** JSON Schema da saída (structured outputs): todo objeto fechado e todo campo obrigatório. */
internal object EsquemaSaida {
    private fun texto() = mapOf("type" to "string")

    private fun lista(item: Any) = mapOf("type" to "array", "items" to item)

    private fun enumDe(valores: List<String>) = mapOf("type" to "string", "enum" to valores)

    private fun objeto(vararg campos: Pair<String, Any>) =
        mapOf(
            "type" to "object",
            "properties" to campos.toMap(),
            "required" to campos.map { it.first },
            "additionalProperties" to false,
        )

    val raiz: Map<String, Any> =
        objeto(
            "veredicto" to texto(),
            "oQueFazer" to
                objeto(
                    "acoes30Dias" to lista(objeto("titulo" to texto(), "detalhe" to texto())),
                    "realocacoes" to lista(objeto("vender" to texto(), "motivo" to texto(), "destino" to texto(), "valor" to texto())),
                    "novosAtivos" to
                        lista(
                            objeto(
                                "sugestao" to texto(),
                                "lacuna" to enumDe(TipoLacuna.entries.map { it.name }),
                                "prioridade" to enumDe(Prioridade.entries.map { it.name }),
                                "valor" to texto(),
                                "justificativa" to texto(),
                            ),
                        ),
                ),
            "mercado" to objeto("analise" to texto()),
            "alocacao" to
                objeto("diagnostico" to lista(objeto("status" to enumDe(StatusDiagnostico.entries.map { it.name }), "texto" to texto()))),
            "fundos" to objeto("notas" to lista(texto())),
            "fiis" to objeto("notas" to lista(texto())),
            "acoesEtfs" to objeto("notas" to lista(texto())),
            "alertas" to objeto("comentarios" to lista(objeto("regra" to texto(), "texto" to texto()))),
        )
}

private val JSON_SAIDA = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

/** JSON guardado no banco (já validado quando foi gerado). */
public fun SaidaAnalise.paraJson(): String = JSON_SAIDA.encodeToString(SaidaAnalise.serializer(), this)

/** `null` se o JSON guardado não corresponde mais ao esquema (ex.: versão antiga do app). */
public fun saidaDeJson(json: String): SaidaAnalise? =
    try {
        JSON_SAIDA.decodeFromString(SaidaAnalise.serializer(), json)
    } catch (_: kotlinx.serialization.SerializationException) {
        null
    }
