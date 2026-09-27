package com.investimentoeasy.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
internal interface ComplementoDao {
    @Insert
    suspend fun inserir(complemento: ComplementoEntity)

    @Query("SELECT * FROM complemento WHERE snapshotId = :snapshotId ORDER BY recebidoEm DESC, id DESC LIMIT 1")
    suspend fun ultimo(snapshotId: String): ComplementoEntity?
}
