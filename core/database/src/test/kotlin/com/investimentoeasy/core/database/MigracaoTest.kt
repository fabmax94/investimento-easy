package com.investimentoeasy.core.database

import android.database.sqlite.SQLiteException
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.investimentoeasy.core.model.Money
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

/** Quem já tem carteira gravada na versão 1 não pode perder nada ao atualizar o app. */
@RunWith(RobolectricTestRunner::class)
class MigracaoTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), InvestimentoDatabase::class.java)

    private val nome = "migracao-teste.db"

    @Test
    fun `v1 ate a versao atual preserva snapshots, posicoes, base e a imutabilidade`() =
        runTest {
            helper.createDatabase(nome, 1).use { v1 ->
                InvestimentoDatabase.Imutabilidade.onCreate(v1)
                v1.execSQL(
                    "INSERT INTO snapshot (id, dataReferencia, patrimonio, patrimonioOrigem, metodo, versao, confirmadoEm, " +
                        "divergenciaAceita) " +
                        "VALUES ('s1', '2026-09-03', '150250.00', 'RELATORIO', 'PARSER', 1, 1790000000000, 0)",
                )
                v1.execSQL(
                    "INSERT INTO posicao (snapshotId, ordem, chave, nome, tipo, classe, gestora, emissor, indexador, taxa, vencimento, " +
                        "saldo, saldoOrigem, quantidade, quantidadeOrigem, rentabilidadeMes, rentabilidadeMesOrigem, rentabilidadeAno, " +
                        "rentabilidadeAnoOrigem, rentabilidade24Meses, rentabilidade24MesesOrigem) VALUES ('s1', 0, 'TICKER:HGLG11', " +
                        "'HGLG11', 'FII', 'FII_TIJOLO', NULL, NULL, NULL, NULL, NULL, '16000.00', 'RELATORIO', '100', 'RELATORIO', " +
                        "'0.9', 'RELATORIO', '-0.36', 'RELATORIO', '7.85', 'RELATORIO')",
                )
                v1.execSQL("INSERT INTO base (unica, snapshotId) VALUES (1, 's1')")
            }

            helper.runMigrationsAndValidate(nome, 5, true, *InvestimentoDatabase.MIGRACOES).use { v2 ->
                shouldThrow<SQLiteException> { v2.execSQL("UPDATE snapshot SET versao = 2 WHERE id = 's1'") }
                shouldThrow<SQLiteException> { v2.execSQL("DELETE FROM posicao WHERE snapshotId = 's1'") }
            }

            val db =
                Room
                    .databaseBuilder(ApplicationProvider.getApplicationContext(), InvestimentoDatabase::class.java, nome)
                    .addMigrations(*InvestimentoDatabase.MIGRACOES)
                    .allowMainThreadQueries()
                    .build()
            try {
                val base = RoomSnapshotRepository(db.snapshotDao()).base()!!
                base.dataReferencia shouldBe LocalDate.of(2026, 9, 3)
                base.patrimonioInformado!!.valor shouldBe Money.of("150250.00")
                base.posicoes.single().ativo.nome shouldBe "HGLG11"
                base.posicoes.single().percentualCdiAno.shouldBeNull()
                base.contexto.shouldBeNull()
            } finally {
                db.close()
            }
        }
}
