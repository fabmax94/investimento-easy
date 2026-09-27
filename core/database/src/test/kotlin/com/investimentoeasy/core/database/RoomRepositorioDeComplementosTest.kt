package com.investimentoeasy.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.investimentoeasy.core.domain.complemento.ComplementoPlanilha
import com.investimentoeasy.core.domain.complemento.DadosDaPlanilha
import com.investimentoeasy.core.model.ChaveAtivo
import com.investimentoeasy.core.model.Indexador
import com.investimentoeasy.core.model.Money
import com.investimentoeasy.core.model.Percent
import com.investimentoeasy.core.model.ProventoPrevisto
import com.investimentoeasy.core.model.SnapshotId
import com.investimentoeasy.core.model.TipoAtivo
import com.investimentoeasy.core.testing.snapshotConfirmado
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
class RoomRepositorioDeComplementosTest {
    private val db =
        Room
            .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), InvestimentoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    private val repo = RoomRepositorioDeComplementos(db.complementoDao())

    @After
    fun fechar() = db.close()

    private val completo =
        DadosDaPlanilha(
            chave =
                ChaveAtivo.CreditoPrivado(
                    TipoAtivo.CDB,
                    "BANCO EXEMPLO S.A.",
                    Indexador.CDI,
                    Percent.of("112.00"),
                    YearMonth.of(2028, 3),
                ),
            nomeNaPlanilha = "CDB BANCO EXEMPLO S.A. - MAR/2028",
            saldoNaPlanilha = Money.of("9900.00"),
            quantidadeNaPlanilha = BigDecimal("10"),
            valorAplicado = Money.of("8000.00"),
            precoMedio = null,
            rentabilidadeDesdeInicio = Percent.of("23.75"),
            valorLiquido = Money.of("9615.00"),
            ir = Money.of("285.00"),
            iof = Money.ZERO,
            taxa = "112,00% CDI",
            dataAplicacao = LocalDate.of(2023, 3, 10),
            vencimento = LocalDate.of(2028, 3, 10),
            quantidadeMudou = true,
        )

    private val minimo =
        DadosDaPlanilha(
            ChaveAtivo.FundoPorNome("TREND NASDAQ 100 FIA"), "Trend Nasdaq 100 FIM RL", Money.of("38000.00"),
            null, null, null, null, null, null, null, null, null, null, false,
        )

    @Test
    fun `guarda o complemento sem perder nenhum campo e devolve o mais recente`() =
        runTest {
            RoomSnapshotRepository(db.snapshotDao()).inserirConfirmado(snapshotConfirmado("s1", LocalDate.of(2026, 9, 3)))
            repo.ultimo(SnapshotId("s1")).shouldBeNull()
            val antigo =
                ComplementoPlanilha(
                    SnapshotId("s1"),
                    LocalDate.of(2026, 6, 21),
                    Instant.parse("2026-09-13T10:00:00Z"),
                    listOf(completo, minimo),
                    listOf(ProventoPrevisto("KNCR11", "RENDIMENTO", BigDecimal("200"), Money.of("200.00"), LocalDate.of(2026, 6, 25))),
                )
            val novo = antigo.copy(recebidoEm = Instant.parse("2026-09-14T10:00:00Z"), dataPlanilha = null, proventos = emptyList())
            repo.salvar(novo)
            repo.salvar(antigo)
            repo.ultimo(SnapshotId("s1")) shouldBe novo
            repo.ultimo(SnapshotId("outro")).shouldBeNull()
        }

    @Test
    fun `ida e volta do JSON preserva tudo`() {
        val complemento =
            ComplementoPlanilha(
                SnapshotId("s1"),
                LocalDate.of(2026, 6, 21),
                Instant.parse("2026-09-13T10:00:00Z"),
                listOf(completo, minimo),
                listOf(ProventoPrevisto("ITSA4", "DIVIDENDO", null, Money.of("10.00"), null)),
            )
        val json = ComplementoJson.de(complemento).texto()
        ComplementoJson.de(json).paraModelo("s1", "2026-06-21", complemento.recebidoEm.toEpochMilli()) shouldBe complemento
    }
}
