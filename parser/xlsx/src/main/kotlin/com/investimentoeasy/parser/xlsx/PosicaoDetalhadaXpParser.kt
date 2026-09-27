package com.investimentoeasy.parser.xlsx

import com.investimentoeasy.core.model.AvisoExtracao
import com.investimentoeasy.core.model.CodigoAviso
import com.investimentoeasy.core.model.ItemPlanilha
import com.investimentoeasy.core.model.Money
import com.investimentoeasy.core.model.PlanilhaPosicao
import com.investimentoeasy.core.model.ProventoPrevisto
import com.investimentoeasy.core.model.SecaoPlanilha
import com.investimentoeasy.core.model.soma
import com.investimentoeasy.parser.xlsx.NumerosPlanilha.data
import com.investimentoeasy.parser.xlsx.NumerosPlanilha.dinheiro
import com.investimentoeasy.parser.xlsx.NumerosPlanilha.percentual
import com.investimentoeasy.parser.xlsx.NumerosPlanilha.quantidade

public sealed interface ResultadoPlanilha {
    public data class Sucesso(val planilha: PlanilhaPosicao) : ResultadoPlanilha

    /** É um .xlsx, mas não a "Posição Detalhada" da XP. */
    public data object FormatoNaoReconhecido : ResultadoPlanilha
}

/**
 * Parser determinístico da planilha "Posição Detalhada" da XP (R2).
 *
 * A planilha tem seções (Fundos, Ações, Renda Fixa, Fundos Imobiliários) com o total na coluna G,
 * e cada subgrupo (`34% | Renda Variável Global`) tem o próprio cabeçalho. As colunas são lidas pelo
 * nome do cabeçalho, não pela posição, porque mudam de uma seção para outra.
 *
 * O cabeçalho da planilha traz conta, nome do titular e assessor: só a data é aproveitada (R7).
 */
public class PosicaoDetalhadaXpParser {
    public fun reconhece(aba: Aba): Boolean {
        val textos = aba.linhas.take(LINHAS_DO_CABECALHO).flatMap { it.preenchidas.values }
        return textos.any { it.contains(MARCA_PATRIMONIO, ignoreCase = true) } && textos.any { it.equals(TOTAL_INVESTIDO, true) }
    }

    public fun parse(aba: Aba): ResultadoPlanilha {
        if (!reconhece(aba)) return ResultadoPlanilha.FormatoNaoReconhecido
        return ResultadoPlanilha.Sucesso(Leitura(aba.linhas).ler())
    }

    private class Leitura(
        private val linhas: List<LinhaPlanilha>,
    ) {
        private val avisos = mutableListOf<AvisoExtracao>()
        private val itens = mutableListOf<ItemPlanilha>()
        private val proventos = mutableListOf<ProventoPrevisto>()
        private val totaisDasSecoes = mutableListOf<Pair<SecaoPlanilha, Money>>()

        private var modo = Modo.POSICOES
        private var secao: SecaoPlanilha? = null
        private var subgrupo: String? = null
        private var colunas: Map<String, Int> = emptyMap()

        fun ler(): PlanilhaPosicao {
            linhas.forEach(::linha)
            conferirTotais()
            return PlanilhaPosicao(
                dataPosicao = dataDaPosicao(linhas),
                patrimonio = patrimonio(linhas),
                itens = itens,
                proventos = proventos,
                avisos = avisos,
            )
        }

        private fun linha(linha: LinhaPlanilha) {
            val celulas = linha.preenchidas
            val primeira = celulas[0] ?: return
            when {
                primeira.startsWith(TITULO_DISTRIBUICOES, true) || primeira.equals(TITULO_PROVENTOS, true) -> mudarPara(Modo.PROVENTOS)
                primeira.equals(TITULO_CUSTODIA, true) -> mudarPara(Modo.IGNORAR)
                celulas.keys == setOf(0, COLUNA_TOTAL) && dinheiro(celulas[COLUNA_TOTAL]) != null -> abrirSecao(primeira, celulas)
                SUBGRUPO.matches(primeira) -> abrirSubgrupo(primeira, celulas)
                subgrupo != null && modo == Modo.POSICOES -> item(linha)
                subgrupo != null && modo == Modo.PROVENTOS -> provento(linha)
            }
        }

        private fun mudarPara(novo: Modo) {
            modo = novo
            secao = null
            subgrupo = null
        }

        private fun abrirSecao(
            titulo: String,
            celulas: Map<Int, String>,
        ) {
            subgrupo = null
            val nova = SECOES[titulo.lowercase()]
            secao = nova ?: SecaoPlanilha.OUTRA
            if (modo != Modo.POSICOES) return
            if (nova == null) avisos += AvisoExtracao(CodigoAviso.ESTRATEGIA_DESCONHECIDA, "Seção da planilha desconhecida: $titulo")
            dinheiro(celulas[COLUNA_TOTAL])?.let { totaisDasSecoes += (nova ?: SecaoPlanilha.OUTRA) to it }
        }

        private fun abrirSubgrupo(
            titulo: String,
            celulas: Map<Int, String>,
        ) {
            subgrupo = SUBGRUPO.find(titulo)!!.groupValues[1].trim()
            colunas = celulas.filterKeys { it != 0 }.entries.associate { (coluna, nome) -> nome.lowercase() to coluna }
        }

        private fun item(linha: LinhaPlanilha) {
            val nome = linha.preenchidas.getValue(0)
            val saldo = dinheiro(valor(linha, "posição", "posição a mercado", "valor total"))
            if (saldo == null) {
                avisos += AvisoExtracao(CodigoAviso.LINHA_NAO_INTERPRETADA, "Linha ${linha.numero} sem posição: $nome")
                return
            }
            itens +=
                ItemPlanilha(
                    nome = nome,
                    secao = secao ?: SecaoPlanilha.OUTRA,
                    subgrupo = subgrupo!!,
                    saldo = saldo,
                    quantidade = quantidade(valor(linha, "qtd. total", "quantidade de cotas", "quantidade")),
                    valorAplicado = dinheiro(valor(linha, "valor aplicado original", "valor aplicado")),
                    precoMedio = dinheiro(valor(linha, "preço médio", "preço médio (abertura)")),
                    rentabilidadeDesdeInicio =
                        percentual(valor(linha, "rentabilidade líquida", "rentabilidade c/ proventos", "rentabilidade (%)")),
                    valorLiquido = dinheiro(valor(linha, "valor líquido")),
                    ir = dinheiro(valor(linha, "ir")),
                    iof = dinheiro(valor(linha, "iof")),
                    taxa = valor(linha, "taxa a mercado", "taxa")?.takeUnless(NumerosPlanilha::ausente),
                    dataAplicacao = data(valor(linha, "data aplicação")),
                    vencimento = data(valor(linha, "data vencimento")),
                )
        }

        private fun provento(linha: LinhaPlanilha) {
            val ativo = linha.preenchidas.getValue(0)
            val liquido = dinheiro(valor(linha, "valor provisionado líquido", "valor provisionado bruto"))
            val evento = valor(linha, "evento")
            if (liquido == null || evento == null) {
                avisos += AvisoExtracao(CodigoAviso.LINHA_NAO_INTERPRETADA, "Provento da linha ${linha.numero} incompleto: $ativo")
                return
            }
            proventos +=
                ProventoPrevisto(
                    ativo = ativo,
                    evento = evento,
                    quantidade = quantidade(valor(linha, "provisionado")),
                    valorLiquido = liquido,
                    dataPagamento = data(valor(linha, "previsão pagamento")),
                )
        }

        /** Primeiro cabeçalho encontrado entre os [nomes] (a ordem é a preferência). */
        private fun valor(
            linha: LinhaPlanilha,
            vararg nomes: String,
        ): String? = nomes.firstNotNullOfOrNull { colunas[it] }?.let { linha.preenchidas[it] }

        /** A soma das linhas de cada seção deve bater com o total que a planilha mostra. */
        private fun conferirTotais() {
            totaisDasSecoes.groupBy({ it.first }, { it.second }).forEach { (secao, totais) ->
                val esperado = totais.soma()
                val lido = itens.filter { it.secao == secao }.map { it.saldo }.soma()
                if ((esperado - lido).abs() > TOLERANCIA) {
                    avisos +=
                        AvisoExtracao(
                            CodigoAviso.SUBTOTAL_ESTRATEGIA_DIVERGENTE,
                            "Seção $secao: a planilha mostra $esperado e as linhas somam $lido",
                        )
                }
            }
        }
    }

    private enum class Modo { POSICOES, PROVENTOS, IGNORAR }

    private companion object {
        const val COLUNA_TOTAL = 6
        const val TOTAL_INVESTIDO = "Total investido"
        const val TITULO_DISTRIBUICOES = "Dividendos, proventos"
        const val TITULO_PROVENTOS = "Proventos"
        const val TITULO_CUSTODIA = "Custódia Remunerada"
        val TOLERANCIA: Money = Money.of("0.05")
        val SUBGRUPO = Regex("""^-?[\d.,]+%\s*\|\s*(.+)$""")
        val SECOES =
            mapOf(
                "fundos de investimentos" to SecaoPlanilha.FUNDOS,
                "fundos de investimento" to SecaoPlanilha.FUNDOS,
                "ações" to SecaoPlanilha.ACOES,
                "renda fixa" to SecaoPlanilha.RENDA_FIXA,
                "fundos imobiliários" to SecaoPlanilha.FUNDOS_IMOBILIARIOS,
            )
    }
}

/** Só a data: o resto da célula (conta, horário) é descartado (R7). */
private fun dataDaPosicao(linhas: List<LinhaPlanilha>) =
    linhas
        .take(LINHAS_DO_CABECALHO)
        .flatMap { it.preenchidas.values }
        .firstNotNullOfOrNull { DATA_NO_TEXTO.find(it)?.value }
        ?.let(::data)

/** O valor fica na linha de baixo do título "…, este é o seu patrimônio" (o nome do titular é ignorado). */
private fun patrimonio(linhas: List<LinhaPlanilha>): Money? {
    val indice = linhas.indexOfFirst { l -> l.preenchidas.values.any { it.contains(MARCA_PATRIMONIO, true) } }
    val coluna = linhas.getOrNull(indice)?.preenchidas?.entries?.first { it.value.contains(MARCA_PATRIMONIO, true) }?.key
    return coluna?.let { dinheiro(linhas.getOrNull(indice + 1)?.preenchidas?.get(it)) }
}

private const val LINHAS_DO_CABECALHO = 6
private const val MARCA_PATRIMONIO = "este é o seu patrimônio"
private val DATA_NO_TEXTO = Regex("""\d{2}/\d{2}/\d{4}""")

/**
 * Do arquivo à leitura: abre o .xlsx e interpreta a primeira aba.
 *
 * @throws PlanilhaIlegivelException se não for um .xlsx legível.
 */
public fun PosicaoDetalhadaXpParser.parse(
    conteudo: ByteArray,
    leitor: LeitorXlsx = LeitorXlsx(),
): ResultadoPlanilha = parse(leitor.primeiraAba(conteudo))
