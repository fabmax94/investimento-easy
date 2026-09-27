package com.investimentoeasy.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SnapshotEntity::class, PosicaoEntity::class, BaseEntity::class],
    version = 1,
    exportSchema = true,
)
internal abstract class InvestimentoDatabase : RoomDatabase() {
    abstract fun snapshotDao(): SnapshotDao

    /**
     * R16 no próprio banco: snapshots e posições confirmados não podem ser alterados nem
     * apagados, mesmo por código que contorne o DAO.
     */
    object Imutabilidade : Callback() {
        private val TABELAS = listOf("snapshot", "posicao")

        override fun onCreate(db: SupportSQLiteDatabase) {
            TABELAS.forEach { tabela ->
                listOf("UPDATE", "DELETE").forEach { operacao ->
                    db.execSQL(
                        "CREATE TRIGGER IF NOT EXISTS ${tabela}_sem_${operacao.lowercase()} BEFORE $operacao ON $tabela " +
                            "BEGIN SELECT RAISE(ABORT, 'R16: $tabela confirmado é imutável'); END",
                    )
                }
            }
        }
    }

    companion object {
        const val NOME = "investimento.db"
    }
}
