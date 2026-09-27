package com.investimentoeasy.feature.upload

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.github.takahirom.roborazzi.captureRoboImage
import com.investimentoeasy.core.designsystem.CastanhaTema
import com.investimentoeasy.core.importacao.MotivoFalha
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w390dp-h2400dp-xhdpi")
class TelasPlanilhaTest {
    @get:Rule
    val compose = createComposeRule()

    private val complemento = Cenarios.complemento()
    private val conferencia = montarConferencia(complemento.base, complemento.casamento)

    @Test
    fun enviar_planilha_lida_leva_para_a_conferencia() {
        var conferiu = 0
        val estado =
            EstadoImportacao(leitura = Leitura.PlanilhaLida("PosicaoDetalhada.xlsx", 30_000), complemento = complemento)
        compose.setContent {
            CastanhaTema(escuro = false) { EnviarCarteiraScreen(estado, {}, {}, {}, aoConferirPlanilha = { conferiu++ }) }
        }
        compose.onNodeWithText("Formato identificado: Posição Detalhada (XP)").assertExists()
        compose.onNodeWithText("11 posições casadas com a base").assertExists()
        compose.onRoot().captureRoboImage("src/test/screenshots/enviar_planilha.png")
        compose.onNodeWithText("Conferir planilha").performScrollTo().performClick()
        conferiu shouldBe 1
    }

    @Test
    fun enviar_planilha_sem_base_e_planilha_desconhecida() {
        compose.setContent {
            CastanhaTema(escuro = false) { EnviarCarteiraScreen(EstadoImportacao(leitura = Leitura.PlanilhaSemBase), {}, {}, {}) }
        }
        compose.onNodeWithText("! ENVIE O PDF PRIMEIRO").assertExists()
        compose.onNodeWithText("Revisar extração").assertIsNotEnabled()
    }

    @Test
    fun enviar_planilha_nao_reconhecida_explica() {
        compose.setContent {
            CastanhaTema(escuro = false) {
                EnviarCarteiraScreen(EstadoImportacao(leitura = Leitura.Falhou(MotivoFalha.PLANILHA_NAO_RECONHECIDA)), {}, {}, {})
            }
        }
        compose.onNodeWithText(mensagemDe(MotivoFalha.PLANILHA_NAO_RECONHECIDA)).assertExists()
    }

    @Test
    fun conferir_mostra_casadas_e_o_que_ficou_de_fora() {
        var guardou = 0
        compose.setContent {
            CastanhaTema(
                escuro = false,
            ) { ConferirPlanilhaScreen(conferencia, guardando = false, aoGuardar = { guardou++ }, aoVoltar = {}) }
        }
        compose.onNodeWithText("Casadas com a base (11)").assertExists()
        compose.onNodeWithText("Na planilha: Trend Nasdaq 100 FIM RL").assertExists()
        compose.onNodeWithText("Só na planilha (1)").assertExists()
        compose.onNodeWithText("! PLANILHA MAIS ANTIGA QUE A BASE").assertExists()
        compose.onRoot().captureRoboImage("src/test/screenshots/conferir_planilha.png")
        compose.onNodeWithText("Guardar dados da planilha").performScrollTo().assertIsEnabled().performClick()
        guardou shouldBe 1
    }

    @Test
    @Config(qualifiers = "w390dp-h2400dp-night-xhdpi")
    fun conferir_escuro() {
        compose.setContent { CastanhaTema(escuro = true) { ConferirPlanilhaScreen(conferencia, false, {}, {}) } }
        compose.onRoot().captureRoboImage("src/test/screenshots/conferir_planilha_escuro.png")
    }

    @Test
    fun conferir_sem_casadas_nao_guarda() {
        val vazia = conferencia.copy(casadas = emptyList())
        compose.setContent { CastanhaTema(escuro = false) { ConferirPlanilhaScreen(vazia, false, {}, {}) } }
        compose.onNodeWithText("● NADA CASOU").assertExists()
        compose.onNodeWithText("Guardar dados da planilha").performScrollTo().assertIsNotEnabled()
    }
}
