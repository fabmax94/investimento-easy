package com.investimentoeasy.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

/**
 * R16: só há inserção (que falha se o id já existir) e leitura. Não há update nem delete;
 * o banco ainda tem triggers que abortam qualquer tentativa (ver [InvestimentoDatabase]).
 */
@Dao
internal interface SnapshotDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun inserirSnapshot(snapshot: SnapshotEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun inserirPosicoes(posicoes: List<PosicaoEntity>)

    @Transaction
    suspend fun inserir(
        snapshot: SnapshotEntity,
        posicoes: List<PosicaoEntity>,
    ) {
        inserirSnapshot(snapshot)
        inserirPosicoes(posicoes)
    }

    @Query("SELECT * FROM snapshot ORDER BY dataReferencia, versao")
    suspend fun snapshots(): List<SnapshotEntity>

    @Query("SELECT * FROM snapshot WHERE id = :id")
    suspend fun snapshot(id: String): SnapshotEntity?

    @Query("SELECT * FROM posicao WHERE snapshotId = :snapshotId ORDER BY ordem")
    suspend fun posicoes(snapshotId: String): List<PosicaoEntity>

    @Query("SELECT snapshotId FROM base WHERE unica = 1")
    suspend fun idDaBase(): String?

    @Upsert
    suspend fun definirBase(base: BaseEntity)
}
