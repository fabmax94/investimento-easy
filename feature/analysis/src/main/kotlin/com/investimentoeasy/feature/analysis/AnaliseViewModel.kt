package com.investimentoeasy.feature.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investimentoeasy.core.ai.FabricaDeModelo
import com.investimentoeasy.core.ai.GerarAnaliseIa
import com.investimentoeasy.core.ai.RespostaDoModelo
import com.investimentoeasy.core.ai.ResultadoAnaliseIa
import com.investimentoeasy.core.ai.SaidaAnalise
import com.investimentoeasy.core.ai.paraJson
import com.investimentoeasy.core.ai.saidaDeJson
import com.investimentoeasy.core.common.Io
import com.investimentoeasy.core.domain.analise.AnalisarCarteira
import com.investimentoeasy.core.domain.analise.AnaliseDeterministica
import com.investimentoeasy.core.domain.analise.AnaliseGuardada
import com.investimentoeasy.core.domain.analise.RepositorioDeAnalises
import com.investimentoeasy.core.domain.snapshot.SnapshotRepository
import com.investimentoeasy.core.model.Snapshot
import com.investimentoeasy.core.seguranca.CofreDeChave
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.Instant
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

data class AnaliseIa(
    val saida: SaidaAnalise,
    val geradaEm: Instant,
    val modelo: String,
)

data class EstadoAnalise(
    val carregando: Boolean = true,
    val semCarteira: Boolean = false,
    val base: Snapshot? = null,
    val deterministica: AnaliseDeterministica? = null,
    val ia: AnaliseIa? = null,
    val aba: AbaAnalise = AbaAnalise.O_QUE_FAZER,
    val temChave: Boolean = false,
    val gerando: Boolean = false,
    val erro: String? = null,
)

/**
 * As camadas 1 e 2 rodam sempre, no aparelho e sem custo. A Camada 3 (Claude) só roda quando o
 * usuário pede, com a chave dele, e o resultado fica guardado para a mesma base.
 */
@HiltViewModel
class AnaliseViewModel
    @Inject
    constructor(
        private val snapshots: SnapshotRepository,
        private val analises: RepositorioDeAnalises,
        private val cofre: CofreDeChave,
        private val fabrica: FabricaDeModelo,
        private val relogio: Clock,
        @param:Io private val io: CoroutineDispatcher,
    ) : ViewModel() {
        private val analisar = AnalisarCarteira()
        private val _estado = MutableStateFlow(EstadoAnalise())
        val estado: StateFlow<EstadoAnalise> = _estado.asStateFlow()

        /** Chamado sempre que a tela aparece: a base pode ter mudado num upload. */
        fun carregar() {
            viewModelScope.launch {
                val carregado = withContext(io) { carregarDoDisco() }
                _estado.update {
                    it.copy(
                        carregando = false,
                        semCarteira = carregado.base == null,
                        base = carregado.base,
                        deterministica = carregado.deterministica,
                        ia = carregado.ia,
                        temChave = carregado.temChave,
                    )
                }
            }
        }

        private suspend fun carregarDoDisco(): EstadoAnalise {
            val base = snapshots.base()
            val guardada = base?.let { analises.ultima(it.id) }
            return EstadoAnalise(
                base = base,
                deterministica = base?.let(analisar::invoke),
                ia = guardada?.let { g -> saidaDeJson(g.conteudoJson)?.let { AnaliseIa(it, g.geradaEm, g.modelo) } },
                temChave = cofre.ler() != null,
            )
        }

        fun selecionarAba(aba: AbaAnalise) {
            _estado.update { it.copy(aba = aba) }
        }

        fun salvarChave(chave: String) {
            if (chave.isBlank()) return
            viewModelScope.launch {
                withContext(io) { cofre.gravar(chave) }
                _estado.update { it.copy(temChave = true, erro = null) }
            }
        }

        fun apagarChave() {
            viewModelScope.launch {
                withContext(io) { cofre.apagar() }
                _estado.update { it.copy(temChave = false) }
            }
        }

        fun gerarAnalise() {
            val atual = _estado.value
            val base = atual.base ?: return
            val deterministica = atual.deterministica ?: return
            if (atual.gerando) return
            _estado.update { it.copy(gerando = true, erro = null) }
            viewModelScope.launch {
                val resultado =
                    withContext(io) {
                        val chave = cofre.ler() ?: return@withContext null
                        val r = GerarAnaliseIa(fabrica.criar(chave))(base, deterministica)
                        if (r is ResultadoAnaliseIa.Gerada) {
                            analises.salvar(AnaliseGuardada(base.id, relogio.instant(), r.modelo, r.versaoPrompt, r.saida.paraJson()))
                        }
                        r
                    }
                _estado.update { estado ->
                    when (resultado) {
                        null ->
                            estado.copy(
                                gerando = false,
                                temChave = false,
                                erro = "Configure a chave da API do Claude para gerar a análise.",
                            )
                        is ResultadoAnaliseIa.Gerada ->
                            estado.copy(gerando = false, ia = AnaliseIa(resultado.saida, relogio.instant(), resultado.modelo), erro = null)
                        is ResultadoAnaliseIa.Rejeitada -> estado.copy(gerando = false, erro = mensagemRejeitada(resultado))
                        is ResultadoAnaliseIa.Falhou -> estado.copy(gerando = false, erro = mensagemDe(resultado.motivo))
                    }
                }
            }
        }
    }

internal fun mensagemRejeitada(r: ResultadoAnaliseIa.Rejeitada): String =
    "A resposta do Claude citou dados que não existem na sua carteira e foi descartada " +
        "(${r.violacoes.size} ${if (r.violacoes.size == 1) "problema" else "problemas"}). Nada foi salvo. Tente gerar de novo."

internal fun mensagemDe(motivo: RespostaDoModelo): String =
    when (motivo) {
        RespostaDoModelo.ChaveInvalida -> "A chave da API foi recusada. Confira a chave nas configurações da análise."
        RespostaDoModelo.LimiteDeUso -> "Limite de uso da API atingido. Tente de novo em alguns minutos."
        RespostaDoModelo.SemConexao -> "Sem conexão com a API do Claude. Verifique a internet e tente de novo."
        RespostaDoModelo.Recusa -> "O modelo recusou gerar esta análise."
        RespostaDoModelo.Incompleta -> "A resposta veio incompleta. Tente de novo."
        is RespostaDoModelo.Erro -> "Não foi possível gerar a análise: ${motivo.mensagem}."
        is RespostaDoModelo.Texto -> "Resposta inesperada do modelo."
    }
