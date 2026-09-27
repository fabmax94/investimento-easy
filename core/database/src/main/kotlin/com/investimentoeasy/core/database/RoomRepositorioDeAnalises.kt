package com.investimentoeasy.core.database

import com.investimentoeasy.core.domain.analise.AnaliseGuardada
import com.investimentoeasy.core.domain.analise.RepositorioDeAnalises
import com.investimentoeasy.core.model.SnapshotId
import java.time.Instant
import javax.inject.Inject

internal class RoomRepositorioDeAnalises
    @Inject
    constructor(
        private val dao: AnaliseDao,
    ) : RepositorioDeAnalises {
        override suspend fun salvar(analise: AnaliseGuardada) {
            dao.inserir(
                AnaliseEntity(
                    snapshotId = analise.snapshotId.valor,
                    geradaEm = analise.geradaEm.toEpochMilli(),
                    modelo = analise.modelo,
                    versaoPrompt = analise.versaoPrompt,
                    conteudo = analise.conteudoJson,
                ),
            )
        }

        override suspend fun ultima(snapshotId: SnapshotId): AnaliseGuardada? =
            dao.ultima(snapshotId.valor)?.let {
                AnaliseGuardada(SnapshotId(it.snapshotId), Instant.ofEpochMilli(it.geradaEm), it.modelo, it.versaoPrompt, it.conteudo)
            }
    }
