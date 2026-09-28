package com.investimentoeasy.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
internal interface MercadoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun gravar(mercado: MercadoEntity)

    @Query("SELECT * FROM mercado WHERE unica = 1")
    suspend fun ultimo(): MercadoEntity?
}
