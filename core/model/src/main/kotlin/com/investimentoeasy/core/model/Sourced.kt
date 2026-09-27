package com.investimentoeasy.core.model

/**
 * Origem de um dado. Regra zero: todo número exibido carrega a sua origem.
 */
public enum class Origem {
    /** Veio do arquivo enviado pelo usuário, lido por parser determinístico. */
    RELATORIO,

    /** Veio de uma fonte pública de mercado (cotação, cota, taxa). */
    MERCADO,

    /** Calculado pelo app a partir de outros dados com origem conhecida. */
    CALCULADO,

    /** Projeção entre uploads (acompanhamento diário). */
    ESTIMADO,

    /** Extraído por IA; fica assim marcado até a confirmação do usuário (R2). */
    IA,
}

/** Um valor acompanhado da sua origem. Ausência de dado é `null`, nunca interpolação. */
public data class Sourced<out T>(
    val valor: T,
    val origem: Origem,
)

public fun <T> T.doRelatorio(): Sourced<T> = Sourced(this, Origem.RELATORIO)

public fun <T> T.calculado(): Sourced<T> = Sourced(this, Origem.CALCULADO)
