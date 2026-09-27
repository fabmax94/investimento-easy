package com.investimentoeasy.core.ai

/** Uma chamada ao modelo com saída JSON num esquema fixo. */
public fun interface ModeloDeLinguagem {
    public fun gerar(pedido: PedidoAoModelo): RespostaDoModelo
}

public data class PedidoAoModelo(
    val sistema: String,
    val mensagem: String,
    val esquema: Map<String, Any>,
)

public sealed interface RespostaDoModelo {
    public data class Texto(val json: String, val modelo: String) : RespostaDoModelo

    /** O modelo recusou (stop_reason "refusal"), mesmo com o fallback de servidor. */
    public data object Recusa : RespostaDoModelo

    /** A resposta foi cortada no limite de tokens. */
    public data object Incompleta : RespostaDoModelo

    public data object ChaveInvalida : RespostaDoModelo

    public data object LimiteDeUso : RespostaDoModelo

    public data object SemConexao : RespostaDoModelo

    public data class Erro(val mensagem: String) : RespostaDoModelo
}

/** Cria o modelo com a chave do usuário (lida do cofre só na hora de gerar). */
public fun interface FabricaDeModelo {
    public fun criar(chave: String): ModeloDeLinguagem
}
