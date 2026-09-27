package com.investimentoeasy.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.investimentoeasy.core.designsystem.Castanha
import com.investimentoeasy.core.designsystem.Icones
import com.investimentoeasy.core.designsystem.componentes.CabecalhoTela
import com.investimentoeasy.core.designsystem.componentes.CartaoAviso
import com.investimentoeasy.core.designsystem.componentes.Tom
import com.investimentoeasy.feature.analysis.RotaAnalise
import com.investimentoeasy.feature.analysis.analise
import com.investimentoeasy.feature.portfolio.RotaCarteira
import com.investimentoeasy.feature.portfolio.carteira
import com.investimentoeasy.feature.upload.Conclusao
import com.investimentoeasy.feature.upload.RotaImportacao
import com.investimentoeasy.feature.upload.importacao
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
data object RotaAlertas

/** Abas da barra inferior do protótipo; "Enviar" abre o fluxo de importação em tela cheia. */
enum class Aba(
    val rotulo: String,
    val icone: ImageVector,
    val rota: Any,
    val classe: KClass<*>,
) {
    CARTEIRA("Carteira", Icones.carteira, RotaCarteira, RotaCarteira::class),
    ANALISE("Análise", Icones.analise, RotaAnalise, RotaAnalise::class),
    ALERTAS("Alertas", Icones.alertas, RotaAlertas, RotaAlertas::class),
    ENVIAR("Enviar", Icones.enviar, RotaImportacao, RotaImportacao::class),
}

@Composable
fun AppNavegacao(navController: NavHostController = rememberNavController()) {
    val snackbar = remember { SnackbarHostState() }
    val escopo = rememberCoroutineScope()
    val entrada by navController.currentBackStackEntryAsState()
    val destino = entrada?.destination
    val naImportacao = destino?.hierarchy?.any { it.hasRoute(RotaImportacao::class) } == true

    Scaffold(
        containerColor = Castanha.cores.surfaceDefault,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (!naImportacao) {
                BarraInferior(
                    abaAtual = Aba.entries.firstOrNull { aba -> destino?.hierarchy?.any { it.hasRoute(aba.classe) } == true },
                    aoEscolher = { aba -> navController.irPara(aba) },
                )
            }
        },
    ) { margens ->
        NavHost(navController, startDestination = RotaCarteira, modifier = Modifier.padding(margens)) {
            carteira(aoEnviar = { navController.irPara(Aba.ENVIAR) })
            analise()
            composable<RotaAlertas> {
                EmBreve("Alertas", "Os alertas de concentração, vencimento e queda chegam na Fase 2, com o acompanhamento diário.")
            }
            importacao(
                navController = navController,
                aoConcluir = { conclusao ->
                    navController.popBackStack(RotaImportacao, inclusive = true)
                    escopo.launch { snackbar.showSnackbar(mensagemDe(conclusao)) }
                },
                aoSair = { navController.popBackStack(RotaImportacao, inclusive = true) },
            )
        }
    }
}

fun mensagemDe(conclusao: Conclusao): String =
    when (conclusao) {
        Conclusao.NOVA_BASE -> "Carteira atualizada: este upload é a nova base."
        Conclusao.HISTORICO -> "Upload guardado no histórico: a data é anterior à base atual."
        Conclusao.MANTIDA_BASE_ATUAL -> "Nada mudou: a base atual foi mantida."
        Conclusao.PLANILHA_GUARDADA -> "Dados da planilha guardados junto da base atual."
    }

private fun NavHostController.irPara(aba: Aba) {
    navigate(aba.rota) {
        popUpTo(graph.findStartDestination().id) { saveState = aba != Aba.ENVIAR }
        launchSingleTop = true
        restoreState = aba != Aba.ENVIAR
    }
}

@Composable
private fun BarraInferior(
    abaAtual: Aba?,
    aoEscolher: (Aba) -> Unit,
) {
    val cores = Castanha.cores
    NavigationBar(containerColor = cores.surfaceDefault, tonalElevation = 0.dp) {
        Aba.entries.forEach { aba ->
            NavigationBarItem(
                modifier = Modifier.testTag("aba_${aba.name}"),
                selected = aba == abaAtual,
                onClick = { aoEscolher(aba) },
                icon = { Icon(aba.icone, contentDescription = null) },
                label = { Text(aba.rotulo, style = Castanha.tipografia.rotulo) },
                colors =
                    NavigationBarItemDefaults.colors(
                        selectedIconColor = cores.accentSolidMedium,
                        selectedTextColor = cores.accentSolidMedium,
                        unselectedIconColor = cores.textMedium,
                        unselectedTextColor = cores.textMedium,
                        indicatorColor = cores.accentSolidSoft,
                    ),
            )
        }
    }
}

@Composable
private fun EmBreve(
    titulo: String,
    texto: String,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        CabecalhoTela(titulo)
        CartaoAviso(Tom.INFORMATIVO, "ⓘ EM CONSTRUÇÃO", texto)
    }
}
