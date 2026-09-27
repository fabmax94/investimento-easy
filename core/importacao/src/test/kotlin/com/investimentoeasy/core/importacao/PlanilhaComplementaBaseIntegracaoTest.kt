package com.investimentoeasy.core.importacao

import com.investimentoeasy.core.domain.complemento.CasarPlanilha
import com.investimentoeasy.core.model.Money
import com.investimentoeasy.core.model.StatusSnapshot
import com.investimentoeasy.core.testing.INSTANTE_FIXO
import com.investimentoeasy.parser.xlsx.FixturesPlanilhaXp
import com.investimentoeasy.parser.xperformance.FixturesXPerformance
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

/** PDF sintético vira a base; a planilha sintética (mais antiga, com nomes diferentes) a complementa. */
class PlanilhaComplementaBaseIntegracaoTest {
    private val importar = ImportarRelatorio(ExtratorDeTextoPdf { FixturesXPerformance.sintetico() })

    private val base =
        importar
            .importar(ArquivoRecebido("x.pdf", null, "%PDF".toByteArray()))
            .shouldBeInstanceOf<ResultadoImportacao.Lido>()
            .revisao.rascunho!!
            .copy(status = StatusSnapshot.CONFIRMADO)

    private val planilha =
        importar
            .importar(ArquivoRecebido("PosicaoDetalhada.xlsx", null, FixturesPlanilhaXp.bytes()))
            .shouldBeInstanceOf<ResultadoImportacao.PlanilhaLida>()
            .planilha

    private val casamento = CasarPlanilha()(base, planilha, INSTANTE_FIXO)

    @Test
    fun `casa tudo o que existe nos dois arquivos, inclusive fundo com sigla diferente e CDB sem taxa no nome`() {
        casamento.casadas shouldBe 11
        casamento.complemento.dados.map { it.nomeNaPlanilha } shouldContainExactlyInAnyOrder
            listOf(
                "Trend Nasdaq 100 FIM RL", "Trend Ouro FIF Multi RL", "Fundo Exemplo Debêntures Incentivadas FIC FIF-Infra RF RL",
                "BOVA11", "ITSA4", "IVVB11", "CDB BANCO EXEMPLO S.A. - MAR/2028", "HGLG11", "KNCR11", "XYZW11", "RECR12",
            )
    }

    @Test
    fun `o que so existe de um lado fica visivel`() {
        casamento.soNaPlanilha.map { it.nome } shouldContainExactly listOf("CDB OUTRO BANCO S.A. - AGO/2026")
        casamento.soNaBase.map { it.ativo.nome } shouldContainExactlyInAnyOrder listOf("LFTB11", "IMAB11")
    }

    @Test
    fun `dados da planilha ficam ligados a chave da posicao da base`() {
        val nasdaq = base.posicoes.single { it.ativo.nome == "Trend Nasdaq 100 FIA" }
        casamento.complemento.de(nasdaq.ativo.chave).shouldNotBeNull().valorAplicado shouldBe Money.of("27000.00")
        val bova = base.posicoes.single { it.ativo.nome == "BOVA11" }
        with(casamento.complemento.de(bova.ativo.chave).shouldNotBeNull()) {
            precoMedio shouldBe Money.of("151.23")
            quantidadeMudou shouldBe false
        }
        casamento.complemento.proventos.size shouldBe 2
    }
}
