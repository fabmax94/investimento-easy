package com.investimentoeasy.core.domain.validacao

import com.investimentoeasy.core.model.AvisoExtracao
import com.investimentoeasy.core.model.ChaveAtivo
import com.investimentoeasy.core.model.ClasseAtivo
import com.investimentoeasy.core.model.CodigoAviso
import com.investimentoeasy.core.model.Money
import com.investimentoeasy.core.model.Percent
import com.investimentoeasy.core.model.TipoAtivo
import com.investimentoeasy.core.testing.ativo
import com.investimentoeasy.core.testing.posicao
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class ValidadorExtracaoTest {
    private val validador = ValidadorExtracao()
    private val dezMil = listOf(posicao(saldo = "10000.00"))

    private fun validar(
        patrimonio: String?,
        posicoes: List<com.investimentoeasy.core.model.Posicao> = dezMil,
        temData: Boolean = true,
    ) = validador.validar(posicoes, patrimonio?.let(Money::of), temData)

    @Test
    fun `carteira consistente nao tem problemas`() {
        val resultado = validar("10000.00")
        resultado.problemas.shouldBeEmpty()
        resultado.gravidadeMaxima.shouldBeNull()
        resultado.conferenciaSoma.shouldNotBeNull().dentroDaTolerancia shouldBe true
    }

    @Test
    fun `R1 - sem data de referencia e impeditivo`() {
        val resultado = validar("10000.00", temData = false)
        resultado.problemas shouldContainExactly listOf(Problema.DataReferenciaAusente)
        resultado.impeditivo shouldBe true
    }

    @Test
    fun `sem posicoes e impeditivo`() {
        validar("0.00", posicoes = emptyList()).problemas shouldContainExactly listOf(Problema.SemPosicoes)
    }

    @Test
    fun `R3 - diferenca de 0,49 por cento passa`() {
        // soma 10.000,00 e patrimônio 10.049,00: diferença de 49,00 = 0,4876%
        val resultado = validar("10049.00")
        resultado.problemas.shouldBeEmpty()
        resultado.conferenciaSoma!!.diferenca shouldBe Money.of("49.00")
    }

    @Test
    fun `R3 - diferenca de exatamente 0,5 por cento passa`() {
        // patrimônio 10.000,00 e soma 9.950,00
        validar("10000.00", posicoes = listOf(posicao(saldo = "9950.00"))).problemas.shouldBeEmpty()
    }

    @Test
    fun `R3 - diferenca acima de 0,5 por cento exige aceite`() {
        val resultado = validar("10000.00", posicoes = listOf(posicao(saldo = "9949.99")))
        val divergencia = resultado.problemas.single().shouldBeInstanceOf<Problema.DivergenciaSoma>()
        divergencia.conferencia.percentual shouldBe Percent.of("0.5001")
        divergencia.conferencia.diferenca shouldBe Money.of("50.01")
        resultado.exigeAceite shouldBe true
        resultado.impeditivo shouldBe false
    }

    @Test
    fun `R3 - soma acima do patrimonio tambem diverge`() {
        validar("10000.00", posicoes = listOf(posicao(saldo = "10100.00"))).exigeAceite shouldBe true
    }

    @Test
    fun `R3 - propriedade - dentro da tolerancia sse diferenca menor ou igual a 0,5 por cento`() =
        runTest {
            checkAll(Arb.long(1_00L..10_000_000_00L), Arb.long(0L..20_000_000_00L)) { patrimonioCent, somaCent ->
                val patrimonio = Money.of(BigDecimal.valueOf(patrimonioCent, 2))
                val soma = BigDecimal.valueOf(somaCent, 2)
                val conferencia = validador.conferirSoma(listOf(posicao(saldo = soma.toPlainString())), patrimonio)
                val esperado = (patrimonio.valor - soma).abs() * BigDecimal(200) <= patrimonio.valor
                conferencia.dentroDaTolerancia shouldBe esperado
            }
        }

    @Test
    fun `R3 - sem patrimonio informado exige aceite`() {
        val resultado = validar(null)
        resultado.problemas shouldContainExactly listOf(Problema.PatrimonioAusente)
        resultado.conferenciaSoma.shouldBeNull()
    }

    @Test
    fun `R3 - patrimonio zero so confere com soma zero`() {
        validar("0.00", posicoes = listOf(posicao(saldo = "0.00", quantidade = null))).exigeAceite shouldBe false
        validar("0.00").exigeAceite shouldBe true
    }

    @Test
    fun `R6 - posicao fantasma e posicao vazia viram alerta e saem do ritmo`() {
        val fantasma = posicao(ativo = ativo("RECR12", ClasseAtivo.FII_PAPEL, TipoAtivo.FII), saldo = "0.00", quantidade = "9")
        val vazia = posicao(ativo = ativo("ABCD11", ClasseAtivo.FII_PAPEL, TipoAtivo.FII), saldo = "0.00", quantidade = "0")
        val resultado = validar("10000.00", posicoes = dezMil + fantasma + vazia)
        resultado.problemas shouldContainExactly
            listOf(
                Problema.Inconsistencia(ChaveAtivo.Ticker("RECR12"), "RECR12", TipoInconsistencia.POSICAO_FANTASMA),
                Problema.Inconsistencia(ChaveAtivo.Ticker("ABCD11"), "ABCD11", TipoInconsistencia.POSICAO_VAZIA),
            )
        resultado.gravidadeMaxima shouldBe Gravidade.ALERTA
        resultado.chavesForaDoRitmo shouldBe setOf(ChaveAtivo.Ticker("RECR12"), ChaveAtivo.Ticker("ABCD11"))
    }

    @Test
    fun `R6 - rentabilidade mensal acima de 30 por cento em renda fixa e implausivel`() {
        val lftb = posicao(ativo = ativo("LFTB11", ClasseAtivo.RF_POS), saldo = "0.01", rentabilidadeMes = "34.36")
        val acao = posicao(ativo = ativo("MGLU3", ClasseAtivo.ACAO, TipoAtivo.ACAO), saldo = "0.01", rentabilidadeMes = "45.00")
        val limite = posicao(ativo = ativo("IMAB11", ClasseAtivo.RF_IPCA), saldo = "0.01", rentabilidadeMes = "-30.00")
        val resultado = validar("10000.03", posicoes = dezMil + lftb + acao + limite)
        resultado.problemas shouldContainExactly
            listOf(Problema.Inconsistencia(ChaveAtivo.Ticker("LFTB11"), "LFTB11", TipoInconsistencia.RENTABILIDADE_IMPLAUSIVEL))
    }

    @Test
    fun `limites sao configuraveis`() {
        val rigido = ValidadorExtracao(LimitesValidacao(toleranciaSoma = Percent.of("0.1")))
        rigido.validar(dezMil, Money.of("10049.00"), true).exigeAceite shouldBe true
    }

    @Test
    fun `avisos de leitura aparecem como alerta`() {
        val aviso = AvisoExtracao(CodigoAviso.LINHA_NAO_INTERPRETADA, "x", 3)
        val resultado = validador.validar(dezMil, Money.of("10000.00"), true, listOf(aviso))
        resultado.problemas shouldContainExactly listOf(Problema.AvisoDeLeitura(aviso))
        resultado.gravidadeMaxima shouldBe Gravidade.ALERTA
    }
}
