package com.investimentoeasy.parser.xperformance

import com.investimentoeasy.core.domain.classificacao.Pendencia
import com.investimentoeasy.core.domain.snapshot.PrepararRevisao
import com.investimentoeasy.core.domain.validacao.Gravidade
import com.investimentoeasy.core.domain.validacao.Problema
import com.investimentoeasy.core.domain.validacao.TipoInconsistencia
import com.investimentoeasy.core.model.ClasseAtivo
import com.investimentoeasy.core.model.Money
import com.investimentoeasy.core.model.Percent
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import java.math.RoundingMode

/** Do texto do relatório até a tela "Revisar extração". */
class RevisaoDoRelatorioIntegracaoTest {
    private val revisao =
        PrepararRevisao()(
            XPerformanceParser().parse(Fixtures.sintetico()).shouldBeInstanceOf<ResultadoParse.Sucesso>().extracao,
        )

    @Test
    fun `R3 - soma das posicoes fica 0,17 por cento abaixo do patrimonio e passa`() {
        val conferencia = revisao.validacao.conferenciaSoma!!
        conferencia.somaPosicoes shouldBe Money.of("150000.00")
        conferencia.diferenca shouldBe Money.of("250.00")
        conferencia.percentual.pontos.setScale(2, RoundingMode.HALF_EVEN) shouldBe Percent.of("0.17").pontos.setScale(2)
        conferencia.dentroDaTolerancia shouldBe true
        revisao.validacao.gravidadeMaxima shouldBe Gravidade.ALERTA
    }

    @Test
    fun `R6 - posicao fantasma e rentabilidade implausivel sao sinalizadas`() {
        revisao.validacao.problemas
            .filterIsInstance<Problema.Inconsistencia>()
            .map { it.nomeAtivo to it.tipo } shouldContainExactlyInAnyOrder
            listOf("RECR12" to TipoInconsistencia.POSICAO_FANTASMA, "LFTB11" to TipoInconsistencia.RENTABILIDADE_IMPLAUSIVEL)
    }

    @Test
    fun `R5 - classes e pendencias da carteira sintetica`() {
        val porNome = revisao.classificacao.associateBy { it.ativo.nome }
        porNome.getValue("CDB BANCO EXEMPLO S.A. - MAR/2028 - 112,00% CDI").ativo.classe shouldBe ClasseAtivo.RF_POS
        porNome.getValue("IMAB11").ativo.classe shouldBe ClasseAtivo.RF_IPCA
        porNome.getValue("Trend Nasdaq 100 FIA").ativo.gestora shouldBe "Trend"
        porNome.getValue("HGLG11").ativo.classe shouldBe ClasseAtivo.FII_TIJOLO
        revisao.classificacao.filter { it.pendencias.isNotEmpty() }.map { it.ativo.nome to it.pendencias } shouldContainExactlyInAnyOrder
            listOf(
                "Fundo Exemplo Debêntures Incentivadas FIC FIF-Infra RF RL" to setOf(Pendencia.GESTORA_DESCONHECIDA),
                "XYZW11" to setOf(Pendencia.SEGMENTO_FII_DESCONHECIDO),
            )
    }
}
