package com.investimentoeasy.core.testing

import com.investimentoeasy.core.domain.complemento.ComplementoPlanilha
import com.investimentoeasy.core.domain.complemento.RepositorioDeComplementos
import com.investimentoeasy.core.domain.mercado.Perfil
import com.investimentoeasy.core.domain.mercado.RepositorioDeMercado
import com.investimentoeasy.core.domain.mercado.RepositorioDePerfil
import com.investimentoeasy.core.domain.snapshot.SnapshotRepository
import com.investimentoeasy.core.model.PanoramaMercado
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

public class FakeRepositorioDeComplementos : RepositorioDeComplementos {
    public val salvos: MutableList<ComplementoPlanilha> = mutableListOf()

    override suspend fun salvar(complemento: ComplementoPlanilha) {
        salvos += complemento
    }

    override suspend fun ultimo(snapshotId: SnapshotId): ComplementoPlanilha? = salvos.lastOrNull { it.snapshotId == snapshotId }
}

public class FakeRepositorioDeMercado(
    private var panorama: PanoramaMercado? = null,
) : RepositorioDeMercado {
    override suspend fun salvar(panorama: PanoramaMercado) {
        this.panorama = panorama
    }

    override suspend fun ultimo(): PanoramaMercado? = panorama
}

public class FakeRepositorioDePerfil(
    private var perfil: Perfil? = null,
) : RepositorioDePerfil {
    override fun ler(): Perfil? = perfil

    override fun gravar(perfil: Perfil) {
        this.perfil = perfil
    }
}
