package com.investimentoeasy.core.database

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.investimentoeasy.core.domain.snapshot.ConfirmarSnapshot
import com.investimentoeasy.core.domain.snapshot.DecisaoMesmaData
import com.investimentoeasy.core.domain.snapshot.PrepararRevisao
import com.investimentoeasy.core.domain.snapshot.ResultadoConfirmacao
import com.investimentoeasy.core.model.Money
import com.investimentoeasy.core.model.Percent
import com.investimentoeasy.core.model.SnapshotId
import com.investimentoeasy.core.model.StatusSnapshot
import com.investimentoeasy.core.testing.extracao
import com.investimentoeasy.core.testing.geradorSequencial
import com.investimentoeasy.core.testing.posicaoExtraida
import com.investimentoeasy.core.testing.relogioFixo
import com.investimentoeasy.core.testing.snapshotConfirmado
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class RoomSnapshotRepositoryTest {
    private val db =
        Room
            .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), InvestimentoDatabase::class.java)
            .addCallback(InvestimentoDatabase.Imutabilidade)
            .allowMainThreadQueries()
            .build()
    private val repo = RoomSnapshotRepository(db.snapshotDao())

    @After
    fun fechar() = db.close()

    @Test
    fun `grava e le um snapshot completo sem perder precisao nem origem`() =
        runTest {
            val revisao =
                PrepararRevisao(geradorId = geradorSequencial())(
                    extracao(
                        listOf(
                            posicaoExtraida("CDB LOJAS EXEMPLO - FINANCEIRA S.A. - DEZ/2027 - 118,00% CDI", "Pós Fixado", "10000.01", "10"),
                            posicaoExtraida(
                                "Trend Multiestratégia FIF Multi",
                                "Alternativo",
                                "2500.99",
                                "1234.567891",
                                rentabilidadeMes = null,
                            ),
                            posicaoExtraida("HGLG11", "Fundos Listados", "1600.00", quantidade = null),
                        ),
                    ),
                )
            val confirmado =
                ConfirmarSnapshot(repo, relogioFixo())(revisao).shouldBeInstanceOf<ResultadoConfirmacao.Confirmado>().snapshot

            repo.confirmados() shouldBe listOf(confirmado)
            repo.base() shouldBe confirmado
            confirmado.patrimonioInformado!!.valor shouldBe Money.of("14101.00")
        }

    @Test
    fun `contexto do relatorio e percentuais do CDI sobrevivem a gravacao`() =
        runTest {
            val revisao = PrepararRevisao(geradorId = geradorSequencial())(ExtracaoSintetica.completa())
            val confirmado =
                ConfirmarSnapshot(repo, relogioFixo())(revisao).shouldBeInstanceOf<ResultadoConfirmacao.Confirmado>().snapshot
            val lido = repo.base()!!
            lido shouldBe confirmado
            lido.contexto!!.referencia("CDI")!!.ano shouldBe Percent.of("9.50")
            lido.contexto!!.evolucaoMensal.single().movimentacoes shouldBe Money.of("-5000.00")
            lido.posicoes.single().percentualCdiAno!!.valor shouldBe Percent.of("152.35")
        }

    @Test
    fun `sem base definida retorna nulo`() =
        runTest {
            repo.base().shouldBeNull()
            repo.confirmados() shouldHaveSize 0
        }

    @Test
    fun `R15 e R16 - fluxo de confirmacao completo sobre o banco`() =
        runTest {
            val confirmar = ConfirmarSnapshot(repo, relogioFixo())
            val preparar = PrepararRevisao(geradorId = geradorSequencial())
            val setembro = preparar(extracao(dataReferencia = LocalDate.of(2026, 9, 3)))
            val agosto = preparar(extracao(dataReferencia = LocalDate.of(2026, 8, 1)))
            val setembroDeNovo = preparar(extracao(dataReferencia = LocalDate.of(2026, 9, 3)))

            confirmar(setembro).shouldBeInstanceOf<ResultadoConfirmacao.Confirmado>().virouBase shouldBe true
            confirmar(agosto).shouldBeInstanceOf<ResultadoConfirmacao.Confirmado>().virouBase shouldBe false
            confirmar(setembroDeNovo).shouldBeInstanceOf<ResultadoConfirmacao.PrecisaDecidirMesmaData>()
            val v2 = confirmar(setembroDeNovo, decisaoMesmaData = DecisaoMesmaData.SUBSTITUIR)
            v2.shouldBeInstanceOf<ResultadoConfirmacao.Confirmado>().snapshot.versao shouldBe 2

            repo.base()!!.id shouldBe SnapshotId("s3")
            repo.confirmados().map { it.id.valor to it.versao } shouldBe listOf("s2" to 1, "s1" to 1, "s3" to 2)
            repo.confirmados().all { it.status == StatusSnapshot.CONFIRMADO } shouldBe true
        }

    @Test
    fun `R16 - reinserir o mesmo id falha`() =
        runTest {
            val s = snapshotConfirmado("a", LocalDate.of(2026, 9, 3))
            repo.inserirConfirmado(s)
            shouldThrow<SQLiteConstraintException> { repo.inserirConfirmado(s) }
        }

    @Test
    fun `R16 - o banco recusa update e delete mesmo fora do DAO`() =
        runTest {
            repo.inserirConfirmado(snapshotConfirmado("a", LocalDate.of(2026, 9, 3)))
            val sql = db.openHelper.writableDatabase
            shouldThrow<SQLiteException> { sql.execSQL("UPDATE snapshot SET versao = 9 WHERE id = 'a'") }
            shouldThrow<SQLiteException> { sql.execSQL("DELETE FROM snapshot WHERE id = 'a'") }
            shouldThrow<SQLiteException> { sql.execSQL("UPDATE posicao SET saldo = '0' WHERE snapshotId = 'a'") }
            shouldThrow<SQLiteException> { sql.execSQL("DELETE FROM posicao WHERE snapshotId = 'a'") }
            repo.confirmados().single().versao shouldBe 1
        }

    @Test
    fun `base so aponta para snapshot existente e pode mudar`() =
        runTest {
            shouldThrow<IllegalStateException> { repo.definirBase(SnapshotId("inexistente")) }
            repo.inserirConfirmado(snapshotConfirmado("a", LocalDate.of(2026, 8, 1)))
            repo.inserirConfirmado(snapshotConfirmado("b", LocalDate.of(2026, 9, 1)))
            repo.definirBase(SnapshotId("a"))
            repo.definirBase(SnapshotId("b"))
            repo.base()!!.id shouldBe SnapshotId("b")
        }

    @Test
    fun `rascunho nao e persistido`() =
        runTest {
            val rascunho = PrepararRevisao()(extracao()).rascunho!!
            shouldThrow<IllegalStateException> { repo.inserirConfirmado(rascunho) }
        }
}
