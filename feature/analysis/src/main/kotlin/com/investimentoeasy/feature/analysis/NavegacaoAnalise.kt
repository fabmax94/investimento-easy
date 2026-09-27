package com.investimentoeasy.feature.analysis

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object RotaAnalise

fun NavGraphBuilder.analise() {
    composable<RotaAnalise> {
        val viewModel: AnaliseViewModel = hiltViewModel()
        val estado by viewModel.estado.collectAsStateWithLifecycle()
        LaunchedEffect(Unit) { viewModel.carregar() }
        AnaliseScreen(
            estado = estado,
            aoSelecionarAba = viewModel::selecionarAba,
            aoGerar = viewModel::gerarAnalise,
            aoSalvarChave = viewModel::salvarChave,
            aoApagarChave = viewModel::apagarChave,
        )
    }
}
