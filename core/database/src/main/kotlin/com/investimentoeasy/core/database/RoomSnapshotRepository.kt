package com.investimentoeasy.core.database

import com.investimentoeasy.core.domain.snapshot.SnapshotRepository
import com.investimentoeasy.core.model.Snapshot
import com.investimentoeasy.core.model.SnapshotId
import javax.inject.Inject

internal class RoomSnapshotRepository
    @Inject
    constructor(
        private val dao: SnapshotDao,
    ) : SnapshotRepository {
        override suspend fun base(): Snapshot? = dao.idDaBase()?.let { id -> carregar(id) }

        override suspend fun confirmados(): List<Snapshot> = dao.snapshots().map { it.paraModelo(dao.posicoes(it.id)) }

        override suspend fun inserirConfirmado(snapshot: Snapshot) {
            dao.inserir(snapshot.paraEntidade(), snapshot.posicoesParaEntidades())
        }

        override suspend fun definirBase(id: SnapshotId) {
            checkNotNull(dao.snapshot(id.valor)) { "Snapshot ${id.valor} não existe" }
            dao.definirBase(BaseEntity(snapshotId = id.valor))
        }

        private suspend fun carregar(id: String): Snapshot? = dao.snapshot(id)?.paraModelo(dao.posicoes(id))
    }
