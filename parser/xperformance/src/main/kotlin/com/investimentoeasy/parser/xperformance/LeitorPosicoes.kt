package com.investimentoeasy.parser.xperformance

import com.investimentoeasy.core.model.AvisoExtracao
import com.investimentoeasy.core.model.CodigoAviso
import com.investimentoeasy.core.model.EstrategiaExtraida
import com.investimentoeasy.core.model.PosicaoExtraida
import com.investimentoeasy.core.model.soma
import com.investimentoeasy.parser.xperformance.NumerosBr.DINHEIRO
import com.investimentoeasy.parser.xperformance.NumerosBr.PERCENTUAL
import com.investimentoeasy.parser.xperformance.NumerosBr.QUANTIDADE

/**
 * Lê a seção "Posição detalhada dos ativos", que pode ocupar várias páginas.
 *
 * Cada linha é `<nome> <saldo> <qtd> <%aloc> <rent mês> <%CDI> <rent ano> <%CDI> <rent 24M> <%CDI>`.
 * Linhas com quantidade `-` são subtotais de estratégia. Nomes longos quebram numa segunda
 * linha, logo abaixo da linha com os números.
 */
internal class LeitorPosicoes(
    private val avisos: MutableList<AvisoExtracao>,
) {
    class Resultado(
        val estrategias: List<EstrategiaExtraida>,
        val posicoes: List<PosicaoExtraida>,
    )

    private val estrategias = mutableListOf<EstrategiaExtraida>()
    private val posicoes = mutableListOf<PosicaoExtraida>()
    private var estrategiaAtual: String? = null

    /** Índice da última posição lida, para anexar o nome quebrado em duas linhas. */
    private var ultimaPosicao: Int? = null

    fun ler(paginas: List<List<String>>): Resultado {
        paginas.forEachIndexed { indice, linhas ->
            if (linhas.any { XPerformanceParser.compactar(it) == SECAO }) lerPagina(linhas, pagina = indice + 1)
        }
        conferirSubtotais()
        return Resultado(estrategias.toList(), posicoes.toList())
    }

    private fun lerPagina(
        linhas: List<String>,
        pagina: Int,
    ) {
        ultimaPosicao = null
        for (linha in linhas.takeWhile { !ehFimDaSecao(it) }) {
            if (ehCabecalho(linha)) continue
            val m = LINHA.matchEntire(linha)
            when {
                m != null && m.grupo("qtd") == "-" -> lerEstrategia(m)
                m != null -> lerPosicao(m, pagina)
                ultimaPosicao != null -> anexarAoNome(linha)
                else -> avisos += AvisoExtracao(CodigoAviso.LINHA_NAO_INTERPRETADA, "Linha não interpretada: \"$linha\"", pagina)
            }
        }
    }

    private fun lerEstrategia(m: MatchResult) {
        val nome = m.grupo("nome")
        ultimaPosicao = null
        estrategiaAtual = nome
        if (nome !in XPerformanceParser.ESTRATEGIAS_CONHECIDAS) {
            avisos += AvisoExtracao(CodigoAviso.ESTRATEGIA_DESCONHECIDA, "Estratégia não mapeada: $nome")
        }
        estrategias +=
            EstrategiaExtraida(
                nome = nome,
                saldo = NumerosBr.dinheiro(m.grupo("saldo")),
                percentualAlocacao = NumerosBr.percentual(m.grupo("aloc")),
            )
    }

    private fun lerPosicao(
        m: MatchResult,
        pagina: Int,
    ) {
        val estrategia = estrategiaAtual
        if (estrategia == null) {
            avisos += AvisoExtracao(CodigoAviso.LINHA_NAO_INTERPRETADA, "Posição antes de qualquer estratégia: \"${m.value}\"", pagina)
            return
        }
        posicoes +=
            PosicaoExtraida(
                nome = m.grupo("nome"),
                estrategia = estrategia,
                saldo = NumerosBr.dinheiro(m.grupo("saldo")),
                quantidade = NumerosBr.quantidade(m.grupo("qtd")),
                percentualAlocacao = NumerosBr.percentual(m.grupo("aloc")),
                rentabilidadeMes = NumerosBr.percentual(m.grupo("rentMes")),
                percentualCdiMes = NumerosBr.percentual(m.grupo("cdiMes")),
                rentabilidadeAno = NumerosBr.percentual(m.grupo("rentAno")),
                percentualCdiAno = NumerosBr.percentual(m.grupo("cdiAno")),
                rentabilidade24Meses = NumerosBr.percentual(m.grupo("rent24")),
                percentualCdi24Meses = NumerosBr.percentual(m.grupo("cdi24")),
            )
        ultimaPosicao = posicoes.lastIndex
    }

    private fun anexarAoNome(continuacao: String) {
        val indice = ultimaPosicao ?: return
        val anterior = posicoes[indice]
        posicoes[indice] = anterior.copy(nome = "${anterior.nome} $continuacao")
    }

    /** Autoverificação: a soma das posições de cada estratégia deve bater com o subtotal informado. */
    private fun conferirSubtotais() {
        for (estrategia in estrategias) {
            val soma = posicoes.filter { it.estrategia == estrategia.nome }.map { it.saldo }.soma()
            if (soma != estrategia.saldo) {
                avisos +=
                    AvisoExtracao(
                        CodigoAviso.SUBTOTAL_ESTRATEGIA_DIVERGENTE,
                        "Estratégia ${estrategia.nome}: subtotal ${estrategia.saldo}, soma das posições $soma",
                    )
            }
        }
    }

    private fun ehCabecalho(linha: String): Boolean {
        val compacta = XPerformanceParser.compactar(linha)
        return XPerformanceParser.ehRodape(linha) ||
            compacta == SECAO ||
            compacta == "POSIÇÃODETALHADA" ||
            compacta.startsWith("PRECIFICAÇÃODERENDAFIXA") ||
            compacta.startsWith("MÊSATUAL") ||
            linha.startsWith("Estratégias Saldo Bruto")
    }

    private fun ehFimDaSecao(linha: String): Boolean = linha == "Legenda" || linha.startsWith("¹")

    private companion object {
        const val SECAO = "POSIÇÃODETALHADADOSATIVOS"
        val LINHA =
            Regex(
                """(?<nome>.+?) (?<saldo>$DINHEIRO) (?<qtd>$QUANTIDADE) (?<aloc>$PERCENTUAL) """ +
                    """(?<rentMes>$PERCENTUAL) (?<cdiMes>$PERCENTUAL) (?<rentAno>$PERCENTUAL) (?<cdiAno>$PERCENTUAL) """ +
                    """(?<rent24>$PERCENTUAL) (?<cdi24>$PERCENTUAL)""",
            )
    }
}
