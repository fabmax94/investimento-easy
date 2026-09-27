package com.investimentoeasy.feature.analysis

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.github.takahirom.roborazzi.captureRoboImage
import com.investimentoeasy.core.designsystem.CastanhaTema
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w390dp-h2200dp-xhdpi")
class AnaliseScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private fun tela(
        estado: EstadoAnalise,
        escuro: Boolean = false,
        aoSelecionar: (AbaAnalise) -> Unit = {},
        aoGerar: () -> Unit = {},
        aoSalvar: (String) -> Unit = {},
    ) = compose.setContent {
        CastanhaTema(escuro = escuro) { AnaliseScreen(estado, aoSelecionar, aoGerar, aoSalvar, aoApagarChave = {}) }
    }

    private fun capturar(nome: String) = compose.onRoot().captureRoboImage("src/test/screenshots/$nome.png")

    @Test
    fun o_que_fazer_sem_analise_do_claude_pede_a_chave() {
        tela(Cenarios.estado())
        compose.onNodeWithText("O que fazer").assertIsSelected()
        compose.onNodeWithText("Principais alertas").assertExists()
        capturar("analise_sem_claude")
    }

    @Test
    fun o_que_fazer_com_chave_gera() {
        var gerou = 0
        tela(Cenarios.estado(temChave = true), aoGerar = { gerou++ })
        compose.onNodeWithText("Gerar análise com o Claude").performClick()
        gerou shouldBe 1
    }

    @Test
    fun o_que_fazer_com_analise() {
        tela(Cenarios.estado(ia = true, temChave = true))
        compose.onNodeWithText("Próximos 30 dias").assertExists()
        compose.onNodeWithText("Trend Nasdaq 100 FIA → IMAB11").assertExists()
        capturar("analise_o_que_fazer")
    }

    @Test
    @Config(qualifiers = "w390dp-h2200dp-night-xhdpi")
    fun o_que_fazer_escuro() {
        tela(Cenarios.estado(ia = true, temChave = true), escuro = true)
        capturar("analise_o_que_fazer_escuro")
    }

    @Test
    fun trocar_de_aba() {
        var aba: AbaAnalise? = null
        tela(Cenarios.estado(), aoSelecionar = { aba = it })
        compose.onNodeWithText("FIIs").performScrollTo().performClick()
        aba shouldBe AbaAnalise.FIIS
    }

    @Test
    fun abas_com_analise() {
        var aba by mutableStateOf(AbaAnalise.MERCADO)
        compose.setContent {
            CastanhaTema(escuro = false) { AnaliseScreen(Cenarios.estado(ia = true, aba = aba, temChave = true), {}, {}, {}, {}) }
        }
        AbaAnalise.entries.drop(1).forEach {
            aba = it
            compose.waitForIdle()
            capturar("analise_${it.name.lowercase()}")
        }
    }

    @Test
    fun alertas_mostram_severidade_e_comentario() {
        tela(Cenarios.estado(ia = true, aba = AbaAnalise.ALERTAS))
        compose.onNodeWithText("● URGENTE").assertExists()
        compose.onNodeWithText("Dois ativos concentram a exposição global.").assertExists()
    }

    @Test
    fun fiis_e_acoes_com_planilha_mostram_custo_e_resultado() {
        var aba by mutableStateOf(AbaAnalise.FIIS)
        compose.setContent {
            CastanhaTema(escuro = false) {
                AnaliseScreen(Cenarios.estado(aba = aba, comPlanilha = true), {}, {}, {}, {})
            }
        }
        compose.onNodeWithText("PM R$ 95,00", substring = true).assertExists()
        compose.onNodeWithText("ⓘ PREÇO MÉDIO E RESULTADO").assertDoesNotExist()
        capturar("analise_fiis_com_planilha")
        aba = AbaAnalise.ACOES_ETFS
        compose.waitForIdle()
        compose.onNodeWithText("PM R$ 151,23", substring = true).assertExists()
    }

    @Test
    fun sem_planilha_explica_de_onde_vem_o_preco_medio() {
        tela(Cenarios.estado(aba = AbaAnalise.FIIS))
        compose.onNodeWithText("ⓘ PREÇO MÉDIO E RESULTADO").assertExists()
    }
}
