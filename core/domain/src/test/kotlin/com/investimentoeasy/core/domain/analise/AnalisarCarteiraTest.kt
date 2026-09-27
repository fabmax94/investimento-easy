package com.investimentoeasy.core.domain.analise

import com.investimentoeasy.core.model.Ativo
import com.investimentoeasy.core.model.ChaveAtivo
import com.investimentoeasy.core.model.ClasseAtivo
import com.investimentoeasy.core.model.ContextoRelatorio
import com.investimentoeasy.core.model.EvolucaoMensal
import com.investimentoeasy.core.model.Indexador
import com.investimentoeasy.core.model.IndiceReferencia
import com.investimentoeasy.core.model.MetodoExtracao
import com.investimentoeasy.core.model.Money
import com.investimentoeasy.core.model.Percent
import com.investimentoeasy.core.model.Posicao
import com.investimentoeasy.core.model.Snapshot
import com.investimentoeasy.core.model.SnapshotId
import com.investimentoeasy.core.model.StatusSnapshot
import com.investimentoeasy.core.model.TipoAtivo
import com.investimentoeasy.core.model.doRelatorio
import com.investimentoeasy.core.testing.ativo
import com.investimentoeasy.core.testing.posicao
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class AnalisarCarteiraTest {
    private val data = LocalDate.of(2026, 9, 3)
    private val analisar = AnalisarCarteira()

    private fun snapshot(
        posicoes: List<Posicao>,
        contexto: ContextoRelatorio? = null,
    ) = Snapshot(SnapshotId("s"), data, null, posicoes, MetodoExtracao.PARSER, StatusSnapshot.CONFIRMADO, contexto = contexto)

    private fun fundo(
        nome: String,
        gestora: String?,
        saldo: String,
        classe: ClasseAtivo = ClasseAtivo.FUNDO_MULTIMERCADO,
        cdiAno: String? = null,
    ) = Posicao(
        ativo = Ativo(ChaveAtivo.FundoPorNome(nome.uppercase()), nome, TipoAtivo.FUNDO, classe, gestora = gestora),
        saldo = Money.of(saldo).doRelatorio(),
        quantidade = null,
        percentualCdiAno = cdiAno?.let { Percent.of(it).doRelatorio() },
    )

    private fun cdb(
        emissor: String,
        saldo: String,
        vencimento: YearMonth,
    ) = Posicao(
        ativo =
            Ativo(
                ChaveAtivo.CreditoPrivado(TipoAtivo.CDB, emissor, Indexador.CDI, Percent.of("110"), vencimento),
                "CDB $emissor",
                TipoAtivo.CDB,
                ClasseAtivo.RF_POS,
                emissor = emissor,
                vencimento = vencimento,
            ),
        saldo = Money.of(saldo).doRelatorio(),
        quantidade = null,
    )

    private fun regras(a: AnaliseDeterministica) = a.alertas.map { it.severidade to it.regra }

    @Test
    fun `concentracao por gestora acima de 40 por cento e urgente, exatamente 40 nao`() {
        val acima = analisar(snapshot(listOf(fundo("A1", "Trend", "41000.00"), fundo("B1", "Verde", "59000.00"))))
        acima.gestoras.map { it.nome to it.percentual } shouldContainExactly
            listOf("Verde" to Percent.of("59"), "Trend" to Percent.of("41"))
        regras(acima) shouldContainExactly
            listOf(Severidade.URGENTE to RegraAlerta.CONCENTRACAO_GESTORA, Severidade.URGENTE to RegraAlerta.CONCENTRACAO_GESTORA)

        val exato =
            analisar(snapshot(listOf(fundo("A1", "Trend", "40000.00"), fundo("B1", "Verde", "30000.00"), fundo("C1", "SPX", "30000.00"))))
        exato.alertas.shouldBeEmpty()
    }

    @Test
    fun `gestora nao identificada nunca vira alerta de concentracao`() {
        val a = analisar(snapshot(listOf(fundo("A1", null, "100000.00"))))
        a.gestoras.single().nome shouldBe AnalisarCarteira.GESTORA_NAO_IDENTIFICADA
        a.alertas.shouldBeEmpty()
    }

    @Test
    fun `vencimento em ate 6 meses e urgente`() {
        val a =
            analisar(
                snapshot(
                    listOf(
                        cdb("BANCO A", "1000.00", YearMonth.of(2027, 3)),
                        cdb("BANCO B", "1000.00", YearMonth.of(2027, 4)),
                        cdb("BANCO C", "1000.00", YearMonth.of(2026, 8)),
                    ),
                ),
            )
        a.vencimentos.map { it.posicao.ativo.nome to it.mesesRestantes } shouldContainExactly listOf("CDB BANCO A" to 6L)
        a.alertas.single().regra shouldBe RegraAlerta.VENCIMENTO_PROXIMO
        a.alertas.single().titulo shouldBe "Vence em 03/2027: defina o destino do dinheiro"
    }

    @Test
    fun `emissor acima do FGC soma todos os papeis do mesmo emissor`() {
        val a =
            analisar(
                snapshot(
                    listOf(
                        cdb("BANCO A", "200000.00", YearMonth.of(2030, 1)),
                        cdb("BANCO A", "50000.01", YearMonth.of(2031, 1)),
                        cdb("BANCO B", "250000.00", YearMonth.of(2030, 1)),
                    ),
                ),
            )
        a.emissoresGarantidos.map { it.nome to it.valor } shouldContainExactly
            listOf("BANCO A" to Money.of("250000.01"), "BANCO B" to Money.of("250000.00"))
        regras(a) shouldContainExactly listOf(Severidade.CRITICO to RegraAlerta.EMISSOR_ACIMA_DO_FGC)
    }

    @Test
    fun `exposicao global acima de 30 por cento e critica`() {
        val a =
            analisar(
                snapshot(
                    listOf(
                        posicao(ativo("IVVB11", ClasseAtivo.RV_GLOBAL), "31000.00"),
                        posicao(ativo("BOVA11", ClasseAtivo.ETF), "69000.00"),
                    ),
                ),
            )
        a.exposicaoGlobal shouldBe Percent.of("31")
        regras(a) shouldContainExactly listOf(Severidade.CRITICO to RegraAlerta.EXPOSICAO_GLOBAL)
        a.alertas.single().ativos shouldBe listOf("IVVB11")
    }

    @Test
    fun `fundo abaixo do CDI no ano e posicao simbolica sao atencao`() {
        val a =
            analisar(
                snapshot(
                    listOf(
                        fundo("Fraco", "Verde", "1000.00", cdiAno = "99.99"),
                        fundo("Forte", "SPX", "1000.00", cdiAno = "100.00"),
                        fundo("Sem dado", "Kinea", "1000.00"),
                        posicao(ativo("HGRU11", ClasseAtivo.FII_TIJOLO, TipoAtivo.FII), "499.99"),
                        posicao(ativo("KNRI11", ClasseAtivo.FII_TIJOLO, TipoAtivo.FII), "500.00"),
                    ),
                ),
            )
        a.fundosAbaixoDoCdi.map { it.posicao.ativo.nome } shouldBe listOf("Fraco")
        a.simbolicas.map { it.ativo.nome } shouldBe listOf("HGRU11")
        regras(a) shouldContainExactly
            listOf(Severidade.ATENCAO to RegraAlerta.FUNDO_ABAIXO_DO_CDI, Severidade.ATENCAO to RegraAlerta.POSICAO_SIMBOLICA)
    }

    @Test
    fun `R6 - dado inconsistente fica fora do ritmo e vira alerta, fantasma e urgente`() {
        val lftb =
            posicao(ativo("LFTB11", ClasseAtivo.RF_POS), "1000.00", rentabilidadeMes = "34.36")
                .copy(rentabilidadeAno = Percent.of("37.78").doRelatorio(), rentabilidade24Meses = Percent.of("38.15").doRelatorio())
        val fantasma = posicao(ativo("RECR12", ClasseAtivo.FII_PAPEL, TipoAtivo.FII), "0.00", quantidade = "9")
        val a = analisar(snapshot(listOf(lftb, fantasma)))
        a.ativos.first { it.posicao.ativo.nome == "LFTB11" }.let {
            it.dadoAConferir shouldBe true
            it.ritmo.shouldBeNull()
        }
        regras(a) shouldContainExactly
            listOf(Severidade.URGENTE to RegraAlerta.POSICAO_FANTASMA, Severidade.INFO to RegraAlerta.DADO_A_CONFERIR)
    }

    @Test
    fun `R19 - resgates acima de 20 por cento em 12 meses sugerem consumo, e caixa zero e critico`() {
        fun mes(
            m: YearMonth,
            mov: String,
        ) = EvolucaoMensal(m, Money.ZERO, Money.of(mov), Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO, null, null)
        val contexto =
            ContextoRelatorio(
                resumo = null,
                referencias = listOf(IndiceReferencia("CDI", Percent.of("0.16"), Percent.of("9.5"), null, null)),
                rentabilidadeMensal = emptyList(),
                evolucaoMensal =
                    listOf(
                        // setembro de 2025 fica fora da janela de 12 meses
                        mes(YearMonth.of(2025, 9), "-900.00"),
                        mes(YearMonth.of(2025, 10), "-100.00"),
                        mes(YearMonth.of(2026, 8), "-110.00"),
                        mes(YearMonth.of(2026, 9), "500.00"),
                    ),
            )
        val a = analisar(snapshot(listOf(posicao(ativo("BOVA11", ClasseAtivo.ETF), "1000.00")), contexto))
        a.saques!!.resgates12Meses shouldBe Money.of("210.00")
        a.saques!!.percentualDoPatrimonio shouldBe Percent.of("21")
        a.saques!!.mesesComResgate shouldBe 2
        a.saques!!.sugereConsumo shouldBe true
        a.mercado.single().nome shouldBe "CDI"
        a.lacunas.map { it.tipo } shouldContainExactly
            listOf(TipoLacuna.PROTECAO_INFLACAO, TipoLacuna.SEM_PREFIXADO, TipoLacuna.SEM_LIQUIDEZ_COM_SAQUES)
        regras(a) shouldContainExactly listOf(Severidade.CRITICO to RegraAlerta.CAIXA_ZERO_COM_SAQUES)
    }

    @Test
    fun `sem contexto do relatorio (snapshot antigo) nao inventa saques nem mercado`() {
        val a = analisar(snapshot(listOf(posicao(saldo = "10000.00"))))
        a.saques.shouldBeNull()
        a.mercado.shouldBeEmpty()
        a.lacunas.map { it.tipo } shouldContainExactly listOf(TipoLacuna.PROTECAO_INFLACAO, TipoLacuna.SEM_PREFIXADO)
    }

    @Test
    fun `destaque exige ritmo acelerando e mais de 150 por cento do CDI no ano`() {
        fun etf(
            ticker: String,
            cdi: String,
        ) = posicao(ativo(ticker, ClasseAtivo.ETF), "10000.00").copy(
            rentabilidadeAno = Percent.of("14.47").doRelatorio(),
            rentabilidade24Meses = Percent.of("17.69").doRelatorio(),
            percentualCdiAno = Percent.of(cdi).doRelatorio(),
        )
        val a = analisar(snapshot(listOf(etf("BOVA11", "152.35"), etf("DIVO11", "150.00"))))
        regras(a) shouldContainExactly listOf(Severidade.DESTAQUE to RegraAlerta.PERFORMANCE_EXCEPCIONAL)
        a.alertas.single().ativos shouldBe listOf("BOVA11")
    }

    @Test
    fun `limites sao configuraveis - R23`() {
        val rigoroso = AnalisarCarteira(LimitesAnalise(concentracaoGestora = Percent.of("10")))
        regras(rigoroso(snapshot(listOf(fundo("A1", "Trend", "11000.00"), fundo("B1", null, "89000.00"))))) shouldContainExactly
            listOf(Severidade.URGENTE to RegraAlerta.CONCENTRACAO_GESTORA)
    }
}
