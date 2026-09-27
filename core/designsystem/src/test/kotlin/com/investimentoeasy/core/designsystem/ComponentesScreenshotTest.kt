package com.investimentoeasy.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.investimentoeasy.core.designsystem.componentes.BotaoContorno
import com.investimentoeasy.core.designsystem.componentes.BotaoPrimario
import com.investimentoeasy.core.designsystem.componentes.BotaoTexto
import com.investimentoeasy.core.designsystem.componentes.CabecalhoTela
import com.investimentoeasy.core.designsystem.componentes.CartaoAviso
import com.investimentoeasy.core.designsystem.componentes.CartaoContorno
import com.investimentoeasy.core.designsystem.componentes.CartaoSuave
import com.investimentoeasy.core.designsystem.componentes.ChipOrigem
import com.investimentoeasy.core.designsystem.componentes.Etiqueta
import com.investimentoeasy.core.designsystem.componentes.LinhaValor
import com.investimentoeasy.core.designsystem.componentes.TituloSecao
import com.investimentoeasy.core.designsystem.componentes.Tom
import com.investimentoeasy.core.model.Origem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Catálogo dos componentes, comparado com as imagens versionadas em `src/test/screenshots`. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ComponentesScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    @Config(qualifiers = "w390dp-h1200dp-xhdpi")
    fun catalogo_claro() = capturar(escuro = false, nome = "catalogo_claro")

    @Test
    @Config(qualifiers = "w390dp-h1200dp-night-xhdpi")
    fun catalogo_escuro() = capturar(escuro = true, nome = "catalogo_escuro")

    /** Acessibilidade: nada pode sumir ou sobrepor com a fonte em 200%. */
    @Test
    @Config(qualifiers = "w390dp-h2000dp-xhdpi", fontScale = 2.0f)
    fun catalogo_fonte_grande() = capturar(escuro = false, nome = "catalogo_fonte_grande")

    private fun capturar(
        escuro: Boolean,
        nome: String,
    ) {
        compose.setContent { CastanhaTema(escuro = escuro) { Catalogo() } }
        compose.onRoot().captureRoboImage("src/test/screenshots/$nome.png")
    }
}

@Composable
private fun Catalogo() {
    Column(
        modifier =
            Modifier
                .width(390.dp)
                .background(Castanha.cores.surfaceDefault)
                .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CabecalhoTela("Revisar extração", subtitulo = "Confira antes de virar a nova base.")
        CartaoSuave {
            Text("Patrimônio informado", style = Castanha.tipografia.corpo, color = Castanha.cores.textMedium)
            Text("R$ 150.250", style = Castanha.tipografia.destaque, color = Castanha.cores.textIntense)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Etiqueta("Estimativa", Tom.INFORMATIVO)
                ChipOrigem(Origem.RELATORIO)
            }
        }
        CartaoContorno {
            LinhaValor("Data de referência", "03/09/2026")
            LinhaValor("Diferença") { Etiqueta("✓ 0,17% · dentro de 0,5%", Tom.POSITIVO) }
        }
        TituloSecao("Precisa da sua atenção (2)")
        CartaoAviso(Tom.ATENCAO, "▲ DADO FORA DO PLAUSÍVEL", "LFTB11 aparece com +34,36% no mês.")
        CartaoAviso(Tom.NEGATIVO, "● URGENTE", "Posição com quantidade e saldo zero.") { BotaoTexto("Conferir", onClick = {}) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Etiqueta("Pronto", Tom.POSITIVO)
            Etiqueta("Neutro", Tom.NEUTRO)
            Etiqueta("Novo", Tom.NEGATIVO)
        }
        BotaoContorno("Escolher arquivo", onClick = {})
        BotaoPrimario("Confirmar e atualizar", onClick = {})
        BotaoPrimario("Confirmar e atualizar", onClick = {}, habilitado = false)
    }
}
