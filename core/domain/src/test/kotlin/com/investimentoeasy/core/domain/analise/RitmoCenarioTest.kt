package com.investimentoeasy.core.domain.analise

import com.investimentoeasy.core.model.ClasseAtivo
import com.investimentoeasy.core.model.Percent
import com.investimentoeasy.core.model.TipoAtivo
import com.investimentoeasy.core.testing.ativo
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDate

class RitmoCenarioTest {
    private val setembro = LocalDate.of(2026, 9, 3)

    @Test
    fun `meses decorridos no ano como na skill - 03 de setembro e 8,1 meses`() {
        mesesDecorridosNoAno(setembro) shouldBe (8.09 plusOrMinus 0.01)
        mesesDecorridosNoAno(LocalDate.of(2026, 12, 31)) shouldBe (12.0 plusOrMinus 1e-9)
    }

    @Test
    fun `ritmo acelerando com o delta anualizado`() {
        // ano 14,47% em 8,09 meses = 22,2% a.a.; 24M 17,69% = 8,5% a.a.; delta +13,7 pp
        val r = ritmo(Percent.of("14.47"), Percent.of("17.69"), setembro)!!
        r.ritmo shouldBe Ritmo.ACELERANDO
        r.deltaAoAno shouldBe Percent.of("13.7")
    }

    @Test
    fun `ritmo desacelerando`() {
        // ano -0,36% (≈ -0,5% a.a.) vs 24M 7,85% (≈ 3,85% a.a.)
        val r = ritmo(Percent.of("-0.36"), Percent.of("7.85"), setembro)!!
        r.ritmo shouldBe Ritmo.DESACELERANDO
        r.deltaAoAno shouldBe Percent.of("-4.4")
    }

    @Test
    fun `ritmo estavel dentro de 3 pp`() {
        ritmo(Percent.of("6.5"), Percent.of("21.0"), setembro)!!.ritmo shouldBe Ritmo.ESTAVEL
    }

    @Test
    fun `sem base quando ano e 24M sao iguais ou o ano mal comecou`() {
        ritmo(Percent.of("4.95"), Percent.of("4.95"), setembro) shouldBe RitmoAtivo(Ritmo.SEM_BASE, null)
        ritmo(Percent.of("1"), Percent.of("20"), LocalDate.of(2026, 1, 20)) shouldBe RitmoAtivo(Ritmo.SEM_BASE, null)
    }

    @Test
    fun `regra zero - sem retorno nao ha ritmo`() {
        ritmo(null, Percent.of("10"), setembro).shouldBeNull()
        ritmo(Percent.of("10"), null, setembro).shouldBeNull()
    }

    @ParameterizedTest(name = "{0} ({1}) -> {2}")
    @CsvSource(
        "LFTB11, RF_POS, ADVERSO",
        "IMAB11, RF_IPCA, FAVORAVEL",
        "HGLG11, FII_TIJOLO, FAVORAVEL",
        "TEPP11, FII_TIJOLO, NEUTRO",
        "TVRI11, FII_TIJOLO, NEUTRO",
        "KNCR11, FII_PAPEL, INDEFINIDO",
        "BOVA11, ETF, FAVORAVEL",
        "ITSA4, ACAO, FAVORAVEL",
        "IVVB11, RV_GLOBAL, ADVERSO",
        "KFOF11, FII_FOF, INDEFINIDO",
        "GOLD11, ETF, NEUTRO",
    )
    fun `tabela de cenarios da skill`(
        ticker: String,
        classe: ClasseAtivo,
        esperado: Cenario,
    ) {
        cenario(ativo(ticker, classe)).cenario shouldBe esperado
    }

    @Test
    fun `fundo de ouro e neutro mesmo classificado como multimercado`() {
        val ouro = ativo("X", ClasseAtivo.FUNDO_MULTIMERCADO, TipoAtivo.FUNDO).copy(nome = "Trend Ouro FIF Multi RL")
        cenario(ouro) shouldBe CenarioAtivo(Cenario.NEUTRO, "hedge, não segue o ciclo local")
    }
}
