package com.investimentoeasy.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
internal interface AnaliseDao {
    @Insert
    suspend fun inserir(analise: AnaliseEntity)

    @Query("SELECT * FROM analise WHERE snapshotId = :snapshotId ORDER BY geradaEm DESC, id DESC LIMIT 1")
    suspend fun ultima(snapshotId: String): AnaliseEntity?
}
