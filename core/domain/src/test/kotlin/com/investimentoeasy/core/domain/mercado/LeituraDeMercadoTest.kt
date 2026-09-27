package com.investimentoeasy.core.domain.mercado

import com.investimentoeasy.core.domain.alocacao.GrupoAlocacao
import com.investimentoeasy.core.domain.analise.Cenario
import com.investimentoeasy.core.domain.analise.cenario
import com.investimentoeasy.core.model.ClasseAtivo
import com.investimentoeasy.core.model.Cotacao
import com.investimentoeasy.core.model.ExpectativasFocus
import com.investimentoeasy.core.model.FonteMercado
import com.investimentoeasy.core.model.InformeFii
import com.investimentoeasy.core.model.PanoramaMercado
import com.investimentoeasy.core.model.Percent
import com.investimentoeasy.core.model.TipoAtivo
import com.investimentoeasy.core.model.ValorDeMercado
import com.investimentoeasy.core.testing.INSTANTE_FIXO
import com.investimentoeasy.core.testing.ativo
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class LeituraDeMercadoTest {
    private val hoje = LocalDate.of(2026, 9, 26)

    private fun panorama(
        selic: String? = "13.75",
        selicFimDeAno: Map<Int, String> = mapOf(2026 to "13.50", 2027 to "12.00"),
        dolar: String? = "5.20",
        cambio2026: String? = "5.20",
    ) = PanoramaMercado(
        obtidoEm = INSTANTE_FIXO,
        selicMeta = selic?.let { ValorDeMercado(BigDecimal(it), hoje, FonteMercado.BANCO_CENTRAL) },
        ipca12Meses = null,
        dolar = dolar?.let { ValorDeMercado(BigDecimal(it), hoje, FonteMercado.BANCO_CENTRAL) },
        focus =
            ExpectativasFocus(
                LocalDate.of(2026, 9, 18),
                selicFimDeAno.mapValues { BigDecimal(it.value) },
                emptyMap(),
                cambio2026?.let { mapOf(2026 to BigDecimal(it)) }.orEmpty(),
            ),
        ibovespa = null,
        ifix = null,
        cotacoes = emptyMap(),
        fiis = emptyMap(),
        falhas = emptyList(),
    )

    @Test
    fun `alvos de cada perfil somam 100 e respeitam a faixa`() {
        Perfil.entries.forEach { perfil ->
            val alvos = alvosDe(perfil)
            alvos.values.sumOf { it.alvo.pontos }.compareTo(BigDecimal(100)) shouldBe 0
            alvos.values.all { it.minimo <= it.alvo && it.alvo <= it.maximo } shouldBe true
            (GrupoAlocacao.A_CLASSIFICAR in alvos) shouldBe false
        }
        val acoes = { perfil: Perfil -> alvosDe(perfil).getValue(GrupoAlocacao.ACOES_BRASIL).alvo }
        (acoes(Perfil.ARROJADO) > acoes(Perfil.CONSERVADOR)) shouldBe true
    }

    @Test
    fun `ciclo de juros pela Selic de hoje contra o Focus`() {
        with(leituraDeJuros(panorama(), hoje)!!) {
            ciclo shouldBe CicloJuros.CORTE
            anoEsperado shouldBe 2027
            selicAtual shouldBe BigDecimal("13.75")
            selicEsperada shouldBe BigDecimal("12.00")
        }
        leituraDeJuros(panorama(selicFimDeAno = mapOf(2027 to "14.50")), hoje)!!.ciclo shouldBe CicloJuros.ALTA
        leituraDeJuros(panorama(selicFimDeAno = mapOf(2027 to "13.25")), hoje)!!.ciclo shouldBe CicloJuros.ESTAVEL
        // Até junho, o horizonte é o fim deste ano.
        leituraDeJuros(panorama(), LocalDate.of(2026, 3, 1))!!.anoEsperado shouldBe 2026
    }

    @Test
    fun `sem Selic do BC usa o Focus deste ano e sem Focus nao ha leitura`() {
        leituraDeJuros(panorama(selic = null), hoje)!!.selicAtual shouldBe BigDecimal("13.50")
        leituraDeJuros(panorama().copy(focus = null), hoje).shouldBeNull()
        leituraDeJuros(panorama(selicFimDeAno = mapOf(2026 to "13.50")), hoje).shouldBeNull()
    }

    @Test
    fun `cambio pelo dolar de hoje contra o Focus`() {
        leituraDeCambio(panorama(), hoje)!!.direcao shouldBe DirecaoCambio.ESTAVEL
        leituraDeCambio(panorama(cambio2026 = "5.60"), hoje)!!.direcao shouldBe DirecaoCambio.REAL_MAIS_FRACO
        leituraDeCambio(panorama(cambio2026 = "4.90"), hoje)!!.direcao shouldBe DirecaoCambio.REAL_MAIS_FORTE
        leituraDeCambio(panorama(dolar = null), hoje).shouldBeNull()
    }

    @Test
    fun `cenario muda com o ciclo e o global com o cambio`() {
        val pos = ativo("LFTB11", ClasseAtivo.RF_POS)
        val ipca = ativo("IMAB11", ClasseAtivo.RF_IPCA)
        val global = ativo("IVVB11", ClasseAtivo.RV_GLOBAL)
        val corte = leituraDeJuros(panorama(), hoje)
        val alta = leituraDeJuros(panorama(selicFimDeAno = mapOf(2027 to "14.50")), hoje)
        val estavel = leituraDeJuros(panorama(selicFimDeAno = mapOf(2027 to "13.50")), hoje)
        cenario(pos, corte).cenario shouldBe Cenario.ADVERSO
        cenario(pos, alta).cenario shouldBe Cenario.FAVORAVEL
        cenario(ipca, alta).cenario shouldBe Cenario.ADVERSO
        cenario(ipca, estavel).cenario shouldBe Cenario.NEUTRO
        cenario(pos).cenario shouldBe Cenario.ADVERSO
        cenario(global, cambio = leituraDeCambio(panorama(cambio2026 = "5.60"), hoje)).cenario shouldBe Cenario.FAVORAVEL
        cenario(global).cenario shouldBe Cenario.ADVERSO
        cenario(ativo("HGLG11", ClasseAtivo.FII_TIJOLO, TipoAtivo.FII), alta).cenario shouldBe Cenario.ADVERSO
    }

    private fun cotacao(
        preco: String,
        r12: String?,
        maxima: String? = null,
    ) = Cotacao("X", BigDecimal(preco), hoje, null, null, r12?.let(Percent::of), maxima?.let(::BigDecimal))

    @Test
    fun `FII com P-VP, distancia ao VP e DY corrente`() {
        val informe = InformeFii("KNCR", LocalDate.of(2026, 8, 1), BigDecimal("100.00"), Percent.of("12.00"), Percent.of("0.70"), 12)
        val fii = fiiNoMercado("KNCR11", cotacao("80.00", "-20"), informe, cotacao("13", "2"))!!
        fii.pvp shouldBe BigDecimal("0.80")
        fii.ateOValorPatrimonial shouldBe Percent.of("25.00")
        fii.dividendYieldCorrenteAnualizado shouldBe Percent.of("8.40")
        fii.distribuicaoEncolhendo shouldBe true
        fii.motivoQueda shouldBe MotivoQueda.ESPECIFICA
        fiiNoMercado("KNCR11", null, informe, null).shouldBeNull()
    }

    @Test
    fun `motivo da queda comparado ao IFIX`() {
        motivoQueda(Percent.of("5"), Percent.of("-3")) shouldBe MotivoQueda.SEM_QUEDA
        motivoQueda(Percent.of("-5"), Percent.of("-3")) shouldBe MotivoQueda.CICLICA
        motivoQueda(Percent.of("-15"), Percent.of("-3")) shouldBe MotivoQueda.ESPECIFICA
        motivoQueda(Percent.of("-15"), null) shouldBe MotivoQueda.SEM_DADO
        motivoQueda(null, Percent.of("1")) shouldBe MotivoQueda.SEM_DADO
    }

    @Test
    fun `distancia da maxima de 52 semanas`() {
        distanciaDaMaxima(cotacao("90", null, "120")) shouldBe Percent.of("-25.00")
        distanciaDaMaxima(cotacao("90", null)).shouldBeNull()
    }
}
