package com.investimentoeasy.core.testing

import com.investimentoeasy.core.domain.analise.AnaliseGuardada
import com.investimentoeasy.core.domain.analise.RepositorioDeAnalises
import com.investimentoeasy.core.domain.snapshot.SnapshotRepository
import com.investimentoeasy.core.model.Snapshot
import com.investimentoeasy.core.model.SnapshotId

/** Repositório em memória que reforça a imutabilidade (R16): reinserir um id falha. */
public class FakeSnapshotRepository(
    iniciais: List<Snapshot> = emptyList(),
    base: SnapshotId? = null,
) : SnapshotRepository {
    private val snapshots = linkedMapOf<SnapshotId, Snapshot>().apply { iniciais.forEach { put(it.id, it) } }
    private var baseId: SnapshotId? = base

    override suspend fun base(): Snapshot? = baseId?.let(snapshots::get)

    override suspend fun confirmados(): List<Snapshot> = snapshots.values.toList()

    override suspend fun inserirConfirmado(snapshot: Snapshot) {
        check(snapshot.id !in snapshots) { "Snapshot ${snapshot.id} já existe e é imutável" }
        snapshots[snapshot.id] = snapshot
    }

    override suspend fun definirBase(id: SnapshotId) {
        check(id in snapshots) { "Snapshot $id não existe" }
        baseId = id
    }
}

public class FakeRepositorioDeAnalises : RepositorioDeAnalises {
    public val salvas: MutableList<AnaliseGuardada> = mutableListOf()

    override suspend fun salvar(analise: AnaliseGuardada) {
        salvas += analise
    }

    override suspend fun ultima(snapshotId: SnapshotId): AnaliseGuardada? =
        salvas.filter { it.snapshotId == snapshotId }.maxByOrNull { it.geradaEm }
}
