package com.investimentoeasy.feature.portfolio

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import com.investimentoeasy.core.designsystem.CastanhaTema
import com.investimentoeasy.core.domain.alocacao.FatiaAlocacao
import com.investimentoeasy.core.domain.alocacao.GrupoAlocacao
import com.investimentoeasy.core.model.Money
import com.investimentoeasy.core.model.Origem
import com.investimentoeasy.core.model.Percent
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w390dp-h1100dp-xhdpi")
class CarteiraScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val base =
        EstadoCarteira.ComBase(
            dataReferencia = LocalDate.of(2026, 9, 3),
            diasDesdeReferencia = 24,
            atualidade = Atualidade.EM_DIA,
            patrimonio = Money.of("150250.00"),
            origemPatrimonio = Origem.RELATORIO,
            versao = 1,
            divergenciaAceita = false,
            alocacao =
                listOf(
                    FatiaAlocacao(GrupoAlocacao.RENDA_FIXA_POS, 3, Money.of("20000.00"), Percent.of("13.33")),
                    FatiaAlocacao(GrupoAlocacao.FIIS, 4, Money.of("40000.00"), Percent.of("26.67")),
                    FatiaAlocacao(GrupoAlocacao.EXTERIOR, 2, Money.of("90000.00"), Percent.of("60")),
                ),
        )

    @Test
    fun com_base() {
        compose.setContent { CastanhaTema(escuro = false) { CarteiraScreen(base, aoEnviar = {}) } }
        compose.onNodeWithText("R$ 150.250").assertExists()
        compose.onNodeWithContentDescription("Origem: relatório").assertExists()
        compose.onRoot().captureRoboImage("src/test/screenshots/carteira.png")
    }

    @Test
    @Config(qualifiers = "w390dp-h1100dp-night-xhdpi")
    fun com_base_escuro_desatualizada() {
        compose.setContent {
            CastanhaTema(escuro = true) {
                CarteiraScreen(
                    base.copy(diasDesdeReferencia = 50, atualidade = Atualidade.DESATUALIZADA, divergenciaAceita = true),
                    aoEnviar = {},
                )
            }
        }
        compose.onNodeWithText("▲ CARTEIRA DESATUALIZADA").assertExists()
        compose.onRoot().captureRoboImage("src/test/screenshots/carteira_escuro_desatualizada.png")
    }

    @Test
    fun sem_carteira_leva_ao_envio() {
        var enviou = 0
        compose.setContent { CastanhaTema(escuro = false) { CarteiraScreen(EstadoCarteira.SemCarteira, aoEnviar = { enviou++ }) } }
        compose.onRoot().captureRoboImage("src/test/screenshots/carteira_vazia.png")
        compose.onNodeWithText("Enviar carteira").performClick()
        compose.onNodeWithContentDescription("Enviar nova carteira").performClick()
        enviou shouldBe 2
    }
}
