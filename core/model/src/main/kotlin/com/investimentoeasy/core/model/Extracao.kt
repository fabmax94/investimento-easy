package com.investimentoeasy.core.model

import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

/**
 * Resultado bruto da leitura de um arquivo de carteira, antes de classificação e validação.
 *
 * Dados sensíveis (número de conta, nome do assessor) nunca fazem parte deste modelo (R7).
 */
public data class ExtracaoCarteira(
    val formato: FormatoArquivo,
    val metodo: MetodoExtracao,
    val dataReferencia: LocalDate?,
    val resumo: ResumoCarteira?,
    val referencias: List<IndiceReferencia>,
    val estrategias: List<EstrategiaExtraida>,
    val posicoes: List<PosicaoExtraida>,
    val rentabilidadeMensal: List<RentabilidadeMensal>,
    val evolucaoMensal: List<EvolucaoMensal>,
    val movimentacoes: Movimentacoes,
    val avisos: List<AvisoExtracao>,
)

public enum class FormatoArquivo { XPERFORMANCE_PDF, XP_XLSX, GENERICO }

public data class ResumoCarteira(
    val patrimonioTotalBruto: Money,
    val rentabilidadeMes: Percent?,
    val ganhoMes: Money?,
    val rentabilidade24Meses: Percent?,
    val ganho24Meses: Money?,
)

/** Linha da tabela de referências (Portfólio, CDI, Ibovespa, IPCA, Dólar). */
public data class IndiceReferencia(
    val nome: String,
    val mes: Percent?,
    val ano: Percent?,
    val dozeMeses: Percent?,
    val vinteQuatroMeses: Percent?,
)

public data class EstrategiaExtraida(
    val nome: String,
    val saldo: Money,
    val percentualAlocacao: Percent?,
)

public data class PosicaoExtraida(
    val nome: String,
    val estrategia: String,
    val saldo: Money,
    val quantidade: BigDecimal?,
    val percentualAlocacao: Percent?,
    val rentabilidadeMes: Percent?,
    val percentualCdiMes: Percent?,
    val rentabilidadeAno: Percent?,
    val percentualCdiAno: Percent?,
    val rentabilidade24Meses: Percent?,
    val percentualCdi24Meses: Percent?,
)

public data class RentabilidadeMensal(
    val mes: YearMonth,
    val portfolio: Percent?,
    val percentualCdi: Percent?,
)

public data class EvolucaoMensal(
    val mes: YearMonth,
    val patrimonioInicial: Money,
    val movimentacoes: Money,
    val ir: Money,
    val iof: Money,
    val patrimonioFinal: Money,
    val ganhoFinanceiro: Money,
    val rentabilidade: Percent?,
    val percentualCdi: Percent?,
)

public sealed interface Movimentacoes {
    /** O arquivo não traz a seção de movimentações. */
    public data object NaoInformadas : Movimentacoes

    /** O arquivo declara que não houve movimentações no período. */
    public data object Nenhuma : Movimentacoes

    /** Há movimentações num formato que o parser ainda não interpreta; não são descartadas em silêncio. */
    public data class NaoInterpretadas(val quantidadeLinhas: Int) : Movimentacoes
}

public data class AvisoExtracao(
    val codigo: CodigoAviso,
    val mensagem: String,
    val pagina: Int? = null,
)

public enum class CodigoAviso {
    LINHA_NAO_INTERPRETADA,
    SUBTOTAL_ESTRATEGIA_DIVERGENTE,
    ESTRATEGIA_DESCONHECIDA,
    SECAO_AUSENTE,
    MOVIMENTACOES_NAO_INTERPRETADAS,
}
