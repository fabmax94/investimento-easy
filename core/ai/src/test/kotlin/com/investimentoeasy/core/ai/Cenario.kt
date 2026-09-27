package com.investimentoeasy.core.ai

import com.investimentoeasy.core.domain.analise.AnalisarCarteira
import com.investimentoeasy.core.domain.analise.AnaliseDeterministica
import com.investimentoeasy.core.model.Ativo
import com.investimentoeasy.core.model.ChaveAtivo
import com.investimentoeasy.core.model.ClasseAtivo
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
import java.time.LocalDate

/** Carteira pequena com números conhecidos: BOVA11 R$ 10.000 e um fundo global R$ 40.000. */
internal object Cenario {
    val snapshot: Snapshot =
        Snapshot(
            id = SnapshotId("s"),
            dataReferencia = LocalDate.of(2026, 9, 3),
            patrimonioInformado = Money.of("50000.00").doRelatorio(),
            posicoes =
                listOf(
                    posicao(ativo("BOVA11", ClasseAtivo.ETF), "10000.00").copy(
                        rentabilidadeAno = Percent.of("14.47").doRelatorio(),
                        rentabilidade24Meses = Percent.of("17.69").doRelatorio(),
                        percentualCdiAno = Percent.of("152.35").doRelatorio(),
                    ),
                    Posicao(
                        Ativo(
                            ChaveAtivo.FundoPorNome("TREND GLOBAL"),
                            "Trend Global FIA",
                            TipoAtivo.FUNDO,
                            ClasseAtivo.RV_GLOBAL,
                            gestora = "Trend",
                        ),
                        Money.of("40000.00").doRelatorio(),
                        null,
                    ),
                ),
            metodo = MetodoExtracao.PARSER,
            status = StatusSnapshot.CONFIRMADO,
        )

    val analise: AnaliseDeterministica = AnalisarCarteira()(snapshot)

    val entrada: EntradaAnalise = montarEntrada(snapshot, analise)

    fun saida(
        veredicto: String = "A carteira tem R$ 40.000 em renda variável global, 80% do total.",
        novos: List<NovoAtivo> = emptyList(),
        realocacoes: List<Realocacao> = emptyList(),
    ) = SaidaAnalise(
        veredicto = veredicto,
        oQueFazer = OQueFazer(listOf(Acao("Reduzir a concentração", "Trend Global FIA pesa 80%.")), realocacoes, novos),
        mercado = TextoAba("Sem índices no relatório."),
        alocacao = Diagnosticos(listOf(Diagnostico(StatusDiagnostico.CRITICO, "Exterior em 80%."))),
        fundos = Notas(listOf("Trend Global FIA sem %CDI no relatório.")),
        fiis = Notas(emptyList()),
        acoesEtfs = Notas(listOf("BOVA11 com 152% do CDI no ano e ritmo acelerando (+13,7 pp).")),
        alertas = Comentarios(listOf(ComentarioAlerta("EXPOSICAO_GLOBAL", "80% em um só fundo global."))),
    )
}
