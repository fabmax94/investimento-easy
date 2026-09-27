package com.investimentoeasy.core.model

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/** Uma linha de posição: o que o relatório trouxe, sem cálculo. */
public data class Posicao(
    val ativo: Ativo,
    val saldo: Sourced<Money>,
    val quantidade: Sourced<BigDecimal>?,
    val rentabilidadeMes: Sourced<Percent>? = null,
    val rentabilidadeAno: Sourced<Percent>? = null,
    val rentabilidade24Meses: Sourced<Percent>? = null,
)

public enum class MetodoExtracao { PARSER, IA }

public enum class StatusSnapshot {
    /** Extraído, aguardando revisão e confirmação do usuário (R8). */
    RASCUNHO,

    /** Confirmado: imutável (R16). */
    CONFIRMADO,
}

@JvmInline
public value class SnapshotId(
    public val valor: String,
)

public data class Snapshot(
    val id: SnapshotId,
    val dataReferencia: LocalDate,
    val patrimonioInformado: Sourced<Money>?,
    val posicoes: List<Posicao>,
    val metodo: MetodoExtracao,
    val status: StatusSnapshot = StatusSnapshot.RASCUNHO,
    val versao: Int = 1,
    val confirmadoEm: Instant? = null,
    val divergenciaAceita: Boolean = false,
) {
    val somaPosicoes: Money get() = posicoes.map { it.saldo.valor }.soma()
}
