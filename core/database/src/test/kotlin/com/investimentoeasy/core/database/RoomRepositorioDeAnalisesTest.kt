package com.investimentoeasy.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.investimentoeasy.core.domain.analise.AnaliseGuardada
import com.investimentoeasy.core.model.SnapshotId
import com.investimentoeasy.core.testing.snapshotConfirmado
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class RoomRepositorioDeAnalisesTest {
    private val db =
        Room
            .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), InvestimentoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    private val repo = RoomRepositorioDeAnalises(db.analiseDao())

    @After
    fun fechar() = db.close()

    @Test
    fun `guarda varias analises por snapshot e devolve a mais recente`() =
        runTest {
            RoomSnapshotRepository(db.snapshotDao()).inserirConfirmado(snapshotConfirmado("s1", LocalDate.of(2026, 9, 3)))
            repo.ultima(SnapshotId("s1")).shouldBeNull()
            val antiga =
                AnaliseGuardada(SnapshotId("s1"), Instant.parse("2026-09-13T10:00:00Z"), "claude-opus-5", "analise-v1", "{\"a\":1}")
            val nova = antiga.copy(geradaEm = Instant.parse("2026-09-14T10:00:00Z"), conteudoJson = "{\"a\":2}")
            repo.salvar(nova)
            repo.salvar(antiga)
            repo.ultima(SnapshotId("s1")) shouldBe nova
            repo.ultima(SnapshotId("outro")).shouldBeNull()
        }
}
