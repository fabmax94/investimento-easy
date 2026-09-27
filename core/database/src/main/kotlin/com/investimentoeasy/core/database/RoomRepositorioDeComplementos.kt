package com.investimentoeasy.core.database

import com.investimentoeasy.core.domain.complemento.ComplementoPlanilha
import com.investimentoeasy.core.domain.complemento.RepositorioDeComplementos
import com.investimentoeasy.core.model.SnapshotId
import javax.inject.Inject

internal class RoomRepositorioDeComplementos
    @Inject
    constructor(
        private val dao: ComplementoDao,
    ) : RepositorioDeComplementos {
        override suspend fun salvar(complemento: ComplementoPlanilha) {
            dao.inserir(
                ComplementoEntity(
                    snapshotId = complemento.snapshotId.valor,
                    dataPlanilha = complemento.dataPlanilha?.toString(),
                    recebidoEm = complemento.recebidoEm.toEpochMilli(),
                    conteudo = ComplementoJson.de(complemento).texto(),
                ),
            )
        }

        override suspend fun ultimo(snapshotId: SnapshotId): ComplementoPlanilha? =
            dao.ultimo(snapshotId.valor)?.let { ComplementoJson.de(it.conteudo).paraModelo(it.snapshotId, it.dataPlanilha, it.recebidoEm) }
    }
