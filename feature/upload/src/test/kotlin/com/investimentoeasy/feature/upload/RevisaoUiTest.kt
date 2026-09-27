package com.investimentoeasy.feature.upload

import com.investimentoeasy.core.designsystem.componentes.Tom
import com.investimentoeasy.core.domain.alocacao.GrupoAlocacao
import com.investimentoeasy.core.model.Money
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.Test
import java.time.LocalDate

class RevisaoUiTest {
    @Test
    fun `relatorio consistente pode ser confirmado e mostra os alertas R6 antes das pendencias`() {
        val ui = montarRevisaoUi(Cenarios.revisao(), aceitouDivergencia = false)
        ui.dataReferencia shouldBe LocalDate.of(2026, 9, 3)
        ui.patrimonioInformado shouldBe Money.of("150250.00")
        ui.totalPosicoes shouldBe 13
        ui.podeConfirmar shouldBe true
        ui.exigeAceite shouldBe false
        ui.avisos.map { it.tom to it.rotulo } shouldContainExactly
            listOf(
                Tom.ATENCAO to "▲ DADO FORA DO PLAUSÍVEL",
                Tom.ATENCAO to "▲ POSIÇÃO SEM SALDO",
                Tom.INFORMATIVO to "ⓘ CLASSIFICAÇÃO A CONFIRMAR",
                Tom.INFORMATIVO to "ⓘ CLASSIFICAÇÃO A CONFIRMAR",
            )
        ui.avisos[0].texto shouldContain "LFTB11 aparece com +34,36% no mês"
        ui.avisos[1].texto shouldContain "RECR12"
        ui.avisos[3].texto shouldContain "XYZW11: segmento do FII"
    }

    @Test
    fun `alocacao por grupo soma a carteira`() {
        val ui = montarRevisaoUi(Cenarios.revisao(), aceitouDivergencia = false)
        ui.alocacao.map { it.grupo to it.valor } shouldContainExactly
            listOf(
                GrupoAlocacao.RENDA_FIXA_POS to Money.of("20000.00"),
                GrupoAlocacao.RENDA_FIXA_IPCA to Money.of("5000.00"),
                GrupoAlocacao.MULTIMERCADO to Money.of("20000.00"),
                GrupoAlocacao.FIIS to Money.of("40000.00"),
                GrupoAlocacao.ACOES_BRASIL to Money.of("15000.00"),
                GrupoAlocacao.EXTERIOR to Money.of("50000.00"),
            )
    }

    @Test
    fun `R3 - divergencia bloqueia ate o aceite e vem primeiro`() {
        val revisao = Cenarios.revisao(Cenarios.paginasComDivergencia())
        val semAceite = montarRevisaoUi(revisao, aceitouDivergencia = false)
        semAceite.podeConfirmar shouldBe false
        semAceite.exigeAceite shouldBe true
        semAceite.avisos.first().rotulo shouldBe "● SOMA NÃO CONFERE"
        semAceite.avisos.first().acao shouldBe AcaoAviso.ACEITAR_DIVERGENCIA
        semAceite.avisos.first().texto shouldContain "R$ 150.000,00"
        montarRevisaoUi(revisao, aceitouDivergencia = true).podeConfirmar shouldBe true
    }

    @Test
    fun `R1 - sem data nao pode confirmar e oferece informar`() {
        val ui = montarRevisaoUi(Cenarios.revisao(Cenarios.paginasSemData()), aceitouDivergencia = true)
        ui.podeConfirmar shouldBe false
        ui.avisos.first().acao shouldBe AcaoAviso.INFORMAR_DATA
    }
}
