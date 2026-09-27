package com.investimentoeasy.core.domain.complemento

import com.investimentoeasy.core.domain.snapshot.SnapshotRepository
import com.investimentoeasy.core.model.PlanilhaPosicao
import com.investimentoeasy.core.model.Snapshot
import com.investimentoeasy.core.model.SnapshotId
import java.time.Clock

/** Complementos da planilha, vários por snapshot; vale o mais recente. */
public interface RepositorioDeComplementos {
    public suspend fun salvar(complemento: ComplementoPlanilha)

    public suspend fun ultimo(snapshotId: SnapshotId): ComplementoPlanilha?
}

public sealed interface PreparoComplemento {
    /** A planilha complementa uma base; sem base (nenhum PDF confirmado), não há o que complementar. */
    public data object SemBase : PreparoComplemento

    public data class Pronto(
        val base: Snapshot,
        val casamento: Casamento,
    ) : PreparoComplemento
}

/**
 * Planilha → casamento com a base atual → (o usuário confere) → guardado ao lado da base.
 * Nada é guardado sem [guardar] explícito, como no PDF (R8).
 */
public class ComplementarBase(
    private val snapshots: SnapshotRepository,
    private val complementos: RepositorioDeComplementos,
    private val relogio: Clock,
    private val casar: CasarPlanilha = CasarPlanilha(),
) {
    public suspend fun preparar(planilha: PlanilhaPosicao): PreparoComplemento {
        val base = snapshots.base() ?: return PreparoComplemento.SemBase
        return PreparoComplemento.Pronto(base, casar(base, planilha, relogio.instant()))
    }

    public suspend fun guardar(casamento: Casamento) {
        complementos.salvar(casamento.complemento)
    }
}
