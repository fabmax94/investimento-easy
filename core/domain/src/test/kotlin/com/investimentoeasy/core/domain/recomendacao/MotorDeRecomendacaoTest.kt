package com.investimentoeasy.core.domain.recomendacao

import com.investimentoeasy.core.domain.alocacao.GrupoAlocacao
import com.investimentoeasy.core.domain.analise.AnalisarCarteira
import com.investimentoeasy.core.domain.analise.RegraAlerta
import com.investimentoeasy.core.domain.classificacao.ClassificadorAtivos
import com.investimentoeasy.core.domain.mercado.Perfil
import com.investimentoeasy.core.domain.mercado.leituraDeCambio
import com.investimentoeasy.core.domain.mercado.leituraDeJuros
import com.investimentoeasy.core.model.ContextoRelatorio
import com.investimentoeasy.core.model.Cotacao
import com.investimentoeasy.core.model.EvolucaoMensal
import com.investimentoeasy.core.model.ExpectativasFocus
import com.investimentoeasy.core.model.FonteMercado
import com.investimentoeasy.core.model.IndiceReferencia
import com.investimentoeasy.core.model.InformeFii
import com.investimentoeasy.core.model.Money
import com.investimentoeasy.core.model.PanoramaMercado
import com.investimentoeasy.core.model.Percent
import com.investimentoeasy.core.model.Posicao
import com.investimentoeasy.core.model.Snapshot
import com.investimentoeasy.core.model.ValorDeMercado
import com.investimentoeasy.core.model.doRelatorio
import com.investimentoeasy.core.testing.INSTANTE_FIXO
import com.investimentoeasy.core.testing.posicaoExtraida
import com.investimentoeasy.core.testing.snapshotConfirmado
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

/** Carteira de R$ 100 mil com números redondos: cada valor esperado dá para conferir de cabeça. */
class MotorDeRecomendacaoTest {
    private val hoje = LocalDate.of(2026, 9, 26)
    private val classificador = ClassificadorAtivos()

    private fun posicao(
        nome: String,
        estrategia: String,
        saldo: String,
        quantidade: String? = null,
    ): Posicao {
        val e = posicaoExtraida(nome, estrategia, saldo, quantidade)
        return Posicao(classificador.classificar(e).ativo, e.saldo.doRelatorio(), e.quantidade?.doRelatorio())
    }

    private val posicoes =
        listOf(
            posicao("BOVA11", "Renda Variável Brasil", "5000.00", "50"),
            posicao("IVVB11", "Renda Variável Global", "40000.00", "100"),
            posicao("Trend Nasdaq 100 FIA", "Renda Variável Global", "20000.00"),
            posicao("CDB BANCO EXEMPLO S.A. - MAR/2028 - 112,00% CDI", "Pós Fixado", "30000.00", "30"),
            posicao("HGLG11", "Fundos Listados", "5000.00", "50"),
        )

    private fun evolucao(
        mes: Int,
        movimentacao: String,
    ) = EvolucaoMensal(
        YearMonth.of(2026, mes), Money.of("100000.00"), Money.of(movimentacao), Money.ZERO, Money.ZERO, Money.of("100000.00"),
        Money.ZERO, null, null,
    )

    private val contexto =
        ContextoRelatorio(
            resumo = null,
            referencias =
                listOf(
                    IndiceReferencia("Portfólio", null, null, null, Percent.of("30.00")),
                    IndiceReferencia("CDI", null, null, Percent.of("13.40"), Percent.of("28.00")),
                ),
            rentabilidadeMensal = emptyList(),
            evolucaoMensal = listOf(evolucao(7, "-1000.00"), evolucao(8, "-30000.00"), evolucao(9, "-2000.00")),
        )

    private val base: Snapshot =
        snapshotConfirmado(
            "base",
            LocalDate.of(2026, 9, 3),
        ).copy(posicoes = posicoes, patrimonioInformado = Money.of("100000.00").doRelatorio())

    private fun cotacao(
        simbolo: String,
        preco: String,
        r12: String,
    ) = Cotacao(simbolo, BigDecimal(preco), hoje, Percent.of("1"), Percent.of("2"), Percent.of(r12), BigDecimal(preco))

    private val panorama =
        PanoramaMercado(
            obtidoEm = INSTANTE_FIXO,
            selicMeta = ValorDeMercado(BigDecimal("13.75"), hoje, FonteMercado.BANCO_CENTRAL),
            ipca12Meses = ValorDeMercado(BigDecimal("4.22"), LocalDate.of(2026, 8, 1), FonteMercado.BANCO_CENTRAL),
            dolar = ValorDeMercado(BigDecimal("5.20"), hoje, FonteMercado.BANCO_CENTRAL),
            focus =
                ExpectativasFocus(
                    LocalDate.of(2026, 9, 18),
                    mapOf(2026 to BigDecimal("13.50"), 2027 to BigDecimal("12.00")),
                    mapOf(2026 to BigDecimal("4.92")),
                    mapOf(2026 to BigDecimal("5.20")),
                ),
            ibovespa = cotacao("^BVSP", "183000", "26.3"),
            ifix = cotacao("XFIX11.SA", "13.17", "4.2"),
            cotacoes = mapOf("BOVA11" to cotacao("BOVA11.SA", "100", "27"), "HGLG11" to cotacao("HGLG11.SA", "90", "1")),
            fiis =
                mapOf(
                    "HGLG" to
                        InformeFii("HGLG", LocalDate.of(2026, 8, 1), BigDecimal("100.00"), Percent.of("8.00"), Percent.of("0.70"), 12),
                ),
            falhas = emptyList(),
        )

    private fun recomendar(
        perfil: Perfil = Perfil.MODERADO,
        comMercado: Boolean = true,
        snapshot: Snapshot = base,
    ): Recomendacao {
        val mercado = panorama.takeIf { comMercado }
        val analise = AnalisarCarteira()(snapshot, mercado?.let { leituraDeJuros(it, hoje) }, mercado?.let { leituraDeCambio(it, hoje) })
        return MotorDeRecomendacao()(EntradaDoMotor(snapshot, analise, perfil, mercado, null, hoje))
    }

    @Test
    fun `exterior acima do maximo vira venda com destino no grupo mais abaixo do perfil`() {
        val r = recomendar()
        // Exterior 60% contra máximo de 20%: 40 pp de R$ 100 mil. IVVB11 (maior) cobre sozinho.
        r.realocacoes.map { it.vender to it.valor } shouldContainExactly listOf("IVVB11" to Money.of("40000.00"))
        r.realocacoes.single().destino shouldBe "Tesouro IPCA+ ou IMAB11 (renda fixa IPCA+)"
        r.realocacoes.single().motivo shouldStartWith "Exterior em 60,0%, acima do máximo de 20% do perfil moderado"
    }

    @Test
    fun `sugestoes levam os grupos abaixo do minimo ao alvo, ligadas ao ciclo`() {
        val r = recomendar()
        val ipca = r.novosAtivos.first { it.grupo == GrupoAlocacao.RENDA_FIXA_IPCA }
        ipca.valor shouldBe Money.of("20000.00")
        ipca.prioridade shouldBe Prioridade.ALTA
        ipca.justificativa shouldContain "Com o Focus projetando Selic de 13,75% para 12,00% no fim de 2027"
        r.novosAtivos.first { it.grupo == GrupoAlocacao.RENDA_FIXA_PRE }.valor shouldBe Money.of("5000.00")
        r.novosAtivos.none { it.grupo == GrupoAlocacao.ACOES_BRASIL } shouldBe true
    }

    @Test
    fun `diagnostico contra a faixa do perfil`() {
        val r = recomendar()
        r.alocacao.first { it.texto.startsWith("Exterior") }.status shouldBe StatusDiagnostico.CRITICO
        r.alocacao.first { it.texto.startsWith("Ações Brasil") }.status shouldBe StatusDiagnostico.OK
        r.alocacao.first { it.texto.startsWith("Renda fixa IPCA+") }.texto shouldContain "abaixo do mínimo"
    }

    @Test
    fun `veredicto com desempenho, maior risco e ciclo`() {
        val r = recomendar(snapshot = base.copy(contexto = contexto))
        r.veredicto shouldContain "Em 24 meses a carteira rendeu 30,0%, 107% do CDI: a performance não é o problema."
        r.veredicto shouldContain "renda variável global"
        r.veredicto shouldContain "O Focus projeta Selic de 13,75% hoje para 12,00% no fim de 2027: ciclo de corte."
    }

    @Test
    fun `colchao pela mediana dos saques, sem inflar com o saque grande isolado`() {
        val r = recomendar(snapshot = base.copy(contexto = contexto))
        val colchao = r.novosAtivos.first { it.motivo == MotivoSugestao.COLCHAO_DE_LIQUIDEZ }
        // Saques de 1.000, 30.000 e 2.000: mediana 2.000 × 6 meses.
        colchao.valor shouldBe Money.of("12000.00")
        r.acoes30Dias.first { it.titulo == "Montar o colchão de liquidez" }.detalhe shouldContain "R$ 12.000"
        r.realocacoes.single().destino shouldBe "Tesouro Selic (colchão de liquidez)"
    }

    @Test
    fun `colchao ja conta como pos-fixado e nao duplica a sugestao de Tesouro Selic`() {
        // Conservador: pós-fixado em 30% contra mínimo de 35% e alvo de 45% (faltam 15 mil); o colchão já leva 12 mil.
        val r = recomendar(Perfil.CONSERVADOR, snapshot = base.copy(contexto = contexto))
        r.novosAtivos.filter { it.grupo == GrupoAlocacao.RENDA_FIXA_POS }.map { it.motivo to it.valor } shouldContainExactly
            listOf(MotivoSugestao.COLCHAO_DE_LIQUIDEZ to Money.of("12000.00"), MotivoSugestao.ABAIXO_DO_PERFIL to Money.of("3000.00"))
    }

    @Test
    fun `mercado liga cada indicador a carteira e FII traz P-VP`() {
        val r = recomendar()
        r.mercado.first() shouldStartWith "Juros: Selic de 13,75% com o Focus em 12,00% no fim de 2027."
        r.mercado.first() shouldContain "R$ 30.000 em pós-fixado"
        r.mercado.any { it.startsWith("Dólar: R$ 5,20 hoje") } shouldBe true
        r.fiis.first() shouldBe "HGLG11: P/VP 0,90 (VP de 08/2026), desconto de 11,1% até o valor patrimonial; DY 12m de 8,0%."
        r.acoesEtfs.first() shouldContain "BOVA11: 3m +1,0%, 6m +2,0%, 12m +27,0%; +0,7% contra o Ibovespa em 12 meses"
        r.mercadoObtidoEm shouldBe INSTANTE_FIXO
    }

    @Test
    fun `sem mercado faz o que da com o relatorio e o perfil`() {
        val r = recomendar(comMercado = false)
        r.mercado.shouldBeEmpty()
        r.fiis.shouldBeEmpty()
        r.acoesEtfs.shouldBeEmpty()
        r.veredicto shouldNotContain "Focus"
        r.realocacoes.single().vender shouldBe "IVVB11"
        r.novosAtivos.first { it.grupo == GrupoAlocacao.RENDA_FIXA_IPCA }.justificativa shouldNotContain "Focus"
        r.mercadoObtidoEm shouldBe null
    }

    @Test
    fun `perfil muda a faixa e o tamanho das sugestoes`() {
        val conservador = recomendar(Perfil.CONSERVADOR)
        val arrojado = recomendar(Perfil.ARROJADO)

        // Exterior: máximo 10% no conservador (vende 50 mil, em duas posições) e 30% no arrojado (vende 30 mil).
        fun exterior(r: Recomendacao) = r.realocacoes.filter { it.motivo.startsWith("Exterior") }.sumOf { it.valor.valor }
        exterior(conservador) shouldBe BigDecimal("50000.00")
        exterior(arrojado) shouldBe BigDecimal("30000.00")
        // No arrojado a renda fixa pós (30%) também passa do máximo de 20%: o CDB cede 10 mil.
        arrojado.realocacoes.first { it.vender.startsWith("CDB") }.valor shouldBe Money.of("10000.00")
        arrojado.novosAtivos.first { it.grupo == GrupoAlocacao.ACOES_BRASIL }.sugestao shouldBe "PIBB11 ou SMAL11 (small caps)"
    }

    @Test
    fun `cada alerta ganha um comentario e o plano tem no maximo cinco acoes`() {
        val r = recomendar(snapshot = base.copy(contexto = contexto))
        r.comentariosAlertas.keys shouldBe r.comentariosAlertas.keys.intersect(RegraAlerta.entries.toSet())
        r.comentariosAlertas[RegraAlerta.EXPOSICAO_GLOBAL]!! shouldStartWith "60,0% em renda variável global, acima do limite de 30%"
        (r.acoes30Dias.size <= 5) shouldBe true
    }
}
