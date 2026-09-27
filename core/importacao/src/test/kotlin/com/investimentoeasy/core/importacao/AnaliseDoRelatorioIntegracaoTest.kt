package com.investimentoeasy.core.importacao

import com.investimentoeasy.core.domain.analise.AnalisarCarteira
import com.investimentoeasy.core.domain.analise.RegraAlerta
import com.investimentoeasy.core.domain.analise.Ritmo
import com.investimentoeasy.core.domain.analise.Severidade
import com.investimentoeasy.core.domain.analise.TipoLacuna
import com.investimentoeasy.core.model.Percent
import com.investimentoeasy.core.model.StatusSnapshot
import com.investimentoeasy.parser.xperformance.FixturesXPerformance
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

/** Do texto do relatório sintético até as Camadas 1 e 2. */
class AnaliseDoRelatorioIntegracaoTest {
    private val analise =
        ImportarRelatorio(ExtratorDeTextoPdf { FixturesXPerformance.sintetico() })
            .importar(ArquivoRecebido("x.pdf", null, "%PDF".toByteArray()))
            .shouldBeInstanceOf<ResultadoImportacao.Lido>()
            .revisao.rascunho!!
            .copy(status = StatusSnapshot.CONFIRMADO)
            .let { AnalisarCarteira()(it) }

    @Test
    fun `alertas da carteira sintetica, na ordem de severidade`() {
        analise.alertas.map { it.severidade to it.regra to it.ativos } shouldContainExactly
            listOf(
                Severidade.URGENTE to RegraAlerta.POSICAO_FANTASMA to listOf("RECR12"),
                Severidade.CRITICO to RegraAlerta.EXPOSICAO_GLOBAL to listOf("IVVB11", "Trend Nasdaq 100 FIA"),
                Severidade.CRITICO to RegraAlerta.CAIXA_ZERO_COM_SAQUES to emptyList(),
                Severidade.ATENCAO to RegraAlerta.FUNDO_ABAIXO_DO_CDI to
                    listOf("Fundo Exemplo Debêntures Incentivadas FIC FIF-Infra RF RL"),
                Severidade.ATENCAO to RegraAlerta.FUNDO_ABAIXO_DO_CDI to listOf("Trend Ouro FIF Multi RL"),
                Severidade.INFO to RegraAlerta.DADO_A_CONFERIR to listOf("LFTB11"),
                Severidade.DESTAQUE to RegraAlerta.PERFORMANCE_EXCEPCIONAL to listOf("BOVA11"),
                Severidade.DESTAQUE to RegraAlerta.PERFORMANCE_EXCEPCIONAL to listOf("Trend Nasdaq 100 FIA"),
            )
    }

    @Test
    fun `Trend soma exatamente 40 por cento e por isso nao dispara concentracao`() {
        analise.gestoras.first().let {
            it.nome shouldBe "Trend"
            it.percentual shouldBe Percent.of("40")
        }
    }

    @Test
    fun `ritmo, lacunas e mercado do relatorio`() {
        analise.ativos.first { it.posicao.ativo.nome == "Trend Nasdaq 100 FIA" }.ritmo!!.ritmo shouldBe Ritmo.ACELERANDO
        analise.ativos.first { it.posicao.ativo.nome == "HGLG11" }.ritmo!!.ritmo shouldBe Ritmo.DESACELERANDO
        analise.lacunas.map { it.tipo } shouldContainExactly
            listOf(TipoLacuna.PROTECAO_INFLACAO, TipoLacuna.SEM_PREFIXADO, TipoLacuna.SEM_LIQUIDEZ_COM_SAQUES)
        analise.mercado.map { it.nome } shouldContainExactly listOf("Portfólio", "CDI", "Ibovespa", "IPCA", "Dólar")
        analise.saques!!.sugereConsumo shouldBe false
    }
}
