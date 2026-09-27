package com.investimentoeasy.core.domain.snapshot

import com.investimentoeasy.core.domain.validacao.Gravidade
import com.investimentoeasy.core.domain.validacao.Problema
import com.investimentoeasy.core.model.Snapshot
import com.investimentoeasy.core.model.StatusSnapshot
import java.time.Clock

/** R15: o que fazer quando o novo upload tem a mesma data de referência da base. */
public enum class DecisaoMesmaData { SUBSTITUIR, MANTER_ATUAL }

public sealed interface ResultadoConfirmacao {
    /** Confirmado; [virouBase] é falso quando a data é mais antiga que a base (vai para o histórico). */
    public data class Confirmado(val snapshot: Snapshot, val virouBase: Boolean) : ResultadoConfirmacao

    /** R15: mesma data da base; o usuário precisa decidir se substitui. */
    public data class PrecisaDecidirMesmaData(val baseAtual: Snapshot) : ResultadoConfirmacao

    /** O usuário decidiu manter a base atual; nada foi gravado. */
    public data object MantidoAtual : ResultadoConfirmacao

    public data class Bloqueado(val motivos: List<Problema>) : ResultadoConfirmacao
}

/**
 * Confirmação explícita do snapshot (R8), com as regras de base e histórico (R15, R16).
 */
public class ConfirmarSnapshot(
    private val repositorio: SnapshotRepository,
    private val relogio: Clock,
) {
    public suspend operator fun invoke(
        revisao: Revisao,
        aceitouDivergencia: Boolean = false,
        decisaoMesmaData: DecisaoMesmaData? = null,
    ): ResultadoConfirmacao {
        val rascunho = revisao.rascunho
        val bloqueio = bloqueio(revisao, aceitouDivergencia)
        if (bloqueio != null || rascunho == null) return bloqueio ?: ResultadoConfirmacao.Bloqueado(listOf(Problema.DataReferenciaAusente))
        require(rascunho.status == StatusSnapshot.RASCUNHO) { "Snapshot já confirmado é imutável (R16)" }

        val base = repositorio.base()
        return when {
            base == null || rascunho.dataReferencia.isAfter(base.dataReferencia) -> gravar(rascunho, revisao, viraBase = true)
            rascunho.dataReferencia.isBefore(base.dataReferencia) -> gravar(rascunho, revisao, viraBase = false)
            decisaoMesmaData == null -> ResultadoConfirmacao.PrecisaDecidirMesmaData(base)
            decisaoMesmaData == DecisaoMesmaData.MANTER_ATUAL -> ResultadoConfirmacao.MantidoAtual
            else -> gravar(rascunho, revisao, viraBase = true)
        }
    }

    private fun bloqueio(
        revisao: Revisao,
        aceitouDivergencia: Boolean,
    ): ResultadoConfirmacao.Bloqueado? {
        val validacao = revisao.validacao
        val gravidadeQueBloqueia =
            when {
                validacao.impeditivo -> Gravidade.IMPEDITIVO
                validacao.exigeAceite && !aceitouDivergencia -> Gravidade.REQUER_ACEITE
                else -> return null
            }
        return ResultadoConfirmacao.Bloqueado(validacao.problemas.filter { it.gravidade == gravidadeQueBloqueia })
    }

    /** R16: nada é sobrescrito; uma nova confirmação na mesma data vira a próxima versão. */
    private suspend fun gravar(
        rascunho: Snapshot,
        revisao: Revisao,
        viraBase: Boolean,
    ): ResultadoConfirmacao.Confirmado {
        val data = rascunho.dataReferencia
        val versao = (repositorio.confirmados().filter { it.dataReferencia == data }.maxOfOrNull { it.versao } ?: 0) + 1
        val confirmado =
            rascunho.copy(
                status = StatusSnapshot.CONFIRMADO,
                versao = versao,
                confirmadoEm = relogio.instant(),
                divergenciaAceita = revisao.validacao.exigeAceite,
            )
        repositorio.inserirConfirmado(confirmado)
        if (viraBase) repositorio.definirBase(confirmado.id)
        return ResultadoConfirmacao.Confirmado(confirmado, viraBase)
    }
}
