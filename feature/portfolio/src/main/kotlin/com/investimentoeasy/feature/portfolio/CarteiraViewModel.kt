package com.investimentoeasy.feature.portfolio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investimentoeasy.core.common.Io
import com.investimentoeasy.core.domain.alocacao.FatiaAlocacao
import com.investimentoeasy.core.domain.alocacao.alocacaoPorGrupo
import com.investimentoeasy.core.domain.snapshot.SnapshotRepository
import com.investimentoeasy.core.model.Money
import com.investimentoeasy.core.model.Origem
import com.investimentoeasy.core.model.Snapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/** Quão recente é o último upload (jornada: sugere novo upload após 30 dias, desatualizado após 45). */
enum class Atualidade { EM_DIA, SUGERIR_UPLOAD, DESATUALIZADA }

sealed interface EstadoCarteira {
    data object Carregando : EstadoCarteira

    data object SemCarteira : EstadoCarteira

    data class ComBase(
        val dataReferencia: LocalDate,
        val diasDesdeReferencia: Long,
        val atualidade: Atualidade,
        val patrimonio: Money,
        val origemPatrimonio: Origem,
        val versao: Int,
        val divergenciaAceita: Boolean,
        val alocacao: List<FatiaAlocacao>,
    ) : EstadoCarteira
}

@HiltViewModel
class CarteiraViewModel
    @Inject
    constructor(
        private val repositorio: SnapshotRepository,
        private val relogio: Clock,
        @param:Io private val io: CoroutineDispatcher,
    ) : ViewModel() {
        private val _estado = MutableStateFlow<EstadoCarteira>(EstadoCarteira.Carregando)
        val estado: StateFlow<EstadoCarteira> = _estado.asStateFlow()

        /** Chamado sempre que a tela aparece: a base pode ter mudado num upload. */
        fun carregar() {
            viewModelScope.launch {
                val base = withContext(io) { repositorio.base() }
                _estado.value = base?.let(::estadoDe) ?: EstadoCarteira.SemCarteira
            }
        }

        private fun estadoDe(base: Snapshot): EstadoCarteira.ComBase {
            val dias = ChronoUnit.DAYS.between(base.dataReferencia, LocalDate.now(relogio)).coerceAtLeast(0)
            // Patrimônio informado pelo relatório; sem ele, a soma das posições (calculada).
            val (patrimonio, origem) =
                base.patrimonioInformado?.let { it.valor to it.origem } ?: (base.somaPosicoes to Origem.CALCULADO)
            return EstadoCarteira.ComBase(
                dataReferencia = base.dataReferencia,
                diasDesdeReferencia = dias,
                atualidade =
                    when {
                        dias > DIAS_DESATUALIZADA -> Atualidade.DESATUALIZADA
                        dias > DIAS_SUGERIR_UPLOAD -> Atualidade.SUGERIR_UPLOAD
                        else -> Atualidade.EM_DIA
                    },
                patrimonio = patrimonio,
                origemPatrimonio = origem,
                versao = base.versao,
                divergenciaAceita = base.divergenciaAceita,
                alocacao = alocacaoPorGrupo(base.posicoes),
            )
        }

        companion object {
            const val DIAS_SUGERIR_UPLOAD = 30L
            const val DIAS_DESATUALIZADA = 45L
        }
    }
