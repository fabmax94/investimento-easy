package com.investimentoeasy.core.ai

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class GerarAnaliseIaTest {
    private val json = Json

    private fun resposta(saida: SaidaAnalise) =
        RespostaDoModelo.Texto(
            json.encodeToString(SaidaAnalise.serializer(), saida),
            "claude-opus-5",
        )

    private class ModeloRoteirizado(
        val respostas: List<RespostaDoModelo>,
    ) : ModeloDeLinguagem {
        val pedidos = mutableListOf<PedidoAoModelo>()

        override fun gerar(pedido: PedidoAoModelo): RespostaDoModelo = respostas[pedidos.size].also { pedidos += pedido }
    }

    @Test
    fun `resposta valida vira analise com modelo e versao do prompt`() {
        val modelo = ModeloRoteirizado(listOf(resposta(Cenario.saida())))
        val gerada = GerarAnaliseIa(modelo)(Cenario.snapshot, Cenario.analise).shouldBeInstanceOf<ResultadoAnaliseIa.Gerada>()
        gerada.tentativas shouldBe 1
        gerada.modelo shouldBe "claude-opus-5"
        gerada.versaoPrompt shouldBe "analise-v1"
        val pedido = modelo.pedidos.single()
        pedido.sistema shouldContain "Regra zero"
        pedido.mensagem shouldContain "\"somaDasPosicoes\":\"50000.00\""
        pedido.esquema["additionalProperties"] shouldBe false
    }

    @Test
    fun `numero inventado gera segunda tentativa com a correcao, que passa`() {
        val modelo = ModeloRoteirizado(listOf(resposta(Cenario.saida(veredicto = "Rendeu 37,5%.")), resposta(Cenario.saida())))
        val gerada = GerarAnaliseIa(modelo)(Cenario.snapshot, Cenario.analise).shouldBeInstanceOf<ResultadoAnaliseIa.Gerada>()
        gerada.tentativas shouldBe 2
        modelo.pedidos shouldHaveSize 2
        modelo.pedidos[0].mensagem shouldNotContain "rejeitada"
        modelo.pedidos[1].mensagem shouldContain "O número 37,5 não existe nos dados"
    }

    @Test
    fun `duas respostas com numero inventado - nada e mostrado como analise`() {
        val ruim = resposta(Cenario.saida(veredicto = "Rendeu 37,5%."))
        val rejeitada =
            GerarAnaliseIa(ModeloRoteirizado(listOf(ruim, ruim)))(Cenario.snapshot, Cenario.analise)
                .shouldBeInstanceOf<ResultadoAnaliseIa.Rejeitada>()
        rejeitada.violacoes.single().shouldBeInstanceOf<Violacao.NumeroSemOrigem>()
    }

    @Test
    fun `falhas do modelo e JSON fora do esquema sao repassadas`() {
        listOf(RespostaDoModelo.Recusa, RespostaDoModelo.ChaveInvalida, RespostaDoModelo.SemConexao).forEach { falha ->
            GerarAnaliseIa(ModeloRoteirizado(listOf(falha)))(Cenario.snapshot, Cenario.analise) shouldBe ResultadoAnaliseIa.Falhou(falha)
        }
        GerarAnaliseIa(ModeloRoteirizado(listOf(RespostaDoModelo.Texto("{\"veredicto\": 1}", "m"))))(Cenario.snapshot, Cenario.analise)
            .shouldBeInstanceOf<ResultadoAnaliseIa.Falhou>()
            .motivo
            .shouldBeInstanceOf<RespostaDoModelo.Erro>()
    }

    @Test
    fun `prompt versionado nao tem dado pessoal`() {
        listOf("6371184", "Murilo", "Fabio", "272").forEach { GerarAnaliseIa.PROMPT shouldNotContain it }
    }
}
