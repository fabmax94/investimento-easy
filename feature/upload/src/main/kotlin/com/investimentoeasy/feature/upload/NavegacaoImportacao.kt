package com.investimentoeasy.feature.upload

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import kotlinx.serialization.Serializable

@Serializable
data object RotaImportacao

@Serializable
internal data object RotaEnviar

@Serializable
internal data object RotaRevisar

/**
 * Grafo Enviar → Revisar. As duas telas compartilham o [ImportacaoViewModel] do grafo.
 * [aoConcluir] recebe como a confirmação terminou; a navegação de volta é de quem chama.
 */
fun NavGraphBuilder.importacao(
    navController: NavController,
    aoConcluir: (Conclusao) -> Unit,
    aoSair: () -> Unit,
) {
    navigation<RotaImportacao>(startDestination = RotaEnviar) {
        composable<RotaEnviar> { entrada ->
            val viewModel = viewModelDoGrafo(navController, entrada)
            val estado by viewModel.estado.collectAsStateWithLifecycle()
            val seletor =
                rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
                    uri?.let(viewModel::arquivoEscolhido)
                }
            EnviarCarteiraScreen(
                estado = estado,
                aoEscolherArquivo = { seletor.launch(arrayOf(MIME_PDF)) },
                aoRevisar = { navController.navigate(RotaRevisar) },
                aoVoltar = aoSair,
            )
        }
        composable<RotaRevisar> { entrada ->
            val viewModel = viewModelDoGrafo(navController, entrada)
            val estado by viewModel.estado.collectAsStateWithLifecycle()
            LaunchedEffect(estado.conclusao) {
                estado.conclusao?.let { conclusao ->
                    aoConcluir(conclusao)
                    viewModel.conclusaoTratada()
                }
            }
            estado.revisaoUi?.let { revisao ->
                RevisarExtracaoScreen(
                    revisao = revisao,
                    confirmando = estado.confirmando,
                    baseMesmaData = estado.baseMesmaData,
                    aoInformarData = viewModel::informarDataReferencia,
                    aoAceitarDivergencia = viewModel::aceitarDivergencia,
                    aoConfirmar = viewModel::confirmar,
                    aoCancelarDecisao = viewModel::cancelarDecisaoMesmaData,
                    aoVoltar = { navController.popBackStack() },
                )
            }
        }
    }
}

@Composable
private fun viewModelDoGrafo(
    navController: NavController,
    entrada: NavBackStackEntry,
): ImportacaoViewModel {
    val doGrafo = remember(entrada) { navController.getBackStackEntry(RotaImportacao) }
    return hiltViewModel(doGrafo)
}

private const val MIME_PDF = "application/pdf"
