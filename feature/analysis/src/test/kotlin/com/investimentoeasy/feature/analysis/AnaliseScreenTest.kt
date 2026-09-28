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
import com.investimentoeasy.core.domain.mercado.Perfil
import com.investimentoeasy.core.model.FalhaDeFonte
import com.investimentoeasy.core.model.FonteMercado
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w390dp-h2600dp-xhdpi")
class AnaliseScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private fun tela(
        estado: EstadoAnalise,
        escuro: Boolean = false,
        acoes: AcoesAnalise = AcoesAnalise(),
    ) = compose.setContent { CastanhaTema(escuro = escuro) { AnaliseScreen(estado, acoes) } }

    private fun capturar(nome: String) = compose.onRoot().captureRoboImage("src/test/screenshots/$nome.png")

    @Test
    fun sem_perfil_pede_o_perfil() {
        var escolhido: Perfil? = null
        tela(Cenarios.estado(perfil = null), acoes = AcoesAnalise(aoEscolherPerfil = { escolhido = it }))
        compose.onNodeWithText("O que fazer").assertIsSelected()
        compose.onNodeWithText("QUAL É O SEU PERFIL?").assertExists()
        compose.onNodeWithText("Principais alertas").assertExists()
        capturar("analise_escolher_perfil")
        compose.onNodeWithText("Moderado").performClick()
        escolhido shouldBe Perfil.MODERADO
    }

    @Test
    fun o_que_fazer_com_perfil_e_mercado() {
        var trocou = 0
        tela(Cenarios.estado(), acoes = AcoesAnalise(aoTrocarPerfil = { trocou++ }))
        compose.onNodeWithText("VEREDICTO").assertExists()
        compose.onNodeWithText("Próximos 30 dias").assertExists()
        compose.onNodeWithText("Gerado no aparelho · Perfil moderado").assertExists()
        compose.onNodeWithText("Mercado de ", substring = true).assertExists()
        capturar("analise_o_que_fazer")
        compose.onNodeWithText("Trocar perfil").performClick()
        trocou shouldBe 1
    }

    @Test
    @Config(qualifiers = "w390dp-h2600dp-night-xhdpi")
    fun o_que_fazer_escuro() {
        tela(Cenarios.estado(), escuro = true)
        capturar("analise_o_que_fazer_escuro")
    }

    @Test
    fun sem_mercado_avisa_e_deixa_atualizar() {
        var atualizou = 0
        tela(Cenarios.estado(comMercado = false), acoes = AcoesAnalise(aoAtualizarMercado = { atualizou++ }))
        compose.onNodeWithText("Sem dados de mercado: a análise usa só o relatório.").assertExists()
        compose.onNodeWithText("Atualizar").performClick()
        atualizou shouldBe 1
    }

    @Test
    fun fonte_fora_do_ar_aparece_no_status() {
        tela(Cenarios.estado(falhas = listOf(FalhaDeFonte(FonteMercado.CVM, "HTTP 500"))))
        compose.onNodeWithText("! FONTE INDISPONÍVEL").assertExists()
        compose.onNodeWithText("Não responderam: CVM, informe mensal de FIIs.", substring = true).assertExists()
    }

    @Test
    fun trocar_de_aba() {
        var aba: AbaAnalise? = null
        tela(Cenarios.estado(), acoes = AcoesAnalise(aoSelecionarAba = { aba = it }))
        compose.onNodeWithText("FIIs").performScrollTo().performClick()
        aba shouldBe AbaAnalise.FIIS
    }

    @Test
    fun abas_com_recomendacao() {
        var aba by mutableStateOf(AbaAnalise.MERCADO)
        compose.setContent {
            CastanhaTema(escuro = false) { AnaliseScreen(Cenarios.estado(aba = aba, comPlanilha = true), AcoesAnalise()) }
        }
        AbaAnalise.entries.drop(1).forEach {
            aba = it
            compose.waitForIdle()
            capturar("analise_${it.name.lowercase()}")
        }
    }

    @Test
    fun mercado_mostra_indicadores_e_focus() {
        tela(Cenarios.estado(aba = AbaAnalise.MERCADO))
        compose.onNodeWithText("Indicadores de hoje").assertExists()
        compose.onNodeWithText("Boletim Focus (11/09/2026)").assertExists()
        compose.onNodeWithText("O mercado e a sua carteira").assertExists()
    }

    @Test
    fun fiis_mostram_p_vp_e_custo_da_planilha() {
        tela(Cenarios.estado(aba = AbaAnalise.FIIS, comPlanilha = true))
        compose.onNodeWithText("HGLG11: P/VP", substring = true).assertExists()
        compose.onNodeWithText("PM R$ 160,00", substring = true).assertExists()
        compose.onNodeWithText("ⓘ PREÇO MÉDIO E RESULTADO").assertDoesNotExist()
    }

    @Test
    fun sem_planilha_explica_de_onde_vem_o_preco_medio() {
        tela(Cenarios.estado(aba = AbaAnalise.FIIS))
        compose.onNodeWithText("ⓘ PREÇO MÉDIO E RESULTADO").assertExists()
    }

    @Test
    fun alertas_mostram_severidade_e_comentario() {
        tela(Cenarios.estado(aba = AbaAnalise.ALERTAS))
        compose.onNodeWithText("● URGENTE").assertExists()
        compose.onNodeWithText("em renda variável global, acima do limite de 30%", substring = true).assertExists()
    }

    @Test
    fun alocacao_contra_o_perfil() {
        tela(Cenarios.estado(aba = AbaAnalise.ALOCACAO))
        compose.onNodeWithText("Contra o perfil moderado").assertExists()
    }
}
