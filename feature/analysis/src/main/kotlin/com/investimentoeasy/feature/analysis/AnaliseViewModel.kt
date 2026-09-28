package com.investimentoeasy.feature.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investimentoeasy.core.common.Io
import com.investimentoeasy.core.domain.analise.AnalisarCarteira
import com.investimentoeasy.core.domain.analise.AnaliseDeterministica
import com.investimentoeasy.core.domain.complemento.ComplementoPlanilha
import com.investimentoeasy.core.domain.complemento.RepositorioDeComplementos
import com.investimentoeasy.core.domain.mercado.Perfil
import com.investimentoeasy.core.domain.mercado.ProvedorDeMercado
import com.investimentoeasy.core.domain.mercado.RepositorioDeMercado
import com.investimentoeasy.core.domain.mercado.RepositorioDePerfil
import com.investimentoeasy.core.domain.mercado.leituraDeCambio
import com.investimentoeasy.core.domain.mercado.leituraDeJuros
import com.investimentoeasy.core.domain.recomendacao.EntradaDoMotor
import com.investimentoeasy.core.domain.recomendacao.MotorDeRecomendacao
import com.investimentoeasy.core.domain.recomendacao.Recomendacao
import com.investimentoeasy.core.domain.snapshot.SnapshotRepository
import com.investimentoeasy.core.model.ChaveAtivo
import com.investimentoeasy.core.model.PanoramaMercado
import com.investimentoeasy.core.model.Snapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import javax.inject.Inject

/** As 7 abas da skill, na ordem do protótipo. */
enum class AbaAnalise(
    val rotulo: String,
) {
    O_QUE_FAZER("O que fazer"),
    MERCADO("Mercado"),
    ALOCACAO("Alocação"),
    FUNDOS("Fundos"),
    FIIS("FIIs"),
    ACOES_ETFS("Ações e ETFs"),
    ALERTAS("Alertas"),
}

data class EstadoAnalise(
    val carregando: Boolean = true,
    val semCarteira: Boolean = false,
    val base: Snapshot? = null,
    val deterministica: AnaliseDeterministica? = null,
    /** Só existe com o perfil escolhido: sem perfil não há alvo para recomendar. */
    val recomendacao: Recomendacao? = null,
    val perfil: Perfil? = null,
    val panorama: PanoramaMercado? = null,
    val atualizandoMercado: Boolean = false,
    val escolhendoPerfil: Boolean = false,
    val aba: AbaAnalise = AbaAnalise.O_QUE_FAZER,
    /** Dados da planilha "Posição Detalhada" guardados para a base, se o usuário enviou. */
    val complemento: ComplementoPlanilha? = null,
)

/** O que a análise lê e grava: base, planilha, mercado em cache e perfil. */
class FontesDaAnalise
    @Inject
    constructor(
        val snapshots: SnapshotRepository,
        val complementos: RepositorioDeComplementos,
        val mercado: RepositorioDeMercado,
        val perfil: RepositorioDePerfil,
    )

/**
 * Toda a análise roda no aparelho, sem custo: camadas 1 e 2 (cálculos e alertas) e o motor de
 * recomendação. A rede só é usada para os dados públicos de mercado, que ficam em cache.
 */
@HiltViewModel
class AnaliseViewModel
    @Inject
    constructor(
        private val fontes: FontesDaAnalise,
        private val provedor: ProvedorDeMercado,
        private val relogio: Clock,
        @param:Io private val io: CoroutineDispatcher,
    ) : ViewModel() {
        private val analisar = AnalisarCarteira()
        private val motor = MotorDeRecomendacao()
        private val _estado = MutableStateFlow(EstadoAnalise())
        val estado: StateFlow<EstadoAnalise> = _estado.asStateFlow()

        /** Chamado sempre que a tela aparece: a base pode ter mudado num upload. Mercado velho é atualizado. */
        fun carregar() {
            viewModelScope.launch {
                val lido =
                    withContext(io) {
                        val base = fontes.snapshots.base()
                        Triple(base, base?.let { fontes.complementos.ultimo(it.id) }, fontes.mercado.ultimo())
                    }
                val (base, complemento, panorama) = lido
                val perfil = fontes.perfil.ler()
                _estado.update {
                    recalcular(
                        it.copy(carregando = false, semCarteira = base == null, base = base, complemento = complemento, perfil = perfil),
                        panorama,
                    )
                }
                if (base != null && mercadoVelho(panorama)) atualizarMercado()
            }
        }

        fun selecionarAba(aba: AbaAnalise) {
            _estado.update { it.copy(aba = aba) }
        }

        fun trocarPerfil() {
            _estado.update { it.copy(escolhendoPerfil = true) }
        }

        fun escolherPerfil(perfil: Perfil) {
            fontes.perfil.gravar(perfil)
            _estado.update { recalcular(it.copy(perfil = perfil, escolhendoPerfil = false), it.panorama) }
        }

        fun atualizarMercado() {
            val base = _estado.value.base ?: return
            if (_estado.value.atualizandoMercado) return
            _estado.update { it.copy(atualizandoMercado = true) }
            viewModelScope.launch {
                val panorama =
                    withContext(io) {
                        provedor.atualizar(tickers(base)).also { fontes.mercado.salvar(it) }
                    }
                _estado.update { recalcular(it.copy(atualizandoMercado = false), panorama) }
            }
        }

        private fun recalcular(
            estado: EstadoAnalise,
            panorama: PanoramaMercado?,
        ): EstadoAnalise {
            val base = estado.base ?: return estado.copy(panorama = panorama, deterministica = null, recomendacao = null)
            val hoje = LocalDate.now(relogio)
            val analise = analisar(base, panorama?.let { leituraDeJuros(it, hoje) }, panorama?.let { leituraDeCambio(it, hoje) })
            val recomendacao =
                estado.perfil?.let { perfil -> motor(EntradaDoMotor(base, analise, perfil, panorama, estado.complemento, hoje)) }
            return estado.copy(panorama = panorama, deterministica = analise, recomendacao = recomendacao)
        }

        private fun mercadoVelho(panorama: PanoramaMercado?): Boolean =
            panorama == null || Duration.between(panorama.obtidoEm, relogio.instant()) > VALIDADE_DO_MERCADO

        private companion object {
            /** Cotação e Focus mudam no dia; mais que isso, a tela atualiza sozinha ao abrir. */
            val VALIDADE_DO_MERCADO: Duration = Duration.ofHours(6)
        }
    }

/** Só tickers (nunca valores ou quantidades) saem do aparelho, para buscar cotação. */
internal fun tickers(base: Snapshot): Set<String> =
    base.posicoes.filter { !it.saldo.valor.isZero }.mapNotNull { (it.ativo.chave as? ChaveAtivo.Ticker)?.codigo }.toSet()
