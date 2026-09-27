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
import com.investimentoeasy.core.domain.snapshot.DecisaoMesmaData
import com.investimentoeasy.core.importacao.MotivoFalha
import com.investimentoeasy.parser.xperformance.FixturesXPerformance
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w390dp-h1800dp-xhdpi")
class TelasImportacaoTest {
    @get:Rule
    val compose = createComposeRule()

    private val lida = Leitura.Lida("XPerformance_set2026.pdf", 1_258_291, FixturesXPerformance.PAGINAS)

    private fun estadoLido(paginas: List<String> = FixturesXPerformance.sintetico()) =
        EstadoImportacao(leitura = lida, revisao = Cenarios.revisao(paginas))

    @Test
    fun enviar_antes_do_arquivo_nao_deixa_revisar() {
        compose.setContent { CastanhaTema(escuro = false) { EnviarCarteiraScreen(EstadoImportacao(), {}, {}, {}) } }
        compose.onNodeWithText("Revisar extração").assertIsNotEnabled()
    }

    @Test
    fun enviar_mostra_as_etapas_da_leitura() {
        compose.setContent { CastanhaTema(escuro = false) { EnviarCarteiraScreen(estadoLido(), {}, {}, {}) } }
        compose.onNodeWithText("13 posições extraídas").assertExists()
        compose.onNodeWithText("Soma confere com o patrimônio").assertExists()
        compose.onNodeWithText("Revisar extração").assertIsEnabled()
        compose.onRoot().captureRoboImage("src/test/screenshots/enviar_lido.png")
    }

    @Test
    @Config(qualifiers = "w390dp-h1800dp-night-xhdpi")
    fun enviar_escuro() {
        compose.setContent { CastanhaTema(escuro = true) { EnviarCarteiraScreen(estadoLido(), {}, {}, {}) } }
        compose.onRoot().captureRoboImage("src/test/screenshots/enviar_lido_escuro.png")
    }

    @Test
    fun enviar_falha_explica_o_motivo() {
        compose.setContent {
            CastanhaTema(
                escuro = false,
            ) { EnviarCarteiraScreen(EstadoImportacao(leitura = Leitura.Falhou(MotivoFalha.PDF_PROTEGIDO)), {}, {}, {}) }
        }
        compose.onNodeWithText(mensagemDe(MotivoFalha.PDF_PROTEGIDO)).assertExists()
        compose.onRoot().captureRoboImage("src/test/screenshots/enviar_falha.png")
    }

    @Test
    fun revisar_mostra_resumo_alocacao_e_alertas() {
        var confirmou: DecisaoMesmaData? = DecisaoMesmaData.MANTER_ATUAL
        compose.setContent {
            CastanhaTema(escuro = false) {
                RevisarExtracaoScreen(
                    montarRevisaoUi(Cenarios.revisao(), false),
                    confirmando = false,
                    baseMesmaData = null,
                    aoInformarData = {},
                    aoAceitarDivergencia = {},
                    aoConfirmar = { confirmou = it },
                    aoCancelarDecisao = {},
                    aoVoltar = {},
                )
            }
        }
        compose.onNodeWithText("Posições por classe (13)").assertExists()
        compose.onNodeWithText("Precisa da sua atenção (4)").assertExists()
        compose.onRoot().captureRoboImage("src/test/screenshots/revisar.png")
        compose.onNodeWithText("Confirmar e atualizar").performScrollTo().assertIsEnabled().performClick()
        confirmou shouldBe null
    }

    @Test
    @Config(qualifiers = "w390dp-h1800dp-night-xhdpi")
    fun revisar_escuro() {
        compose.setContent {
            CastanhaTema(escuro = true) {
                RevisarExtracaoScreen(montarRevisaoUi(Cenarios.revisao(), false), false, null, {}, {}, {}, {}, {})
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/revisar_escuro.png")
    }

    @Test
    fun revisar_com_divergencia_so_libera_depois_do_aceite() {
        var aceito = false
        val revisao = Cenarios.revisao(Cenarios.paginasComDivergencia())
        compose.setContent {
            CastanhaTema(escuro = false) {
                RevisarExtracaoScreen(montarRevisaoUi(revisao, aceito), false, null, {}, { aceito = it }, {}, {}, {})
            }
        }
        compose.onNodeWithText("Confirmar e atualizar").assertIsNotEnabled()
        compose.onRoot().captureRoboImage("src/test/screenshots/revisar_divergencia.png")
        compose.onNodeWithText("Aceito a divergência e quero confirmar assim").performClick()
        aceito shouldBe true
    }

    @Test
    fun revisar_mesma_data_pergunta_se_substitui() {
        var decisao: DecisaoMesmaData? = null
        compose.setContent {
            CastanhaTema(escuro = false) {
                RevisarExtracaoScreen(
                    montarRevisaoUi(Cenarios.revisao(), false),
                    confirmando = false,
                    baseMesmaData = java.time.LocalDate.of(2026, 9, 3),
                    aoInformarData = {},
                    aoAceitarDivergencia = {},
                    aoConfirmar = { decisao = it },
                    aoCancelarDecisao = {},
                    aoVoltar = {},
                )
            }
        }
        compose.onNodeWithText("Já existe uma base de 03/09/2026").assertExists()
        compose.onNodeWithText("Substituir").performClick()
        decisao shouldBe DecisaoMesmaData.SUBSTITUIR
    }
}
