package com.investimentoeasy.core.domain.analise

import com.investimentoeasy.core.model.SnapshotId
import java.time.Instant

/**
 * Análise interpretada (Camada 3) guardada junto do snapshot. O conteúdo é o JSON validado das
 * 7 abas; a versão do prompt fica registrada para saber com que regras ela foi escrita.
 */
public data class AnaliseGuardada(
    val snapshotId: SnapshotId,
    val geradaEm: Instant,
    val modelo: String,
    val versaoPrompt: String,
    val conteudoJson: String,
)

public interface RepositorioDeAnalises {
    public suspend fun salvar(analise: AnaliseGuardada)

    /** A mais recente para o snapshot, se houver. */
    public suspend fun ultima(snapshotId: SnapshotId): AnaliseGuardada?
}
