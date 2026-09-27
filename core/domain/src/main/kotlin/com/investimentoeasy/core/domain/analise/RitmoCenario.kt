package com.investimentoeasy.core.domain.analise

import com.investimentoeasy.core.domain.classificacao.ClassificadorAtivos
import com.investimentoeasy.core.domain.classificacao.FIIS_LAJE_OU_AGENCIA
import com.investimentoeasy.core.domain.mercado.CicloJuros
import com.investimentoeasy.core.domain.mercado.DirecaoCambio
import com.investimentoeasy.core.domain.mercado.LeituraDeCambio
import com.investimentoeasy.core.domain.mercado.LeituraDeJuros
import com.investimentoeasy.core.model.Ativo
import com.investimentoeasy.core.model.ChaveAtivo
import com.investimentoeasy.core.model.ClasseAtivo
import com.investimentoeasy.core.model.Percent
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import kotlin.math.pow

/** Momentum observado (não é previsão de preço). */
public enum class Ritmo { ACELERANDO, ESTAVEL, DESACELERANDO, SEM_BASE }

public data class RitmoAtivo(
    val ritmo: Ritmo,
    /** Retorno do ano anualizado menos o de 24 meses anualizado, em pontos percentuais ao ano. */
    val deltaAoAno: Percent?,
)

/**
 * Ritmo da skill: `anual(ano, meses decorridos) - anual(24M, 24)`; acima de +3 pp acelera,
 * abaixo de -3 pp desacelera. Ano igual a 24M significa posição nova demais para comparar.
 * Sem um dos retornos, não há ritmo (`null`), nunca um valor inventado.
 */
public fun ritmo(
    rentabilidadeAno: Percent?,
    rentabilidade24Meses: Percent?,
    dataReferencia: LocalDate,
    limite: Percent = Percent.of("3"),
): RitmoAtivo? {
    if (rentabilidadeAno == null || rentabilidade24Meses == null) return null
    val meses = mesesDecorridosNoAno(dataReferencia)
    if (rentabilidadeAno == rentabilidade24Meses || meses < MESES_MINIMOS) return RitmoAtivo(Ritmo.SEM_BASE, null)
    val delta = anualizar(rentabilidadeAno, meses) - anualizar(rentabilidade24Meses, MESES_24)
    val deltaPercent = Percent.of(BigDecimal(delta).setScale(1, RoundingMode.HALF_EVEN))
    val classificacao =
        when {
            deltaPercent > limite -> Ritmo.ACELERANDO
            deltaPercent < Percent.ZERO - limite -> Ritmo.DESACELERANDO
            else -> Ritmo.ESTAVEL
        }
    return RitmoAtivo(classificacao, deltaPercent)
}

/** Meses desde 31/12 do ano anterior até a data de referência (03/09 → 8,1). */
public fun mesesDecorridosNoAno(dataReferencia: LocalDate): Double = dataReferencia.dayOfYear * MESES_NO_ANO / dataReferencia.lengthOfYear()

// Anualização é derivação de percentual (não dinheiro): Double basta para classificar em pontos.
private fun anualizar(
    retorno: Percent,
    meses: Double,
): Double = ((1 + retorno.pontos.toDouble() / CEM).pow(MESES_NO_ANO / meses) - 1) * CEM

private const val MESES_NO_ANO = 12.0
private const val MESES_24 = 24.0
private const val CEM = 100.0

/** Com menos de um mês no ano, a anualização explode ruído: sem base para comparar. */
private const val MESES_MINIMOS = 1.0

/** Sensibilidade ao ciclo de juros ("se o corte de juros continuar..."), nunca "vai subir". */
public enum class Cenario { FAVORAVEL, ADVERSO, NEUTRO, INDEFINIDO }

public data class CenarioAtivo(
    val cenario: Cenario,
    val driver: String,
)

/**
 * Tabela de cenários da skill `portfolio-analysis`, por classe e tipo de ativo, ajustada ao ciclo
 * de juros que o Focus indica. Sem dados de mercado, vale a leitura condicional da skill
 * ("se o corte continuar"). Nunca é previsão de preço.
 */
public fun cenario(
    ativo: Ativo,
    juros: LeituraDeJuros? = null,
    cambio: LeituraDeCambio? = null,
): CenarioAtivo {
    val nome = ClassificadorAtivos.normalizar(ativo.nome)
    val raiz = (ativo.chave as? ChaveAtivo.Ticker)?.codigo?.take(RAIZ_TICKER)
    return when {
        "OURO" in nome || nome.startsWith("GOLD") -> CenarioAtivo(Cenario.NEUTRO, "hedge, não segue o ciclo local")
        ativo.classe == ClasseAtivo.RV_GLOBAL -> cenarioGlobal(cambio)
        ativo.classe == ClasseAtivo.FII_PAPEL ->
            CenarioAtivo(Cenario.INDEFINIDO, "depende do indexador dos CRIs: CDI e IPCA reagem ao juro em sentidos opostos")
        else -> cenarioLocal(ativo.classe, raiz in FIIS_LAJE_OU_AGENCIA, juros?.ciclo ?: CicloJuros.CORTE)
    }
}

private fun cenarioLocal(
    classe: ClasseAtivo,
    lajeOuAgencia: Boolean,
    ciclo: CicloJuros,
): CenarioAtivo {
    val linha = TABELA[classe] ?: return CenarioAtivo(Cenario.INDEFINIDO, "sem regra de cenário para esta classe")
    val (cenarioCorte, driverCorte) = if (classe == ClasseAtivo.FII_TIJOLO && lajeOuAgencia) LAJE else linha.corte
    return when (ciclo) {
        CicloJuros.CORTE -> CenarioAtivo(cenarioCorte, driverCorte)
        CicloJuros.ALTA -> CenarioAtivo(linha.alta.first, linha.alta.second)
        CicloJuros.ESTAVEL -> CenarioAtivo(Cenario.NEUTRO, linha.estavel)
    }
}

private fun cenarioGlobal(cambio: LeituraDeCambio?): CenarioAtivo =
    when (cambio?.direcao) {
        null -> CenarioAtivo(Cenario.ADVERSO, "real forte corta o retorno em BRL (sem hedge cambial)")
        DirecaoCambio.REAL_MAIS_FRACO -> CenarioAtivo(Cenario.FAVORAVEL, "Focus espera dólar ↑: ajuda o retorno em BRL")
        DirecaoCambio.REAL_MAIS_FORTE -> CenarioAtivo(Cenario.ADVERSO, "Focus espera dólar ↓: corta o retorno em BRL")
        DirecaoCambio.ESTAVEL -> CenarioAtivo(Cenario.NEUTRO, "Focus espera dólar estável: vale o ativo lá fora")
    }

private data class LinhaCenario(
    val corte: Pair<Cenario, String>,
    val alta: Pair<Cenario, String>,
    val estavel: String,
)

private val ACOES =
    LinhaCenario(
        Cenario.FAVORAVEL to "Selic ↓ e ciclo eleitoral",
        Cenario.ADVERSO to "juro ↑ encarece o capital e derruba múltiplos",
        "juro estável: sem gatilho de juros",
    )

private val TABELA =
    mapOf(
        ClasseAtivo.RF_POS to
            LinhaCenario(
                Cenario.ADVERSO to "Selic ↓ reduz o carrego",
                Cenario.FAVORAVEL to "Selic ↑ aumenta o carrego",
                "carrego mantido enquanto a Selic não muda",
            ),
        ClasseAtivo.RF_IPCA to
            LinhaCenario(
                Cenario.FAVORAVEL to "ganha marcação com juro ↓",
                Cenario.ADVERSO to "perde marcação com juro ↑",
                "sem ganho de marcação; rende IPCA + taxa",
            ),
        ClasseAtivo.RF_PRE to
            LinhaCenario(
                Cenario.FAVORAVEL to "trava a taxa antes do corte",
                Cenario.ADVERSO to "perde marcação com juro ↑",
                "rende a taxa contratada",
            ),
        ClasseAtivo.FII_TIJOLO to
            LinhaCenario(
                Cenario.FAVORAVEL to "Selic ↓ reprecifica",
                Cenario.ADVERSO to "juro ↑ pressiona as cotas",
                "juro estável: vale a renda dos imóveis",
            ),
        ClasseAtivo.ACAO to ACOES,
        ClasseAtivo.ETF to ACOES,
        ClasseAtivo.FUNDO_ACOES to ACOES,
    )

private val LAJE = Cenario.NEUTRO to "juro ajuda, vacância estrutural não"

private const val RAIZ_TICKER = 4
