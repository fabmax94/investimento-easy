package com.investimentoeasy.core.testing

import com.investimentoeasy.core.domain.snapshot.GeradorId
import com.investimentoeasy.core.model.Ativo
import com.investimentoeasy.core.model.ChaveAtivo
import com.investimentoeasy.core.model.ClasseAtivo
import com.investimentoeasy.core.model.ExtracaoCarteira
import com.investimentoeasy.core.model.FormatoArquivo
import com.investimentoeasy.core.model.MetodoExtracao
import com.investimentoeasy.core.model.Money
import com.investimentoeasy.core.model.Movimentacoes
import com.investimentoeasy.core.model.Percent
import com.investimentoeasy.core.model.Posicao
import com.investimentoeasy.core.model.PosicaoExtraida
import com.investimentoeasy.core.model.ResumoCarteira
import com.investimentoeasy.core.model.Snapshot
import com.investimentoeasy.core.model.SnapshotId
import com.investimentoeasy.core.model.StatusSnapshot
import com.investimentoeasy.core.model.TipoAtivo
import com.investimentoeasy.core.model.doRelatorio
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

public val INSTANTE_FIXO: Instant = Instant.parse("2026-09-13T12:00:00Z")

public fun relogioFixo(instante: Instant = INSTANTE_FIXO): Clock = Clock.fixed(instante, ZoneId.of("America/Sao_Paulo"))

/** Gera ids previsíveis: s1, s2, s3... */
public fun geradorSequencial(prefixo: String = "s"): GeradorId {
    var n = 0
    return GeradorId { SnapshotId("$prefixo${++n}") }
}

public fun posicaoExtraida(
    nome: String = "BOVA11",
    estrategia: String = "Renda Variável Brasil",
    saldo: String = "1000.00",
    quantidade: String? = "10",
    rentabilidadeMes: String? = "1.00",
): PosicaoExtraida =
    PosicaoExtraida(
        nome = nome,
        estrategia = estrategia,
        saldo = Money.of(saldo),
        quantidade = quantidade?.let(::BigDecimal),
        percentualAlocacao = null,
        rentabilidadeMes = rentabilidadeMes?.let(Percent::of),
        percentualCdiMes = null,
        rentabilidadeAno = null,
        percentualCdiAno = null,
        rentabilidade24Meses = null,
        percentualCdi24Meses = null,
    )

public fun extracao(
    posicoes: List<PosicaoExtraida> = listOf(posicaoExtraida()),
    patrimonio: String? = posicoes.fold(BigDecimal.ZERO) { acc, p -> acc + p.saldo.valor }.toPlainString(),
    dataReferencia: LocalDate? = LocalDate.of(2026, 9, 3),
    metodo: MetodoExtracao = MetodoExtracao.PARSER,
): ExtracaoCarteira =
    ExtracaoCarteira(
        formato = FormatoArquivo.XPERFORMANCE_PDF,
        metodo = metodo,
        dataReferencia = dataReferencia,
        resumo = patrimonio?.let { ResumoCarteira(Money.of(it), null, null, null, null) },
        referencias = emptyList(),
        estrategias = emptyList(),
        posicoes = posicoes,
        rentabilidadeMensal = emptyList(),
        evolucaoMensal = emptyList(),
        movimentacoes = Movimentacoes.NaoInformadas,
        avisos = emptyList(),
    )

public fun ativo(
    ticker: String = "BOVA11",
    classe: ClasseAtivo = ClasseAtivo.ETF,
    tipo: TipoAtivo = TipoAtivo.ETF,
): Ativo = Ativo(ChaveAtivo.Ticker(ticker), ticker, tipo, classe)

public fun posicao(
    ativo: Ativo = ativo(),
    saldo: String = "1000.00",
    quantidade: String? = "10",
    rentabilidadeMes: String? = null,
): Posicao =
    Posicao(
        ativo = ativo,
        saldo = Money.of(saldo).doRelatorio(),
        quantidade = quantidade?.let { BigDecimal(it).doRelatorio() },
        rentabilidadeMes = rentabilidadeMes?.let { Percent.of(it).doRelatorio() },
    )

public fun snapshotConfirmado(
    id: String,
    data: LocalDate,
    versao: Int = 1,
): Snapshot =
    Snapshot(
        id = SnapshotId(id),
        dataReferencia = data,
        patrimonioInformado = Money.of("1000.00").doRelatorio(),
        posicoes = listOf(posicao()),
        metodo = MetodoExtracao.PARSER,
        status = StatusSnapshot.CONFIRMADO,
        versao = versao,
        confirmadoEm = INSTANTE_FIXO,
    )
