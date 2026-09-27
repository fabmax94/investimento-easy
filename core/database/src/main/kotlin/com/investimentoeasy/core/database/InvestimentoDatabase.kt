package com.investimentoeasy.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SnapshotEntity::class, PosicaoEntity::class, BaseEntity::class],
    version = 2,
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

        /** v2: %CDI por posição e o contexto do relatório (índices, série e evolução mensal). */
        val MIGRACAO_1_2: Migration =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    listOf("percentualCdiMes", "percentualCdiAno", "percentualCdi24Meses").forEach { coluna ->
                        db.execSQL("ALTER TABLE posicao ADD COLUMN $coluna TEXT")
                        db.execSQL("ALTER TABLE posicao ADD COLUMN ${coluna}Origem TEXT")
                    }
                    db.execSQL("ALTER TABLE snapshot ADD COLUMN contexto TEXT")
                }
            }

        val MIGRACOES: Array<Migration> = arrayOf(MIGRACAO_1_2)
    }
}
