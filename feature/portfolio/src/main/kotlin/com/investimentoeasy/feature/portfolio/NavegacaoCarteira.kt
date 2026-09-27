package com.investimentoeasy.feature.portfolio

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object RotaCarteira

fun NavGraphBuilder.carteira(aoEnviar: () -> Unit) {
    composable<RotaCarteira> {
        val viewModel: CarteiraViewModel = hiltViewModel()
        val estado by viewModel.estado.collectAsStateWithLifecycle()
        LaunchedEffect(Unit) { viewModel.carregar() }
        CarteiraScreen(estado = estado, aoEnviar = aoEnviar)
    }
}
