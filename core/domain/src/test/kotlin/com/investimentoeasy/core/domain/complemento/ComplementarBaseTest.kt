package com.investimentoeasy.core.domain.complemento

import com.investimentoeasy.core.model.ItemPlanilha
import com.investimentoeasy.core.model.Money
import com.investimentoeasy.core.model.PlanilhaPosicao
import com.investimentoeasy.core.model.SecaoPlanilha
import com.investimentoeasy.core.testing.FakeRepositorioDeComplementos
import com.investimentoeasy.core.testing.FakeSnapshotRepository
import com.investimentoeasy.core.testing.INSTANTE_FIXO
import com.investimentoeasy.core.testing.relogioFixo
import com.investimentoeasy.core.testing.snapshotConfirmado
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class ComplementarBaseTest {
    private val planilha =
        PlanilhaPosicao(
            dataPosicao = LocalDate.of(2026, 6, 21),
            patrimonio = null,
            itens = listOf(ItemPlanilha("BOVA11", SecaoPlanilha.ACOES, "Renda Variável Brasil", Money.of("900.00"), BigDecimal("10"))),
            proventos = emptyList(),
            avisos = emptyList(),
        )

    @Test
    fun `sem base nao ha o que complementar`() =
        runTest {
            val complementar = ComplementarBase(FakeSnapshotRepository(), FakeRepositorioDeComplementos(), relogioFixo())
            complementar.preparar(planilha) shouldBe PreparoComplemento.SemBase
        }

    @Test
    fun `casa com a base atual e so guarda quando pedido`() =
        runTest {
            val base = snapshotConfirmado("base", LocalDate.of(2026, 9, 3))
            val complementos = FakeRepositorioDeComplementos()
            val complementar = ComplementarBase(FakeSnapshotRepository(listOf(base), base.id), complementos, relogioFixo())

            val pronto = complementar.preparar(planilha).shouldBeInstanceOf<PreparoComplemento.Pronto>()
            pronto.base shouldBe base
            pronto.casamento.casadas shouldBe 1
            complementos.salvos.shouldBeEmpty()

            complementar.guardar(pronto.casamento)
            complementos.salvos shouldContainExactly listOf(pronto.casamento.complemento)
            complementos.ultimo(base.id)!!.recebidoEm shouldBe INSTANTE_FIXO
        }
}
