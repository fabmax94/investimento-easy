package com.investimentoeasy.core.domain.snapshot

import com.investimentoeasy.core.domain.classificacao.Pendencia
import com.investimentoeasy.core.domain.validacao.Problema
import com.investimentoeasy.core.model.MetodoExtracao
import com.investimentoeasy.core.model.Money
import com.investimentoeasy.core.model.Origem
import com.investimentoeasy.core.model.SnapshotId
import com.investimentoeasy.core.model.StatusSnapshot
import com.investimentoeasy.core.testing.extracao
import com.investimentoeasy.core.testing.geradorSequencial
import com.investimentoeasy.core.testing.posicaoExtraida
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PrepararRevisaoTest {
    private val preparar = PrepararRevisao(geradorId = geradorSequencial())

    @Test
    fun `monta rascunho com posicoes classificadas e origem relatorio`() {
        val revisao = preparar(extracao(listOf(posicaoExtraida("HGLG11", "Fundos Listados", "500.00", "5"))))
        val rascunho = revisao.rascunho.shouldNotBeNull()
        rascunho.id shouldBe SnapshotId("s1")
        rascunho.status shouldBe StatusSnapshot.RASCUNHO
        rascunho.dataReferencia shouldBe LocalDate.of(2026, 9, 3)
        rascunho.patrimonioInformado!!.valor shouldBe Money.of("500.00")
        val posicao = rascunho.posicoes.single()
        posicao.saldo.origem shouldBe Origem.RELATORIO
        posicao.quantidade!!.origem shouldBe Origem.RELATORIO
        posicao.rentabilidadeMes!!.origem shouldBe Origem.RELATORIO
        revisao.validacao.problemas.isEmpty() shouldBe true
    }

    @Test
    fun `R2 - extracao por IA marca todos os campos como IA`() {
        val rascunho = preparar(extracao(metodo = MetodoExtracao.IA)).rascunho.shouldNotBeNull()
        rascunho.metodo shouldBe MetodoExtracao.IA
        rascunho.patrimonioInformado!!.origem shouldBe Origem.IA
        rascunho.posicoes.single().saldo.origem shouldBe Origem.IA
    }

    @Test
    fun `R1 - sem data no arquivo nao ha rascunho ate o usuario informar`() {
        val semData = extracao(dataReferencia = null)
        val revisao = preparar(semData)
        revisao.rascunho.shouldBeNull()
        revisao.validacao.problemas shouldContainExactly listOf(Problema.DataReferenciaAusente)

        val informada = preparar(semData, dataInformadaPeloUsuario = LocalDate.of(2026, 8, 31))
        informada.rascunho!!.dataReferencia shouldBe LocalDate.of(2026, 8, 31)
        informada.validacao.impeditivo shouldBe false
    }

    @Test
    fun `pendencias de classificacao chegam a revisao`() {
        val revisao = preparar(extracao(listOf(posicaoExtraida("XYZW11", "Fundos Listados"))))
        revisao.classificacao.single().pendencias shouldBe setOf(Pendencia.SEGMENTO_FII_DESCONHECIDO)
    }

    @Test
    fun `quantidade e rentabilidade ausentes continuam ausentes`() {
        val revisao = preparar(extracao(listOf(posicaoExtraida(quantidade = null, rentabilidadeMes = null))))
        val posicao = revisao.rascunho!!.posicoes.single()
        posicao.quantidade.shouldBeNull()
        posicao.rentabilidadeMes.shouldBeNull()
    }
}
