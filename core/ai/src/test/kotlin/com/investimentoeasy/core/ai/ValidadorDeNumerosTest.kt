package com.investimentoeasy.core.ai

import com.investimentoeasy.core.domain.analise.TipoLacuna
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class ValidadorDeNumerosTest {
    private val validador = ValidadorDeNumeros(Cenario.entrada)

    private fun violacoes(texto: String) = validador.validar(Cenario.saida(veredicto = texto))

    @Test
    fun `numeros da entrada passam com arredondamento e formato brasileiro`() {
        Cenario.saida().let(validador::validar).shouldBeEmpty()
        violacoes("Patrimônio de R$ 50.000,00, ou R$ 50 mil; BOVA11 com 14,5% no ano e 152,35% do CDI.").shouldBeEmpty()
        violacoes("Posição de 03/09/2026, 2 ativos, vencimento em 12 meses, regra de 40%.").shouldBeEmpty()
    }

    @Test
    fun `tickers e rotulos colados em letras nao sao numeros`() {
        violacoes("HGLG11, IMAB11 e o retorno de 24M do BOVA11.").shouldBeEmpty()
    }

    @Test
    fun `regra zero - numero que nao existe na entrada e rejeitado`() {
        val v = violacoes("A carteira rendeu 37,5% e tem R$ 12.345 parados.")
        v.map { (it as Violacao.NumeroSemOrigem).numero } shouldContainExactly listOf("37,5", "12.345")
        (v.first() as Violacao.NumeroSemOrigem).trecho.contains("37,5%") shouldBe true
    }

    @Test
    fun `soma calculada pelo modelo e rejeitada - quem soma e o codigo`() {
        violacoes("Somando tudo, R$ 60.000.").single().shouldBeInstanceOf<Violacao.NumeroSemOrigem>()
    }

    @Test
    fun `sinal pode ser omitido e mil exige arredondamento compativel`() {
        violacoes("Delta de 13,7 pp; delta negativo seria -13,7.").shouldBeEmpty()
        violacoes("Cerca de R$ 41 mil no exterior.").single().shouldBeInstanceOf<Violacao.NumeroSemOrigem>()
    }

    @Test
    fun `sugestao de compra precisa de lacuna calculada e valor valido`() {
        val entradaLacunas = Cenario.entrada.lacunas.map { it.id }
        entradaLacunas shouldContainExactly listOf("PROTECAO_INFLACAO", "SEM_PREFIXADO")
        val ok = NovoAtivo("IMAB11", TipoLacuna.PROTECAO_INFLACAO, Prioridade.ALTA, "5000.00", "Proteção contra inflação.")
        validador.validar(Cenario.saida(novos = listOf(ok))).shouldBeEmpty()

        val semLacuna = ok.copy(lacuna = TipoLacuna.SEM_LIQUIDEZ_COM_SAQUES)
        validador.validar(Cenario.saida(novos = listOf(semLacuna))) shouldContainExactly
            listOf(Violacao.LacunaInexistente(TipoLacuna.SEM_LIQUIDEZ_COM_SAQUES))

        validador.validar(Cenario.saida(novos = listOf(ok.copy(valor = "R$ 5 mil")))) shouldContainExactly
            listOf(Violacao.ValorInvalido("novosAtivos.valor", "R$ 5 mil"))
        validador.validar(Cenario.saida(novos = listOf(ok.copy(valor = "50000.01")))) shouldContainExactly
            listOf(Violacao.ValorInvalido("novosAtivos.valor", "50000.01"))
    }

    @Test
    fun `venda precisa ser de uma posicao real, com destino e ate o saldo`() {
        val ok = Realocacao("Trend Global FIA", "Concentração.", "IMAB11", "20000.00")
        validador.validar(Cenario.saida(realocacoes = listOf(ok))).shouldBeEmpty()
        validador.validar(Cenario.saida(realocacoes = listOf(ok.copy(destino = " ")))) shouldContainExactly
            listOf(Violacao.VendaInvalida("Trend Global FIA"))
        validador.validar(Cenario.saida(realocacoes = listOf(ok.copy(vender = "XPTO11")))) shouldContainExactly
            listOf(Violacao.VendaInvalida("XPTO11"))
        validador.validar(Cenario.saida(realocacoes = listOf(ok.copy(valor = "40000.01")))) shouldContainExactly
            listOf(Violacao.ValorInvalido("realocacoes.valor", "40000.01"))
    }

    @Test
    fun `entrada nao carrega dados identificaveis e declara o que falta`() {
        val texto = kotlinx.serialization.json.Json.encodeToString(EntradaAnalise.serializer(), Cenario.entrada)
        listOf("conta", "assessor", "arquivo").forEach { campo -> texto.contains("\"$campo") shouldBe false }
        Cenario.entrada.dadosAusentes.any { it.startsWith("Preço médio") } shouldBe true
        Cenario.entrada.dadosAusentes.any { it.startsWith("Índices de referência") } shouldBe true
    }
}
