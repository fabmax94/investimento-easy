package com.investimentoeasy.core.domain.snapshot

import com.investimentoeasy.core.model.Snapshot
import com.investimentoeasy.core.model.SnapshotId

/**
 * Persistência de snapshots confirmados.
 *
 * R16: não há operação de edição nem de exclusão; uma correção entra como nova versão.
 */
public interface SnapshotRepository {
    /** Snapshot que hoje é a base da carteira. */
    public suspend fun base(): Snapshot?

    /** Todos os snapshots confirmados (base e histórico). */
    public suspend fun confirmados(): List<Snapshot>

    /** Insere um snapshot confirmado. Deve falhar se o id já existir. */
    public suspend fun inserirConfirmado(snapshot: Snapshot)

    public suspend fun definirBase(id: SnapshotId)
}
