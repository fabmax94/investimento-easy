package com.investimentoeasy.feature.analysis

import com.investimentoeasy.core.ai.Acao
import com.investimentoeasy.core.ai.ComentarioAlerta
import com.investimentoeasy.core.ai.Comentarios
import com.investimentoeasy.core.ai.Diagnostico
import com.investimentoeasy.core.ai.Diagnosticos
import com.investimentoeasy.core.ai.Notas
import com.investimentoeasy.core.ai.NovoAtivo
import com.investimentoeasy.core.ai.OQueFazer
import com.investimentoeasy.core.ai.Prioridade
import com.investimentoeasy.core.ai.Realocacao
import com.investimentoeasy.core.ai.SaidaAnalise
import com.investimentoeasy.core.ai.StatusDiagnostico
import com.investimentoeasy.core.ai.TextoAba
import com.investimentoeasy.core.domain.analise.AnalisarCarteira
import com.investimentoeasy.core.domain.analise.TipoLacuna
import com.investimentoeasy.core.domain.complemento.CasarPlanilha
import com.investimentoeasy.core.domain.complemento.ComplementoPlanilha
import com.investimentoeasy.core.importacao.ArquivoRecebido
import com.investimentoeasy.core.importacao.ExtratorDeTextoPdf
import com.investimentoeasy.core.importacao.ImportarRelatorio
import com.investimentoeasy.core.importacao.ResultadoImportacao
import com.investimentoeasy.core.model.Snapshot
import com.investimentoeasy.core.model.StatusSnapshot
import com.investimentoeasy.parser.xlsx.FixturesPlanilhaXp
import com.investimentoeasy.parser.xperformance.FixturesXPerformance
import java.time.Instant

/** Carteira sintética do parser, confirmada, e uma análise do Claude com números dela. */
internal object Cenarios {
    val snapshot: Snapshot =
        (
            ImportarRelatorio(ExtratorDeTextoPdf { FixturesXPerformance.sintetico() })
                .importar(ArquivoRecebido("x.pdf", null, "%PDF".toByteArray())) as ResultadoImportacao.Lido
        ).revisao.rascunho!!.copy(status = StatusSnapshot.CONFIRMADO)

    val deterministica = AnalisarCarteira()(snapshot)

    /** Planilha sintética casada com a base sintética. */
    val complemento: ComplementoPlanilha =
        (
            ImportarRelatorio(ExtratorDeTextoPdf { emptyList() })
                .importar(ArquivoRecebido("p.xlsx", null, FixturesPlanilhaXp.bytes())) as ResultadoImportacao.PlanilhaLida
        ).let { CasarPlanilha()(snapshot, it.planilha, Instant.parse("2026-09-13T12:00:00Z")).complemento }

    val saida =
        SaidaAnalise(
            veredicto = "A performance não é o problema. O risco está em 33,3% em renda variável global e na falta de colchão de liquidez.",
            oQueFazer =
                OQueFazer(
                    acoes30Dias =
                        listOf(
                            Acao("Resolver o RECR12", "A posição tem quantidade e saldo zero: confirme com o assessor."),
                            Acao("Conferir o LFTB11", "O relatório mostra 34,36% no mês, fora do plausível para pós-fixado."),
                        ),
                    realocacoes = listOf(Realocacao("Trend Nasdaq 100 FIA", "Reduz a exposição global de 33,3%.", "IMAB11", "10000.00")),
                    novosAtivos =
                        listOf(
                            NovoAtivo(
                                "IMAB11",
                                TipoLacuna.PROTECAO_INFLACAO,
                                Prioridade.ALTA,
                                "10000.00",
                                "IPCA+ está em 3,33% da carteira.",
                            ),
                        ),
                ),
            mercado = TextoAba("O CDI rendeu 9,50% no ano; a carteira, 8,10%."),
            alocacao = Diagnosticos(listOf(Diagnostico(StatusDiagnostico.CRITICO, "Exterior em 33,3%, acima do limite de 30%."))),
            fundos = Notas(listOf("Trend Ouro FIF Multi RL está em 71,47% do CDI no ano.")),
            fiis = Notas(listOf("HGLG11 desacelerando; sem P/VP não dá para dizer se a queda é cíclica.")),
            acoesEtfs = Notas(listOf("BOVA11 com 152,32% do CDI no ano.")),
            alertas = Comentarios(listOf(ComentarioAlerta("EXPOSICAO_GLOBAL", "Dois ativos concentram a exposição global."))),
        )

    fun estado(
        ia: Boolean = false,
        aba: AbaAnalise = AbaAnalise.O_QUE_FAZER,
        temChave: Boolean = false,
        comPlanilha: Boolean = false,
    ) = EstadoAnalise(
        carregando = false,
        base = snapshot,
        deterministica = deterministica,
        ia = if (ia) AnaliseIa(saida, Instant.parse("2026-09-13T12:00:00Z"), "claude-opus-5") else null,
        aba = aba,
        temChave = temChave,
        complemento = if (comPlanilha) complemento else null,
    )
}
