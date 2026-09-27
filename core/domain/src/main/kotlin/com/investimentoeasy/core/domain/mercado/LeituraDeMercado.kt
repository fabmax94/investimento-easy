package com.investimentoeasy.core.domain.mercado

import com.investimentoeasy.core.model.Cotacao
import com.investimentoeasy.core.model.InformeFii
import com.investimentoeasy.core.model.PanoramaMercado
import com.investimentoeasy.core.model.Percent
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.LocalDate

/** Direção dos juros que o mercado espera (Focus), comparada com a Selic de hoje. */
public enum class CicloJuros { CORTE, ALTA, ESTAVEL }

public data class LeituraDeJuros(
    val ciclo: CicloJuros,
    /** Selic meta de hoje; sem o Banco Central, a mediana do Focus para o fim deste ano. */
    val selicAtual: BigDecimal,
    val selicEsperada: BigDecimal,
    val anoEsperado: Int,
    val dataPesquisa: LocalDate,
)

/**
 * Horizonte de 6 a 15 meses: até junho, o fim deste ano; depois, o fim do próximo. Diferença de
 * 0,75 pp ou mais é ciclo (três reuniões de 0,25); menos que isso, estável.
 */
public fun leituraDeJuros(
    panorama: PanoramaMercado,
    hoje: LocalDate,
): LeituraDeJuros? {
    val focus = panorama.focus ?: return null
    val ano = if (hoje.monthValue <= JUNHO) hoje.year else hoje.year + 1
    val esperada = focus.selicFimDeAno[ano] ?: return null
    val atual = panorama.selicMeta?.valor ?: focus.selicFimDeAno[hoje.year] ?: return null
    val delta = esperada - atual
    val ciclo =
        when {
            delta <= -LIMIAR_CICLO -> CicloJuros.CORTE
            delta >= LIMIAR_CICLO -> CicloJuros.ALTA
            else -> CicloJuros.ESTAVEL
        }
    return LeituraDeJuros(ciclo, atual, esperada, ano, focus.dataPesquisa)
}

/** Direção do dólar que o mercado espera para o fim do ano, contra o dólar de hoje. */
public enum class DirecaoCambio { REAL_MAIS_FRACO, REAL_MAIS_FORTE, ESTAVEL }

public data class LeituraDeCambio(
    val direcao: DirecaoCambio,
    val dolarAtual: BigDecimal,
    val dolarEsperado: BigDecimal,
    val ano: Int,
)

public fun leituraDeCambio(
    panorama: PanoramaMercado,
    hoje: LocalDate,
): LeituraDeCambio? {
    val atual = panorama.dolar?.valor ?: return null
    val esperado = panorama.focus?.cambioFimDeAno?.get(hoje.year) ?: return null
    val variacao = esperado.divide(atual, MathContext.DECIMAL64) - BigDecimal.ONE
    val direcao =
        when {
            variacao >= LIMIAR_CAMBIO -> DirecaoCambio.REAL_MAIS_FRACO
            variacao <= -LIMIAR_CAMBIO -> DirecaoCambio.REAL_MAIS_FORTE
            else -> DirecaoCambio.ESTAVEL
        }
    return LeituraDeCambio(direcao, atual, esperado, hoje.year)
}

/** Por que o FII caiu, pelos únicos fatores que os dados públicos permitem separar. */
public enum class MotivoQueda {
    /** Não caiu em 12 meses, ou caiu pouco com o setor subindo (sem padrão para explicar). */
    SEM_QUEDA,

    /** Caiu junto com o IFIX: efeito de juros no setor, costuma reverter com o ciclo. */
    CICLICA,

    /** Caiu bem mais que o IFIX: problema do próprio fundo; vira pergunta, não decisão. */
    ESPECIFICA,

    /** Sem histórico do fundo ou do IFIX para comparar. */
    SEM_DADO,
}

public data class FiiNoMercado(
    val ticker: String,
    val preco: BigDecimal,
    val valorPatrimonialCota: BigDecimal,
    val mesReferenciaVp: LocalDate,
    val pvp: BigDecimal,
    /** Quanto a cota sobe até o valor patrimonial, em %; negativo se negocia com prêmio. */
    val ateOValorPatrimonial: Percent,
    val dividendYield12Meses: Percent?,
    /** Último rendimento mensal × 12: comparado com o DY de 12 meses, mostra se a renda encolheu. */
    val dividendYieldCorrenteAnualizado: Percent?,
    val retorno12Meses: Percent?,
    val motivoQueda: MotivoQueda,
) {
    /** Renda corrente 25% ou mais abaixo da média de 12 meses: distribuição encolhendo. */
    val distribuicaoEncolhendo: Boolean
        get() {
            val doze = dividendYield12Meses?.pontos ?: return false
            val corrente = dividendYieldCorrenteAnualizado?.pontos ?: return false
            return doze.signum() > 0 && corrente < doze.multiply(FATOR_ENCOLHIMENTO)
        }
}

public fun fiiNoMercado(
    ticker: String,
    cotacao: Cotacao?,
    informe: InformeFii?,
    ifix: Cotacao?,
): FiiNoMercado? {
    if (cotacao == null || informe == null || informe.valorPatrimonialCota.signum() <= 0) return null
    val pvp = cotacao.preco.divide(informe.valorPatrimonialCota, MathContext.DECIMAL64).setScale(2, RoundingMode.HALF_EVEN)
    // Fora dessa faixa o VP informado à CVM não bate com a cota (desdobramento, erro de preenchimento).
    if (pvp < PVP_MINIMO_PLAUSIVEL || pvp > PVP_MAXIMO_PLAUSIVEL) return null
    val ate = informe.valorPatrimonialCota.divide(cotacao.preco, MathContext.DECIMAL64).subtract(BigDecimal.ONE).multiply(CEM)
    return FiiNoMercado(
        ticker = ticker,
        preco = cotacao.preco,
        valorPatrimonialCota = informe.valorPatrimonialCota,
        mesReferenciaVp = informe.mesReferencia,
        pvp = pvp,
        ateOValorPatrimonial = Percent.of(ate.setScale(2, RoundingMode.HALF_EVEN)),
        // Zero em todos os meses é campo não preenchido, não fundo sem renda.
        dividendYield12Meses = informe.dividendYield12Meses?.takeIf { it.pontos.signum() > 0 },
        dividendYieldCorrenteAnualizado =
            informe.dividendYieldUltimoMes?.takeIf { it.pontos.signum() > 0 }?.let { Percent.of(it.pontos.multiply(DOZE)) },
        retorno12Meses = cotacao.retorno12Meses,
        motivoQueda = motivoQueda(cotacao.retorno12Meses, ifix?.retorno12Meses),
    )
}

internal fun motivoQueda(
    fundo: Percent?,
    ifix: Percent?,
): MotivoQueda =
    when {
        fundo == null -> MotivoQueda.SEM_DADO
        fundo >= Percent.ZERO -> MotivoQueda.SEM_QUEDA
        ifix == null -> MotivoQueda.SEM_DADO
        fundo.pontos < ifix.pontos - DIFERENCA_ESPECIFICA -> MotivoQueda.ESPECIFICA
        ifix < Percent.ZERO -> MotivoQueda.CICLICA
        // Caiu pouco com o setor subindo: sem padrão que permita dizer o motivo.
        else -> MotivoQueda.SEM_QUEDA
    }

/** Distância da máxima de 52 semanas, em % (negativo = abaixo da máxima). */
public fun distanciaDaMaxima(cotacao: Cotacao): Percent? {
    val maxima = cotacao.maxima52Semanas?.takeIf { it.signum() > 0 } ?: return null
    val pontos = cotacao.preco.divide(maxima, MathContext.DECIMAL64).subtract(BigDecimal.ONE).multiply(CEM)
    return Percent.of(pontos.setScale(2, RoundingMode.HALF_EVEN))
}

private const val JUNHO = 6
private val LIMIAR_CICLO = BigDecimal("0.75")
private val LIMIAR_CAMBIO = BigDecimal("0.03")
private val CEM = BigDecimal(100)
private val DOZE = BigDecimal(12)
private val FATOR_ENCOLHIMENTO = BigDecimal("0.75")
private val PVP_MINIMO_PLAUSIVEL = BigDecimal("0.30")
private val PVP_MAXIMO_PLAUSIVEL = BigDecimal("3.00")

/** 10 pp abaixo do IFIX em 12 meses separa o problema do fundo do movimento do setor. */
private val DIFERENCA_ESPECIFICA = BigDecimal(10)
