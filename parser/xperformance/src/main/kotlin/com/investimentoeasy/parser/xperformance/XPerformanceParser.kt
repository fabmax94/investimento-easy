package com.investimentoeasy.parser.xperformance

import com.investimentoeasy.core.model.AvisoExtracao
import com.investimentoeasy.core.model.CodigoAviso
import com.investimentoeasy.core.model.EstrategiaExtraida
import com.investimentoeasy.core.model.EvolucaoMensal
import com.investimentoeasy.core.model.ExtracaoCarteira
import com.investimentoeasy.core.model.FormatoArquivo
import com.investimentoeasy.core.model.IndiceReferencia
import com.investimentoeasy.core.model.MetodoExtracao
import com.investimentoeasy.core.model.Movimentacoes
import com.investimentoeasy.core.model.RentabilidadeMensal
import com.investimentoeasy.core.model.ResumoCarteira
import com.investimentoeasy.parser.xperformance.NumerosBr.DINHEIRO
import com.investimentoeasy.parser.xperformance.NumerosBr.PERCENTUAL
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

public sealed interface ResultadoParse {
    public data class Sucesso(val extracao: ExtracaoCarteira) : ResultadoParse

    public data object FormatoNaoReconhecido : ResultadoParse
}

/**
 * Parser determinístico do relatório XPerformance da XP (R2).
 *
 * Recebe o texto de cada página (na ordem do PDF, extraído com ordenação por posição) e
 * localiza as seções pelo título, não pelo número da página: a ordem das páginas muda
 * conforme o tamanho da carteira.
 *
 * Número de conta e nome do assessor aparecem no cabeçalho do relatório e são
 * deliberadamente ignorados (R7).
 */
public class XPerformanceParser {
    public fun reconhece(paginas: List<String>): Boolean {
        val primeira = paginas.firstOrNull()?.let(::compactar).orEmpty()
        return primeira.contains("RelatóriodeInvestimentos") && paginas.any { compactar(it).contains("XPerformance") }
    }

    public fun parse(paginas: List<String>): ResultadoParse {
        if (!reconhece(paginas)) return ResultadoParse.FormatoNaoReconhecido
        val avisos = mutableListOf<AvisoExtracao>()
        val linhasPorPagina = paginas.map { pagina -> pagina.lines().map(::normalizarLinha).filter(String::isNotEmpty) }
        val todasAsLinhas = linhasPorPagina.flatten()

        val posicoes = LeitorPosicoes(avisos).ler(linhasPorPagina)
        val resumo = lerResumo(linhasPorPagina)
        if (resumo == null) avisos += AvisoExtracao(CodigoAviso.SECAO_AUSENTE, "Patrimônio total bruto não encontrado")
        val movimentacoes = lerMovimentacoes(linhasPorPagina, avisos)

        val extracao =
            ExtracaoCarteira(
                formato = FormatoArquivo.XPERFORMANCE_PDF,
                metodo = MetodoExtracao.PARSER,
                dataReferencia = lerDataReferencia(todasAsLinhas),
                resumo = resumo,
                referencias = lerReferencias(linhasPorPagina),
                estrategias = posicoes.estrategias.ifEmpty { lerComposicao(todasAsLinhas) },
                posicoes = posicoes.posicoes,
                rentabilidadeMensal = linhasPorPagina.flatMap(::lerRentabilidadeMensal).sortedBy { it.mes },
                evolucaoMensal = todasAsLinhas.mapNotNull(::lerEvolucaoMensal).sortedBy { it.mes },
                movimentacoes = movimentacoes,
                avisos = avisos,
            )
        return ResultadoParse.Sucesso(extracao)
    }

    private fun lerDataReferencia(linhas: List<String>): LocalDate? =
        linhas.firstNotNullOfOrNull { RODAPE_DATA.find(it) }?.let { LocalDate.parse(it.grupo("data"), DATA_BR) }

    private fun lerResumo(paginas: List<List<String>>): ResumoCarteira? =
        paginas.firstNotNullOfOrNull { linhas ->
            val titulo = linhas.indexOfFirst { it.startsWith("PATRIMÔNIO TOTAL BRUTO") }
            linhas.getOrNull(titulo + 1)?.takeIf { titulo >= 0 }?.let(RESUMO::matchEntire)
        }?.let { m ->
            ResumoCarteira(
                patrimonioTotalBruto = NumerosBr.dinheiro(m.grupo("patrimonio")),
                rentabilidadeMes = NumerosBr.percentual(m.grupo("rentMes")),
                ganhoMes = NumerosBr.dinheiro(m.grupo("ganhoMes")),
                rentabilidade24Meses = NumerosBr.percentual(m.grupo("rent24")),
                ganho24Meses = NumerosBr.dinheiro(m.grupo("ganho24")),
            )
        }

    private fun lerReferencias(paginas: List<List<String>>): List<IndiceReferencia> {
        val pagina = paginas.firstOrNull { linhas -> linhas.any { it.startsWith("Referências (%)") } } ?: return emptyList()
        return pagina.mapNotNull(REFERENCIA::matchEntire).map { m ->
            IndiceReferencia(
                nome = m.grupo("nome"),
                mes = NumerosBr.percentual(m.grupo("mes")),
                ano = NumerosBr.percentual(m.grupo("ano")),
                dozeMeses = NumerosBr.percentual(m.grupo("doze")),
                vinteQuatroMeses = NumerosBr.percentual(m.grupo("vinteQuatro")),
            )
        }
    }

    private fun lerComposicao(linhas: List<String>): List<EstrategiaExtraida> =
        linhas
            .flatMap { linha -> COMPOSICAO.findAll(linha).toList() }
            .map { m ->
                EstrategiaExtraida(
                    nome = m.grupo("nome"),
                    saldo = NumerosBr.dinheiro(m.grupo("saldo")),
                    percentualAlocacao = NumerosBr.percentual(m.grupo("pct")),
                )
            }.distinctBy { it.nome }

    /**
     * A tabela anual vem em trios de linhas: "Portfólio <12 meses> <ano> <acum.>", o ano e
     * "%CDI <12 meses> <ano> <acum.>".
     */
    private fun lerRentabilidadeMensal(linhas: List<String>): List<RentabilidadeMensal> =
        linhas.indices.flatMap { i ->
            val portfolio = SERIE_PORTFOLIO.matchEntire(linhas[i])
            val ano = linhas.getOrNull(i + 1)?.takeIf { ANO.matches(it) }?.toInt()
            if (portfolio == null || ano == null) return@flatMap emptyList()
            val valoresPortfolio = portfolio.grupo("valores").split(" ")
            val valoresCdi = linhas.getOrNull(i + 2)?.let(SERIE_CDI::matchEntire)?.grupo("valores")?.split(" ")
            (1..MESES_NO_ANO).mapNotNull { mes ->
                val rent = NumerosBr.percentual(valoresPortfolio[mes - 1])
                val rentCdi = valoresCdi?.getOrNull(mes - 1)?.let(NumerosBr::percentual)
                if (rent == null && rentCdi == null) null else RentabilidadeMensal(YearMonth.of(ano, mes), rent, rentCdi)
            }
        }

    private fun lerEvolucaoMensal(linha: String): EvolucaoMensal? =
        EVOLUCAO.matchEntire(linha)?.let { m ->
            EvolucaoMensal(
                mes = YearMonth.of(SECULO + m.grupo("ano").toInt(), MESES_ABREVIADOS.indexOf(m.grupo("mes")) + 1),
                patrimonioInicial = NumerosBr.dinheiro(m.grupo("inicial")),
                movimentacoes = NumerosBr.dinheiro(m.grupo("mov")),
                ir = NumerosBr.dinheiro(m.grupo("ir")),
                iof = NumerosBr.dinheiro(m.grupo("iof")),
                patrimonioFinal = NumerosBr.dinheiro(m.grupo("final")),
                ganhoFinanceiro = NumerosBr.dinheiro(m.grupo("ganho")),
                rentabilidade = NumerosBr.percentual(m.grupo("rent")),
                percentualCdi = NumerosBr.percentual(m.grupo("cdi")),
            )
        }

    private fun lerMovimentacoes(
        paginas: List<List<String>>,
        avisos: MutableList<AvisoExtracao>,
    ): Movimentacoes {
        val indice = paginas.indexOfFirst { linhas -> linhas.any { compactar(it) == "MOVIMENTAÇÕESDOMÊS" } }
        val linhas = paginas.getOrNull(indice) ?: return Movimentacoes.NaoInformadas
        return if (linhas.any { it.startsWith("Não existem movimentações") }) {
            Movimentacoes.Nenhuma
        } else {
            val conteudo = linhas.filterNot { ehRodape(it) || compactar(it).startsWith("MOVIMENTAÇÕES") }
            avisos +=
                AvisoExtracao(
                    CodigoAviso.MOVIMENTACOES_NAO_INTERPRETADAS,
                    "Movimentações presentes, mas o formato ainda não é interpretado; aportes e resgates serão inferidos",
                    pagina = indice + 1,
                )
            Movimentacoes.NaoInterpretadas(conteudo.size)
        }
    }

    internal companion object {
        private const val MESES_NO_ANO = 12
        private const val SECULO = 2000
        private val DATA_BR: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        private val MESES_ABREVIADOS = listOf("jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez")

        val ESTRATEGIAS_CONHECIDAS: Set<String> =
            setOf(
                "Pós Fixado",
                "Pré Fixado",
                "Inflação",
                "Multimercado",
                "Renda Variável Brasil",
                "Renda Variável Global",
                "Renda Fixa Global",
                "Alternativo",
                "Fundos Listados",
                "Caixa",
                "Proventos",
            )

        private val RODAPE_DATA = Regex("""Data de referência: (?<data>\d{2}/\d{2}/\d{4})""", RegexOption.IGNORE_CASE)
        private val RESUMO =
            Regex(
                """(?<patrimonio>$DINHEIRO) (?<rentMes>$PERCENTUAL) (?<ganhoMes>$DINHEIRO) (?<rent24>$PERCENTUAL) (?<ganho24>$DINHEIRO)""",
            )
        private val REFERENCIA =
            Regex(
                """(?<nome>Portfólio|CDI|Ibovespa|IPCA|Dólar) (?<mes>$PERCENTUAL) (?<ano>$PERCENTUAL) """ +
                    """(?<doze>$PERCENTUAL) (?<vinteQuatro>$PERCENTUAL)""",
            )
        private val COMPOSICAO =
            Regex(
                """(?<nome>${ESTRATEGIAS_CONHECIDAS.joinToString("|") { Regex.escape(it) }}) """ +
                    """\((?<pct>-?[\d.]+,\d+%)\) (?<saldo>$DINHEIRO)""",
            )
        private val SERIE_PORTFOLIO = Regex("""Portfólio (?<valores>(?:$PERCENTUAL ){13}$PERCENTUAL)""")
        private val SERIE_CDI = Regex("""%CDI (?<valores>(?:$PERCENTUAL ){13}$PERCENTUAL)""")
        private val ANO = Regex("""\d{4}""")
        private val EVOLUCAO =
            Regex(
                """(?<mes>${MESES_ABREVIADOS.joinToString("|")})\./(?<ano>\d{2}) (?<inicial>$DINHEIRO) (?<mov>$DINHEIRO) """ +
                    """(?<ir>$DINHEIRO) (?<iof>$DINHEIRO) (?<final>$DINHEIRO) (?<ganho>$DINHEIRO) """ +
                    """(?<rent>$PERCENTUAL) (?<cdi>$PERCENTUAL)""",
            )
        private val ESPACOS = Regex("""[\s   ]+""")

        /** O PDF mistura espaço comum e não separável (U+00A0); tudo vira um espaço simples. */
        fun normalizarLinha(linha: String): String = linha.replace(ESPACOS, " ").trim()

        /** Remove todo espaço: os títulos do relatório vêm letra a letra ("P O S I Ç Ã O"). */
        fun compactar(texto: String): String = texto.filterNot(Char::isWhitespace)

        fun ehRodape(linha: String): Boolean = linha.startsWith("XPerformance - Relatório")
    }
}
