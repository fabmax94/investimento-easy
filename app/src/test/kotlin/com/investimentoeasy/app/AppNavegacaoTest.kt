package com.investimentoeasy.app

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import com.investimentoeasy.feature.upload.Conclusao
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** O app de verdade (Hilt, Room, navegação) rodando na JVM. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, qualifiers = "w390dp-h844dp-xhdpi")
class AppNavegacaoTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    @Before
    fun injetar() = hilt.inject()

    /** A carteira vem do Room num dispatcher de IO de verdade: espera a leitura terminar. */
    private fun esperarCarteiraVazia() =
        compose.waitUntil(TEMPO_MAXIMO_MS) { compose.onAllNodesWithText("Nenhuma carteira ainda").fetchSemanticsNodes().isNotEmpty() }

    @Test
    fun abre_na_carteira_vazia_com_a_barra_inferior() {
        esperarCarteiraVazia()
        compose.onNodeWithTag("aba_CARTEIRA").assertIsSelected()
        compose.onRoot().captureRoboImage("src/test/screenshots/app_inicio.png")
    }

    @Test
    fun enviar_abre_o_fluxo_em_tela_cheia_e_volta() {
        compose.onAllNodesWithText("Enviar carteira")[0].performClick()
        compose.onNodeWithText("Escolher arquivo").assertExists()
        // Dentro da importação não há barra inferior.
        compose.onAllNodesWithTag("aba_ANALISE").fetchSemanticsNodes().size shouldBe 0
        compose.onNodeWithText("‹ Carteira").performClick()
        compose.onNodeWithText("Nenhuma carteira ainda").assertExists()
    }

    @Test
    fun abas_de_analise_e_alertas() {
        compose.onNodeWithTag("aba_ANALISE").performClick()
        compose.onNodeWithText("Sem carteira para analisar").assertExists()
        compose.onNodeWithTag("aba_ALERTAS").performClick()
        compose.onNodeWithTag("aba_ALERTAS").assertIsSelected()
        compose.onNodeWithTag("aba_CARTEIRA").performClick()
        compose.onNodeWithText("Nenhuma carteira ainda").assertExists()
    }

    @Test
    fun mensagens_de_conclusao() {
        mensagemDe(Conclusao.NOVA_BASE) shouldBe "Carteira atualizada: este upload é a nova base."
        mensagemDe(Conclusao.HISTORICO) shouldBe "Upload guardado no histórico: a data é anterior à base atual."
        mensagemDe(Conclusao.MANTIDA_BASE_ATUAL) shouldBe "Nada mudou: a base atual foi mantida."
    }
}

private const val TEMPO_MAXIMO_MS = 5_000L
