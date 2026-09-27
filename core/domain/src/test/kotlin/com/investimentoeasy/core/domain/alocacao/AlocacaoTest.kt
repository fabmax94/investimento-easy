package com.investimentoeasy.core.domain.alocacao

import com.investimentoeasy.core.model.ClasseAtivo
import com.investimentoeasy.core.model.Money
import com.investimentoeasy.core.model.Percent
import com.investimentoeasy.core.model.TipoAtivo
import com.investimentoeasy.core.model.soma
import com.investimentoeasy.core.testing.ativo
import com.investimentoeasy.core.testing.posicao
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.pair
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.RoundingMode

class AlocacaoTest {
    @Test
    fun `agrupa por grupo na ordem de exibicao e soma FIIs de todos os segmentos`() {
        val fatias =
            alocacaoPorGrupo(
                listOf(
                    posicao(ativo("HGLG11", ClasseAtivo.FII_TIJOLO, TipoAtivo.FII), saldo = "600.00"),
                    posicao(ativo("LFTB11", ClasseAtivo.RF_POS), saldo = "250.00"),
                    posicao(ativo("KNCR11", ClasseAtivo.FII_PAPEL, TipoAtivo.FII), saldo = "150.00"),
                    posicao(ativo("RECR12", ClasseAtivo.FII_PAPEL, TipoAtivo.FII), saldo = "0.00"),
                ),
            )
        fatias.map { Triple(it.grupo, it.quantidadeAtivos, it.valor) } shouldBe
            listOf(
                Triple(GrupoAlocacao.RENDA_FIXA_POS, 1, Money.of("250.00")),
                Triple(GrupoAlocacao.FIIS, 3, Money.of("750.00")),
            )
        fatias.map { it.percentual } shouldBe listOf(Percent.of("25"), Percent.of("75"))
    }

    @Test
    fun `carteira vazia ou zerada nao inventa percentual`() {
        alocacaoPorGrupo(emptyList()).shouldBeEmpty()
        alocacaoPorGrupo(listOf(posicao(saldo = "0.00"))).single().percentual.shouldBeNull()
    }

    @Test
    fun `toda classe pertence a exatamente um grupo`() {
        ClasseAtivo.entries.forEach { classe ->
            GrupoAlocacao.entries.count { classe in it.classes } shouldBe 1
        }
    }

    @Test
    fun `propriedade - valores somam o total e percentuais somam 100`() =
        runTest {
            checkAll(Arb.list(Arb.pair(Arb.enum<ClasseAtivo>(), Arb.long(1L..10_000_000_00L)), 1..40)) { itens ->
                val posicoes =
                    itens.mapIndexed { i, (classe, centavos) ->
                        posicao(ativo("T${i}X11", classe), BigDecimal.valueOf(centavos, 2).toPlainString())
                    }
                val fatias = alocacaoPorGrupo(posicoes)
                fatias.map { it.valor }.soma() shouldBe posicoes.map { it.saldo.valor }.soma()
                fatias.sumOf { it.quantidadeAtivos } shouldBe posicoes.size
                val somaPercentual = fatias.sumOf { it.percentual!!.pontos }.setScale(4, RoundingMode.HALF_EVEN)
                ((somaPercentual - BigDecimal(100)).abs() <= BigDecimal("0.0001")) shouldBe true
            }
        }
}
