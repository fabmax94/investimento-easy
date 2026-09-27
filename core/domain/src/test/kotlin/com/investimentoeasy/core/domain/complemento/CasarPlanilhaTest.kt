package com.investimentoeasy.core.domain.complemento

import com.investimentoeasy.core.domain.classificacao.ClassificadorAtivos
import com.investimentoeasy.core.model.ItemPlanilha
import com.investimentoeasy.core.model.Money
import com.investimentoeasy.core.model.PlanilhaPosicao
import com.investimentoeasy.core.model.Posicao
import com.investimentoeasy.core.model.ProventoPrevisto
import com.investimentoeasy.core.model.SecaoPlanilha
import com.investimentoeasy.core.model.doRelatorio
import com.investimentoeasy.core.testing.INSTANTE_FIXO
import com.investimentoeasy.core.testing.posicaoExtraida
import com.investimentoeasy.core.testing.snapshotConfirmado
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class CasarPlanilhaTest {
    private val classificador = ClassificadorAtivos()
    private val casar = CasarPlanilha(classificador)

    private fun posicao(
        nome: String,
        estrategia: String,
        quantidade: String? = null,
    ): Posicao {
        val extraida = posicaoExtraida(nome, estrategia, "1000.00", quantidade)
        return Posicao(classificador.classificar(extraida).ativo, extraida.saldo.doRelatorio(), extraida.quantidade?.doRelatorio())
    }

    private fun base(vararg posicoes: Posicao) = snapshotConfirmado("base", LocalDate.of(2026, 9, 3)).copy(posicoes = posicoes.toList())

    private fun item(
        nome: String,
        secao: SecaoPlanilha,
        subgrupo: String = "Pós-Fixado",
        quantidade: String? = null,
        taxa: String? = null,
    ) = ItemPlanilha(
        nome = nome,
        secao = secao,
        subgrupo = subgrupo,
        saldo = Money.of("900.00"),
        quantidade = quantidade?.let(::BigDecimal),
        precoMedio = Money.of("10.00"),
        valorAplicado = Money.of("800.00"),
        taxa = taxa,
    )

    private fun planilha(vararg itens: ItemPlanilha) =
        PlanilhaPosicao(
            dataPosicao = LocalDate.of(2026, 6, 21),
            patrimonio = null,
            itens = itens.toList(),
            proventos = listOf(ProventoPrevisto("KNCR11", "RENDIMENTO", BigDecimal("200"), Money.of("200.00"), null)),
            avisos = emptyList(),
        )

    @Test
    fun `ticker casa pela chave e sinaliza quantidade diferente`() {
        val bova = posicao("BOVA11", "Renda Variável Brasil", "20")
        val kncr = posicao("KNCR11", "Fundos Listados", "119")
        val casamento =
            casar(
                base(bova, kncr),
                planilha(
                    item("BOVA11", SecaoPlanilha.ACOES, "Renda Variável Brasil", "20"),
                    item("KNCR11", SecaoPlanilha.FUNDOS_IMOBILIARIOS, "Fundos Listados", "100"),
                ),
                INSTANTE_FIXO,
            )
        with(casamento.complemento) {
            de(bova.ativo.chave).shouldNotBeNull().quantidadeMudou shouldBe false
            de(kncr.ativo.chave).shouldNotBeNull().quantidadeMudou shouldBe true
            de(bova.ativo.chave)!!.precoMedio shouldBe Money.of("10.00")
            dataPlanilha shouldBe LocalDate.of(2026, 6, 21)
            recebidoEm shouldBe INSTANTE_FIXO
            proventos.map { it.ativo } shouldContainExactly listOf("KNCR11")
        }
        casamento.casadas shouldBe 2
    }

    @Test
    fun `credito casa remontando o nome com a taxa, como no PDF`() {
        val cdb = posicao("CDB BANCO EXEMPLO S.A. - MAR/2028 - 112,00% CDI", "Pós Fixado", "10")
        val casamento =
            casar(
                base(cdb),
                planilha(item("CDB BANCO EXEMPLO S.A. - MAR/2028", SecaoPlanilha.RENDA_FIXA, quantidade = "10", taxa = "112,00% CDI")),
                INSTANTE_FIXO,
            )
        casamento.complemento.de(cdb.ativo.chave).shouldNotBeNull().taxa shouldBe "112,00% CDI"
    }

    @Test
    fun `credito com outra taxa nao casa`() {
        val cdb = posicao("CDB BANCO EXEMPLO S.A. - MAR/2028 - 112,00% CDI", "Pós Fixado")
        val casamento =
            casar(
                base(cdb),
                planilha(item("CDB BANCO EXEMPLO S.A. - MAR/2028", SecaoPlanilha.RENDA_FIXA, taxa = "110,00% CDI")),
                INSTANTE_FIXO,
            )
        casamento.casadas shouldBe 0
    }

    @Test
    fun `fundos casam apesar das siglas diferentes no PDF e na planilha`() {
        val nasdaq = posicao("TREND NASDAQ 100 FIA", "Renda Variável Global")
        val tecnologia = posicao("Trend Tecnologia Americana CIA RL", "Renda Variável Global")
        val ouro = posicao("Trend Ouro FIF Multi RL", "Alternativo")
        val casamento =
            casar(
                base(nasdaq, tecnologia, ouro),
                planilha(
                    item("Trend Nasdaq 100 FIM RL", SecaoPlanilha.FUNDOS, "Renda Variável Global"),
                    item("Trend Tecnologia Americana FIM RL", SecaoPlanilha.FUNDOS, "Renda Variável Global"),
                    item("Trend Ouro FIF Multi RL", SecaoPlanilha.FUNDOS, "Alternativos"),
                ),
                INSTANTE_FIXO,
            )
        casamento.complemento.de(nasdaq.ativo.chave).shouldNotBeNull().nomeNaPlanilha shouldBe "Trend Nasdaq 100 FIM RL"
        casamento.complemento.de(tecnologia.ativo.chave).shouldNotBeNull().nomeNaPlanilha shouldBe "Trend Tecnologia Americana FIM RL"
        casamento.complemento.de(ouro.ativo.chave).shouldNotBeNull()
        casamento.soNaPlanilha shouldBe emptyList()
    }

    @Test
    fun `na duvida entre dois fundos, nao casa nenhum`() {
        val a = posicao("Fundo Exemplo FIA", "Renda Variável Brasil")
        val b = posicao("Fundo Exemplo FIM", "Multimercado")
        val casamento = casar(base(a, b), planilha(item("Fundo Exemplo RL", SecaoPlanilha.FUNDOS, "Multimercados")), INSTANTE_FIXO)
        casamento.casadas shouldBe 0
        casamento.soNaBase shouldContainExactlyInAnyOrder listOf(a, b)
    }

    @Test
    fun `fundos diferentes da mesma gestora nao casam`() {
        val nasdaq = posicao("TREND NASDAQ 100 FIA", "Renda Variável Global")
        val casamento = casar(base(nasdaq), planilha(item("Trend Ouro FIF Multi RL", SecaoPlanilha.FUNDOS, "Alternativos")), INSTANTE_FIXO)
        casamento.complemento.de(nasdaq.ativo.chave).shouldBeNull()
    }

    @Test
    fun `lista o que ficou so de um lado`() {
        val lftb = posicao("LFTB11", "Pós Fixado", "16")
        val vencido = item("CDB OUTRO BANCO S.A. - AGO/2026", SecaoPlanilha.RENDA_FIXA, taxa = "110,00% CDI")
        val casamento = casar(base(lftb), planilha(vencido), INSTANTE_FIXO)
        casamento.soNaPlanilha shouldContainExactly listOf(vencido)
        casamento.soNaBase shouldContainExactly listOf(lftb)
    }

    @Test
    fun `palavras ignoram siglas, acentos e caixa`() {
        CasarPlanilha.palavras("Sparta Debêntures Incentivadas FIC FIF-Infra RF RL") shouldBe
            setOf("SPARTA", "DEBENTURES", "INCENTIVADAS")
        CasarPlanilha.semelhanca(setOf("A", "B"), setOf("A", "B", "C", "D")) shouldBe 0.5
        CasarPlanilha.semelhanca(emptySet(), setOf("A")) shouldBe 0.0
    }
}
