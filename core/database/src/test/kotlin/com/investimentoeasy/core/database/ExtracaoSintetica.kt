package com.investimentoeasy.core.database

import com.investimentoeasy.core.model.EvolucaoMensal
import com.investimentoeasy.core.model.ExtracaoCarteira
import com.investimentoeasy.core.model.IndiceReferencia
import com.investimentoeasy.core.model.Money
import com.investimentoeasy.core.model.Percent
import com.investimentoeasy.core.model.RentabilidadeMensal
import com.investimentoeasy.core.model.ResumoCarteira
import com.investimentoeasy.core.testing.extracao
import com.investimentoeasy.core.testing.posicaoExtraida
import java.time.YearMonth

internal object ExtracaoSintetica {
    fun completa(): ExtracaoCarteira =
        extracao(listOf(posicaoExtraida("BOVA11", saldo = "1000.00"))).let { base ->
            base.copy(
                resumo = ResumoCarteira(Money.of("1000.00"), Percent.of("0.85"), Money.of("8.50"), Percent.of("25.1"), Money.of("200.00")),
                referencias =
                    listOf(
                        IndiceReferencia("CDI", Percent.of("0.16"), Percent.of("9.50"), Percent.of("13.43"), Percent.of("28.51")),
                    ),
                rentabilidadeMensal = listOf(RentabilidadeMensal(YearMonth.of(2026, 9), Percent.of("0.85"), Percent.of("531.25"))),
                evolucaoMensal =
                    listOf(
                        EvolucaoMensal(
                            YearMonth.of(2026, 8),
                            Money.of("6000.00"),
                            Money.of("-5000.00"),
                            Money.of("-3.00"),
                            Money.ZERO,
                            Money.of("1000.00"),
                            Money.of("3.00"),
                            Percent.of("0.3"),
                            null,
                        ),
                    ),
                posicoes = base.posicoes.map { it.copy(percentualCdiAno = Percent.of("152.35")) },
            )
        }
}
