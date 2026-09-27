package com.investimentoeasy.feature.upload

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investimentoeasy.core.common.Io
import com.investimentoeasy.core.documentos.LeitorDeArquivo
import com.investimentoeasy.core.domain.complemento.ComplementarBase
import com.investimentoeasy.core.domain.complemento.PreparoComplemento
import com.investimentoeasy.core.domain.snapshot.ConfirmarSnapshot
import com.investimentoeasy.core.domain.snapshot.DecisaoMesmaData
import com.investimentoeasy.core.domain.snapshot.PrepararRevisao
import com.investimentoeasy.core.domain.snapshot.ResultadoConfirmacao
import com.investimentoeasy.core.domain.snapshot.Revisao
import com.investimentoeasy.core.importacao.ImportarRelatorio
import com.investimentoeasy.core.importacao.MotivoFalha
import com.investimentoeasy.core.importacao.ResultadoImportacao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDate
import javax.inject.Inject

sealed interface Leitura {
    data object Aguardando : Leitura

    data object Lendo : Leitura

    data class Lida(
        val nomeArquivo: String,
        val tamanhoBytes: Int,
        val paginas: Int,
    ) : Leitura

    /** Planilha "Posição Detalhada": não vira base, complementa a atual. */
    data class PlanilhaLida(
        val nomeArquivo: String,
        val tamanhoBytes: Int,
    ) : Leitura

    /** Planilha lida, mas ainda não há base (PDF confirmado) para complementar. */
    data object PlanilhaSemBase : Leitura

    data class Falhou(
        val motivo: MotivoFalha,
    ) : Leitura
}

/** Como a confirmação terminou; consumido pela navegação. */
enum class Conclusao { NOVA_BASE, HISTORICO, MANTIDA_BASE_ATUAL, PLANILHA_GUARDADA }

data class EstadoImportacao(
    val leitura: Leitura = Leitura.Aguardando,
    val revisao: Revisao? = null,
    val aceitouDivergencia: Boolean = false,
    val confirmando: Boolean = false,
    /** R15: já existe uma base nesta data; o usuário decide se substitui. */
    val baseMesmaData: LocalDate? = null,
    val conclusao: Conclusao? = null,
    /** Planilha casada com a base, aguardando o usuário conferir e guardar. */
    val complemento: PreparoComplemento.Pronto? = null,
) {
    val revisaoUi: RevisaoUi? get() = revisao?.let { montarRevisaoUi(it, aceitouDivergencia) }

    val conferencia: ConferenciaPlanilhaUi? get() = complemento?.let { montarConferencia(it.base, it.casamento) }
}

/**
 * Fluxo Enviar → Revisar → Confirmar (PDF) ou Enviar → Conferir planilha → Guardar (.xlsx).
 * Compartilhado pelas telas do grafo de navegação da importação. Nada vira base sem [confirmar] (R8).
 */
@HiltViewModel
class ImportacaoViewModel
    @Inject
    constructor(
        private val leitor: LeitorDeArquivo,
        private val importar: ImportarRelatorio,
        private val confirmarSnapshot: ConfirmarSnapshot,
        private val complementarBase: ComplementarBase,
        @param:Io private val io: CoroutineDispatcher,
    ) : ViewModel() {
        private val preparar = PrepararRevisao()
        private val _estado = MutableStateFlow(EstadoImportacao())
        val estado: StateFlow<EstadoImportacao> = _estado.asStateFlow()

        fun arquivoEscolhido(uri: Uri) {
            _estado.value = EstadoImportacao(leitura = Leitura.Lendo)
            viewModelScope.launch {
                val resultado =
                    withContext(io) {
                        try {
                            importar.importar(leitor.ler(uri))
                        } catch (_: IOException) {
                            ResultadoImportacao.Falha(MotivoFalha.ARQUIVO_ILEGIVEL)
                        }
                    }
                _estado.value =
                    when (resultado) {
                        is ResultadoImportacao.Lido ->
                            EstadoImportacao(
                                leitura = Leitura.Lida(resultado.nomeArquivo, resultado.tamanhoBytes, resultado.paginas),
                                revisao = resultado.revisao,
                            )
                        is ResultadoImportacao.PlanilhaLida ->
                            when (val preparo = withContext(io) { complementarBase.preparar(resultado.planilha) }) {
                                PreparoComplemento.SemBase -> EstadoImportacao(leitura = Leitura.PlanilhaSemBase)
                                is PreparoComplemento.Pronto ->
                                    EstadoImportacao(
                                        leitura = Leitura.PlanilhaLida(resultado.nomeArquivo, resultado.tamanhoBytes),
                                        complemento = preparo,
                                    )
                            }
                        is ResultadoImportacao.Falha -> EstadoImportacao(leitura = Leitura.Falhou(resultado.motivo))
                    }
            }
        }

        /** Guarda os dados da planilha ao lado da base; nada é guardado sem este passo (R8). */
        fun guardarPlanilha() {
            val atual = _estado.value
            val complemento = atual.complemento ?: return
            if (atual.confirmando || atual.conferencia?.podeGuardar != true) return
            _estado.update { it.copy(confirmando = true) }
            viewModelScope.launch {
                withContext(io) { complementarBase.guardar(complemento.casamento) }
                _estado.update { it.copy(confirmando = false, conclusao = Conclusao.PLANILHA_GUARDADA) }
            }
        }

        /** R1: data informada pelo usuário quando o arquivo não traz. */
        fun informarDataReferencia(data: LocalDate) {
            _estado.update { atual ->
                val revisao = atual.revisao ?: return@update atual
                atual.copy(revisao = preparar(revisao.extracao, dataInformadaPeloUsuario = data))
            }
        }

        /** R3: aceite explícito da divergência entre soma e patrimônio. */
        fun aceitarDivergencia(aceita: Boolean) {
            _estado.update { it.copy(aceitouDivergencia = aceita) }
        }

        fun confirmar(decisao: DecisaoMesmaData? = null) {
            val atual = _estado.value
            val revisao = atual.revisao ?: return
            if (atual.confirmando || !(atual.revisaoUi?.podeConfirmar ?: false)) return
            _estado.update { it.copy(confirmando = true, baseMesmaData = null) }
            viewModelScope.launch {
                val resultado = withContext(io) { confirmarSnapshot(revisao, atual.aceitouDivergencia, decisao) }
                _estado.update { estado ->
                    when (resultado) {
                        is ResultadoConfirmacao.Confirmado ->
                            estado.copy(
                                confirmando = false,
                                conclusao = if (resultado.virouBase) Conclusao.NOVA_BASE else Conclusao.HISTORICO,
                            )
                        is ResultadoConfirmacao.PrecisaDecidirMesmaData ->
                            estado.copy(confirmando = false, baseMesmaData = resultado.baseAtual.dataReferencia)
                        ResultadoConfirmacao.MantidoAtual -> estado.copy(confirmando = false, conclusao = Conclusao.MANTIDA_BASE_ATUAL)
                        // A tela só habilita Confirmar sem bloqueios; chegar aqui é a validação mudando entre telas.
                        is ResultadoConfirmacao.Bloqueado -> estado.copy(confirmando = false)
                    }
                }
            }
        }

        fun cancelarDecisaoMesmaData() {
            _estado.update { it.copy(baseMesmaData = null) }
        }

        fun conclusaoTratada() {
            _estado.value = EstadoImportacao()
        }
    }
