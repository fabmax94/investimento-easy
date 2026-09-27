package com.investimentoeasy.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SnapshotEntity::class, PosicaoEntity::class, BaseEntity::class, AnaliseEntity::class, ComplementoEntity::class],
    version = 4,
    exportSchema = true,
)
internal abstract class InvestimentoDatabase : RoomDatabase() {
    abstract fun snapshotDao(): SnapshotDao

    abstract fun analiseDao(): AnaliseDao

    abstract fun complementoDao(): ComplementoDao

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

        /** v3: análises da Camada 3 guardadas por snapshot. */
        val MIGRACAO_2_3: Migration =
            object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `analise` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`snapshotId` TEXT NOT NULL, `geradaEm` INTEGER NOT NULL, `modelo` TEXT NOT NULL, " +
                            "`versaoPrompt` TEXT NOT NULL, `conteudo` TEXT NOT NULL, " +
                            "FOREIGN KEY(`snapshotId`) REFERENCES `snapshot`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION )",
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_analise_snapshotId` ON `analise` (`snapshotId`)")
                }
            }

        /** v4: dados da planilha "Posição Detalhada" guardados ao lado do snapshot. */
        val MIGRACAO_3_4: Migration =
            object : Migration(3, 4) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `complemento` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`snapshotId` TEXT NOT NULL, `dataPlanilha` TEXT, `recebidoEm` INTEGER NOT NULL, `conteudo` TEXT NOT NULL, " +
                            "FOREIGN KEY(`snapshotId`) REFERENCES `snapshot`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION )",
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_complemento_snapshotId` ON `complemento` (`snapshotId`)")
                }
            }

        val MIGRACOES: Array<Migration> = arrayOf(MIGRACAO_1_2, MIGRACAO_2_3, MIGRACAO_3_4)
    }
}
