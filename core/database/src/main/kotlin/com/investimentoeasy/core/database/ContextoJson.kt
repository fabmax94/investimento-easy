package com.investimentoeasy.core.database

import com.investimentoeasy.core.model.ContextoRelatorio
import com.investimentoeasy.core.model.EvolucaoMensal
import com.investimentoeasy.core.model.IndiceReferencia
import com.investimentoeasy.core.model.Money
import com.investimentoeasy.core.model.Percent
import com.investimentoeasy.core.model.RentabilidadeMensal
import com.investimentoeasy.core.model.ResumoCarteira
import kotlinx.serialization.Serializable
import java.time.YearMonth

/**
 * Contexto do relatório gravado como JSON na coluna `snapshot.contexto`: não é consultado
 * por SQL, só lido junto com o snapshot. Números como texto, para não perder precisão.
 */
@Serializable
internal data class ContextoJson(
    val resumo: ResumoJson? = null,
    val referencias: List<ReferenciaJson> = emptyList(),
    val mensal: List<MensalJson> = emptyList(),
    val evolucao: List<EvolucaoJson> = emptyList(),
) {
    fun paraModelo(): ContextoRelatorio =
        ContextoRelatorio(
            resumo = resumo?.paraModelo(),
            referencias = referencias.map { it.paraModelo() },
            rentabilidadeMensal = mensal.map { it.paraModelo() },
            evolucaoMensal = evolucao.map { it.paraModelo() },
        )

    companion object {
        fun de(c: ContextoRelatorio): ContextoJson =
            ContextoJson(
                resumo =
                    c.resumo?.let {
                        ResumoJson(
                            it.patrimonioTotalBruto.texto(),
                            it.rentabilidadeMes?.texto(),
                            it.ganhoMes?.texto(),
                            it.rentabilidade24Meses?.texto(),
                            it.ganho24Meses?.texto(),
                        )
                    },
                referencias =
                    c.referencias.map {
                        ReferenciaJson(it.nome, it.mes?.texto(), it.ano?.texto(), it.dozeMeses?.texto(), it.vinteQuatroMeses?.texto())
                    },
                mensal = c.rentabilidadeMensal.map { MensalJson(it.mes.toString(), it.portfolio?.texto(), it.percentualCdi?.texto()) },
                evolucao =
                    c.evolucaoMensal.map {
                        EvolucaoJson(
                            it.mes.toString(),
                            it.patrimonioInicial.texto(),
                            it.movimentacoes.texto(),
                            it.ir.texto(),
                            it.iof.texto(),
                            it.patrimonioFinal.texto(),
                            it.ganhoFinanceiro.texto(),
                            it.rentabilidade?.texto(),
                            it.percentualCdi?.texto(),
                        )
                    },
            )
    }
}

@Serializable
internal data class ResumoJson(
    val patrimonio: String,
    val rentabilidadeMes: String?,
    val ganhoMes: String?,
    val rentabilidade24Meses: String?,
    val ganho24Meses: String?,
) {
    fun paraModelo() =
        ResumoCarteira(
            Money.of(patrimonio),
            rentabilidadeMes?.let(Percent::of),
            ganhoMes?.let(Money::of),
            rentabilidade24Meses?.let(Percent::of),
            ganho24Meses?.let(Money::of),
        )
}

@Serializable
internal data class ReferenciaJson(
    val nome: String,
    val mes: String?,
    val ano: String?,
    val doze: String?,
    val vinteQuatro: String?,
) {
    fun paraModelo() =
        IndiceReferencia(nome, mes?.let(Percent::of), ano?.let(Percent::of), doze?.let(Percent::of), vinteQuatro?.let(Percent::of))
}

@Serializable
internal data class MensalJson(
    val mes: String,
    val portfolio: String?,
    val cdi: String?,
) {
    fun paraModelo() = RentabilidadeMensal(YearMonth.parse(mes), portfolio?.let(Percent::of), cdi?.let(Percent::of))
}

@Serializable
internal data class EvolucaoJson(
    val mes: String,
    val inicial: String,
    val movimentacoes: String,
    val ir: String,
    val iof: String,
    val final: String,
    val ganho: String,
    val rentabilidade: String?,
    val cdi: String?,
) {
    fun paraModelo() =
        EvolucaoMensal(
            YearMonth.parse(mes),
            Money.of(inicial),
            Money.of(movimentacoes),
            Money.of(ir),
            Money.of(iof),
            Money.of(final),
            Money.of(ganho),
            rentabilidade?.let(Percent::of),
            cdi?.let(Percent::of),
        )
}

private fun Money.texto(): String = valor.toPlainString()

private fun Percent.texto(): String = pontos.toPlainString()
