package com.investimentoeasy.feature.analysis

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.investimentoeasy.core.designsystem.CastanhaTema
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Fica fora do [AnaliseScreenTest] de propósito: com qualificadores de tela (`w390dp…`) o diálogo com campo de
 * texto nunca fica ocioso no Robolectric; na tela padrão dele funciona.
 */
@RunWith(RobolectricTestRunner::class)
class DialogoChaveTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun configurar_chave_abre_o_dialogo() {
        compose.setContent {
            CastanhaTema(escuro = false) { AnaliseScreen(Cenarios.estado(), {}, {}, {}, {}) }
        }
        compose.onNodeWithText("Configurar chave da API").performScrollTo().performClick()
        compose.onNodeWithText("Chave (sk-ant-…)").assertExists()
    }

    @Test
    fun salvar_entrega_a_chave_digitada() {
        var salvou: String? = null
        compose.setContent { CastanhaTema(escuro = false) { DialogoChave(aoSalvar = { salvou = it }, aoCancelar = {}) } }
        compose.onNodeWithText("Chave (sk-ant-…)").performTextReplacement("sk-ant-teste")
        compose.onNodeWithText("Salvar").performClick()
        salvou shouldBe "sk-ant-teste"
    }

    @Test
    fun cancelar_nao_salva() {
        var salvou: String? = null
        var cancelou = false
        compose.setContent {
            CastanhaTema(escuro = false) { DialogoChave(aoSalvar = { salvou = it }, aoCancelar = { cancelou = true }) }
        }
        compose.onNodeWithText("Cancelar").performClick()
        cancelou shouldBe true
        salvou shouldBe null
    }
}
