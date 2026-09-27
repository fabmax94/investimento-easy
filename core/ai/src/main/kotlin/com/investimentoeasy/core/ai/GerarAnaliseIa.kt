package com.investimentoeasy.core.ai

import com.investimentoeasy.core.domain.analise.AnaliseDeterministica
import com.investimentoeasy.core.domain.complemento.ComplementoPlanilha
import com.investimentoeasy.core.model.Snapshot
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

public sealed interface ResultadoAnaliseIa {
    public data class Gerada(
        val saida: SaidaAnalise,
        val modelo: String,
        val versaoPrompt: String,
        val tentativas: Int,
    ) : ResultadoAnaliseIa

    /** Mesmo depois da segunda tentativa, a resposta citou números sem origem ou sugestões inválidas. */
    public data class Rejeitada(val violacoes: List<Violacao>) : ResultadoAnaliseIa

    public data class Falhou(val motivo: RespostaDoModelo) : ResultadoAnaliseIa
}

/**
 * Camada 3: o Claude recebe os números prontos (camadas 1 e 2) e escreve diagnóstico e plano.
 * A resposta passa pelo [ValidadorDeNumeros]; se reprovar, há uma segunda tentativa com a lista
 * do que foi rejeitado. Reprovando de novo, nada é mostrado como análise.
 */
public class GerarAnaliseIa(
    private val modelo: ModeloDeLinguagem,
) {
    public operator fun invoke(
        snapshot: Snapshot,
        analise: AnaliseDeterministica,
        complemento: ComplementoPlanilha? = null,
    ): ResultadoAnaliseIa {
        val entrada = montarEntrada(snapshot, analise, complemento)
        val validador = ValidadorDeNumeros(entrada)
        val dados = JSON.encodeToString(EntradaAnalise.serializer(), entrada)
        var resultado = tentar(dados, correcao = null, validador, numero = 1)
        if (resultado is ResultadoAnaliseIa.Rejeitada) {
            resultado = tentar(dados, descrever(resultado.violacoes), validador, numero = 2)
        }
        return resultado
    }

    private fun tentar(
        dados: String,
        correcao: String?,
        validador: ValidadorDeNumeros,
        numero: Int,
    ): ResultadoAnaliseIa {
        val resposta = modelo.gerar(PedidoAoModelo(PROMPT, mensagem(dados, correcao), EsquemaSaida.raiz))
        if (resposta !is RespostaDoModelo.Texto) return ResultadoAnaliseIa.Falhou(resposta)
        val saida =
            try {
                JSON.decodeFromString(SaidaAnalise.serializer(), resposta.json)
            } catch (e: SerializationException) {
                return ResultadoAnaliseIa.Falhou(RespostaDoModelo.Erro("JSON fora do esquema: ${e.message}"))
            }
        val violacoes = validador.validar(saida)
        return if (violacoes.isEmpty()) {
            ResultadoAnaliseIa.Gerada(saida, resposta.modelo, VERSAO_PROMPT, numero)
        } else {
            ResultadoAnaliseIa.Rejeitada(violacoes)
        }
    }

    private fun mensagem(
        dados: String,
        correcao: String?,
    ): String =
        buildString {
            append("Analise a carteira a partir destes dados (JSON):\n\n").append(dados)
            if (correcao != null) {
                append("\n\nA resposta anterior foi rejeitada pelo validador. Corrija sem repetir estes problemas:\n").append(correcao)
            }
        }

    private fun descrever(violacoes: List<Violacao>): String =
        violacoes.joinToString("\n") { v ->
            when (v) {
                is Violacao.NumeroSemOrigem ->
                    "- O número ${v.numero} não existe nos dados (trecho: \"${v.trecho}\"). Use só números do JSON."
                is Violacao.ValorInvalido ->
                    "- ${v.campo} = \"${v.valor}\" é inválido: use um decimal positivo, até o saldo ou o patrimônio."
                is Violacao.LacunaInexistente -> "- A lacuna ${v.lacuna} não está em `lacunas`."
                is Violacao.VendaInvalida -> "- \"${v.ativo}\" não é uma posição do JSON, ou a venda está sem destino."
            }
        }

    public companion object {
        public const val VERSAO_PROMPT: String = "analise-v2"
        private val JSON = Json { ignoreUnknownKeys = true }
        internal val PROMPT: String =
            requireNotNull(GerarAnaliseIa::class.java.getResource("/prompts/$VERSAO_PROMPT.md")) { "Prompt $VERSAO_PROMPT ausente" }
                .readText()
    }
}
