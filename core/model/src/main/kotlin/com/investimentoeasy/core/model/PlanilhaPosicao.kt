package com.investimentoeasy.core.model

import java.math.BigDecimal
import java.time.LocalDate

/**
 * Leitura da planilha "Posição Detalhada" da XP. Ela não substitui o PDF (não traz índices nem série
 * mensal): complementa a base com o que o PDF não tem, como preço médio, valor aplicado, taxa e datas
 * da renda fixa e proventos previstos.
 *
 * Número de conta, nome do titular e código/nome do assessor nunca fazem parte deste modelo (R7).
 */
public data class PlanilhaPosicao(
    /** Data em que a XP gerou a planilha; os valores valem para esse dia. */
    val dataPosicao: LocalDate?,
    val patrimonio: Money?,
    val itens: List<ItemPlanilha>,
    val proventos: List<ProventoPrevisto>,
    val avisos: List<AvisoExtracao>,
)

/** Grupo da planilha de onde veio o item (define como casar com a base). */
public enum class SecaoPlanilha { FUNDOS, ACOES, RENDA_FIXA, FUNDOS_IMOBILIARIOS, OUTRA }

/**
 * Uma linha de posição da planilha, só com o que ela traz. Campos ausentes na planilha (ou marcados
 * como "-" / "Indefinido") ficam nulos: regra zero, nunca zero implícito.
 */
public data class ItemPlanilha(
    val nome: String,
    val secao: SecaoPlanilha,
    /** Subgrupo como a planilha escreve (ex.: "Pós-Fixado", "Fundos Listados"). */
    val subgrupo: String,
    val saldo: Money,
    val quantidade: BigDecimal? = null,
    /** Quanto foi aplicado (fundos e renda fixa: valor aplicado original). */
    val valorAplicado: Money? = null,
    /** Preço médio de compra (ações, ETFs e FIIs). */
    val precoMedio: Money? = null,
    /** Rentabilidade desde a aplicação (líquida nos fundos; com proventos nos FIIs). */
    val rentabilidadeDesdeInicio: Percent? = null,
    val valorLiquido: Money? = null,
    val ir: Money? = null,
    val iof: Money? = null,
    /** Taxa como a planilha escreve (ex.: "110,00% CDI"). */
    val taxa: String? = null,
    val dataAplicacao: LocalDate? = null,
    val vencimento: LocalDate? = null,
)

public data class ProventoPrevisto(
    val ativo: String,
    /** Evento como a planilha escreve (ex.: "RENDIMENTO", "DIVIDENDO"). */
    val evento: String,
    val quantidade: BigDecimal?,
    val valorLiquido: Money,
    val dataPagamento: LocalDate?,
)
