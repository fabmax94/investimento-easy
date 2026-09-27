package com.investimentoeasy.feature.portfolio

import com.investimentoeasy.core.domain.alocacao.GrupoAlocacao
import com.investimentoeasy.core.model.Money
import com.investimentoeasy.core.model.Origem
import com.investimentoeasy.core.testing.FakeSnapshotRepository
import com.investimentoeasy.core.testing.relogioFixo
import com.investimentoeasy.core.testing.snapshotConfirmado
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class CarteiraViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    /** O relógio fixo dos testes está em 13/09/2026. */
    private fun estado(
        dataBase: LocalDate?,
        agora: Instant = Instant.parse("2026-09-13T12:00:00Z"),
    ): EstadoCarteira {
        val repo =
            if (dataBase == null) {
                FakeSnapshotRepository()
            } else {
                val base = snapshotConfirmado("b", dataBase)
                FakeSnapshotRepository(listOf(base), base.id)
            }
        return CarteiraViewModel(repo, relogioFixo(agora), dispatcher).apply { carregar() }.estado.value
    }

    @Test
    fun `sem base mostra estado vazio`() {
        estado(null) shouldBe EstadoCarteira.SemCarteira
    }

    @Test
    fun `com base mostra patrimonio do relatorio e alocacao`() {
        val base = estado(LocalDate.of(2026, 9, 3)).shouldBeInstanceOf<EstadoCarteira.ComBase>()
        base.diasDesdeReferencia shouldBe 10
        base.atualidade shouldBe Atualidade.EM_DIA
        base.patrimonio shouldBe Money.of("1000.00")
        base.origemPatrimonio shouldBe Origem.RELATORIO
        base.alocacao.single().grupo shouldBe GrupoAlocacao.ACOES_BRASIL
        textoAtualidade(base) shouldBe "Há 10 dias · sugerimos atualizar a partir de 30 dias"
    }

    @Test
    fun `apos 30 dias sugere upload e apos 45 marca desatualizada`() {
        estado(LocalDate.of(2026, 8, 14)).shouldBeInstanceOf<EstadoCarteira.ComBase>().atualidade shouldBe Atualidade.EM_DIA
        estado(LocalDate.of(2026, 8, 13)).shouldBeInstanceOf<EstadoCarteira.ComBase>().atualidade shouldBe Atualidade.SUGERIR_UPLOAD
        estado(LocalDate.of(2026, 7, 30)).shouldBeInstanceOf<EstadoCarteira.ComBase>().atualidade shouldBe Atualidade.SUGERIR_UPLOAD
        val velha = estado(LocalDate.of(2026, 7, 29)).shouldBeInstanceOf<EstadoCarteira.ComBase>()
        velha.atualidade shouldBe Atualidade.DESATUALIZADA
        textoAtualidade(velha) shouldBe "Há 46 dias · hora de enviar um relatório novo"
    }

    @Test
    fun `datas no mesmo dia e no dia seguinte`() {
        textoAtualidade(estado(LocalDate.of(2026, 9, 13)) as EstadoCarteira.ComBase) shouldBe
            "Posição de hoje · sugerimos atualizar a partir de 30 dias"
        textoAtualidade(estado(LocalDate.of(2026, 9, 12)) as EstadoCarteira.ComBase) shouldBe
            "Há 1 dia · sugerimos atualizar a partir de 30 dias"
    }
}
