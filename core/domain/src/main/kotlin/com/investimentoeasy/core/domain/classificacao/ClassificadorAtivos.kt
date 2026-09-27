package com.investimentoeasy.core.domain.classificacao

import com.investimentoeasy.core.model.Ativo
import com.investimentoeasy.core.model.ChaveAtivo
import com.investimentoeasy.core.model.ClasseAtivo
import com.investimentoeasy.core.model.Indexador
import com.investimentoeasy.core.model.Percent
import com.investimentoeasy.core.model.PosicaoExtraida
import com.investimentoeasy.core.model.TipoAtivo
import java.math.BigDecimal
import java.text.Normalizer
import java.time.YearMonth

/** O que a classificação automática não conseguiu decidir sozinha e o usuário precisa confirmar. */
public enum class Pendencia {
    CLASSE_A_CONFIRMAR,
    TIPO_AMBIGUO,
    SEGMENTO_FII_DESCONHECIDO,
    GESTORA_DESCONHECIDA,
}

public data class AtivoClassificado(
    val ativo: Ativo,
    val pendencias: Set<Pendencia> = emptySet(),
)

/**
 * Classificação automática (R5) e chave única (R4) de cada posição extraída.
 *
 * Usa a estratégia informada pela XP como principal pista e o nome/ticker do ativo para
 * refinar. Quando a informação não basta, marca uma [Pendencia] em vez de adivinhar.
 */
public class ClassificadorAtivos(
    private val catalogoFii: CatalogoFii = CatalogoFiiPadrao,
    private val etfsBrasil: Set<String> = ETFS_BRASIL_CONHECIDOS,
    private val gestoras: List<String> = GESTORAS_CONHECIDAS,
) {
    public fun classificar(posicao: PosicaoExtraida): AtivoClassificado {
        val nome = posicao.nome.trim()
        val estrategia = Estrategia.de(posicao.estrategia)
        return TICKER.matchEntire(nome)?.let { classificarTicker(nome, it.groupValues[1], it.groupValues[2], estrategia) }
            ?: CREDITO.matchEntire(nome)?.let { classificarCredito(nome, it, estrategia) }
            ?: TESOURO.find(nome)?.let { classificarTesouro(nome, it.groupValues[1]) }
            ?: classificarFundo(nome, estrategia)
    }

    private fun classificarTicker(
        ticker: String,
        raiz: String,
        sufixo: String,
        estrategia: Estrategia,
    ): AtivoClassificado {
        val (tipo, classe, pendencia) =
            when (estrategia) {
                Estrategia.FUNDOS_LISTADOS -> fii(raiz)
                Estrategia.POS_FIXADO, Estrategia.INFLACAO, Estrategia.PRE_FIXADO ->
                    Triple(TipoAtivo.ETF, estrategia.classeRendaFixa(), null)
                Estrategia.RENDA_VARIAVEL_BRASIL -> rendaVariavelBrasil(ticker, sufixo)
                Estrategia.RENDA_VARIAVEL_GLOBAL ->
                    Triple(if (sufixo in SUFIXOS_BDR) TipoAtivo.BDR else TipoAtivo.ETF, ClasseAtivo.RV_GLOBAL, null)
                else ->
                    Triple(
                        if (sufixo in SUFIXOS_ACAO) TipoAtivo.ACAO else TipoAtivo.OUTRO,
                        ClasseAtivo.NAO_CLASSIFICADO,
                        Pendencia.CLASSE_A_CONFIRMAR,
                    )
            }
        return AtivoClassificado(Ativo(ChaveAtivo.Ticker(ticker), ticker, tipo, classe), setOfNotNull(pendencia))
    }

    private fun fii(raiz: String): Triple<TipoAtivo, ClasseAtivo, Pendencia?> =
        when (catalogoFii.segmento(raiz)) {
            SegmentoFii.TIJOLO -> Triple(TipoAtivo.FII, ClasseAtivo.FII_TIJOLO, null)
            SegmentoFii.PAPEL -> Triple(TipoAtivo.FII, ClasseAtivo.FII_PAPEL, null)
            SegmentoFii.FOF -> Triple(TipoAtivo.FII, ClasseAtivo.FII_FOF, null)
            null -> Triple(TipoAtivo.FII, ClasseAtivo.FII_NAO_CLASSIFICADO, Pendencia.SEGMENTO_FII_DESCONHECIDO)
        }

    /** Ticker 11 fora da lista de ETFs pode ser unit (ação) ou ETF: fica como ação, a confirmar. */
    private fun rendaVariavelBrasil(
        ticker: String,
        sufixo: String,
    ): Triple<TipoAtivo, ClasseAtivo, Pendencia?> =
        when {
            ticker in etfsBrasil -> Triple(TipoAtivo.ETF, ClasseAtivo.ETF, null)
            sufixo in SUFIXOS_ACAO -> Triple(TipoAtivo.ACAO, ClasseAtivo.ACAO, null)
            else -> Triple(TipoAtivo.ACAO, ClasseAtivo.ACAO, Pendencia.TIPO_AMBIGUO)
        }

    private fun classificarCredito(
        nome: String,
        m: MatchResult,
        estrategia: Estrategia,
    ): AtivoClassificado {
        val tipo = tipoCredito(grupo(m, "tipo"))
        val emissor = normalizar(grupo(m, "emissor"))
        val vencimento = YearMonth.of(grupo(m, "ano").toInt(), MESES.indexOf(grupo(m, "mes").uppercase()) + 1)
        val (indexador, taxa) = lerTaxa(m.groups["taxa"]?.value.orEmpty())
        val classe =
            when (indexador) {
                Indexador.CDI, Indexador.SELIC -> ClasseAtivo.RF_POS
                Indexador.IPCA -> ClasseAtivo.RF_IPCA
                Indexador.PRE -> ClasseAtivo.RF_PRE
                null -> estrategia.classeRendaFixa()
            }
        val pendencias = setOfNotNull(Pendencia.CLASSE_A_CONFIRMAR.takeIf { classe == ClasseAtivo.NAO_CLASSIFICADO })
        val ativo =
            Ativo(
                chave = ChaveAtivo.CreditoPrivado(tipo, emissor, indexador, taxa, vencimento),
                nome = nome,
                tipo = tipo,
                classe = classe,
                emissor = emissor,
                indexador = indexador,
                taxa = taxa,
                vencimento = vencimento,
            )
        return AtivoClassificado(ativo, pendencias)
    }

    private fun classificarTesouro(
        nome: String,
        titulo: String,
    ): AtivoClassificado {
        val (classe, indexador) =
            when {
                titulo.startsWith("Selic", ignoreCase = true) -> ClasseAtivo.RF_POS to Indexador.SELIC
                titulo.startsWith("Prefixado", ignoreCase = true) -> ClasseAtivo.RF_PRE to Indexador.PRE
                else -> ClasseAtivo.RF_IPCA to Indexador.IPCA
            }
        val ativo =
            Ativo(
                chave = ChaveAtivo.Tesouro(normalizar(nome), vencimento = null),
                nome = nome,
                tipo = TipoAtivo.TESOURO,
                classe = classe,
                emissor = "TESOURO NACIONAL",
                indexador = indexador,
            )
        return AtivoClassificado(ativo)
    }

    private fun classificarFundo(
        nome: String,
        estrategia: Estrategia,
    ): AtivoClassificado {
        val nomeNormalizado = normalizar(nome)
        val classe = estrategia.classeFundo() ?: ClasseAtivo.NAO_CLASSIFICADO
        val tipo = if (classe == ClasseAtivo.CAIXA) TipoAtivo.CAIXA else TipoAtivo.FUNDO
        val gestora = if (tipo == TipoAtivo.FUNDO) gestoraDe(nomeNormalizado) else null
        val classeIncerta =
            classe == ClasseAtivo.NAO_CLASSIFICADO || (estrategia == Estrategia.ALTERNATIVO && !nomeNormalizado.contains("MULTI"))
        val pendencias =
            setOfNotNull(
                Pendencia.CLASSE_A_CONFIRMAR.takeIf { classeIncerta },
                Pendencia.GESTORA_DESCONHECIDA.takeIf { tipo == TipoAtivo.FUNDO && gestora == null },
            )
        val ativo = Ativo(ChaveAtivo.FundoPorNome(nomeNormalizado), nome, tipo, classe, gestora = gestora)
        return AtivoClassificado(ativo, pendencias)
    }

    private fun gestoraDe(nomeNormalizado: String): String? =
        gestoras.firstOrNull { marca ->
            val m = normalizar(marca)
            nomeNormalizado == m || nomeNormalizado.startsWith("$m ")
        }

    private fun lerTaxa(texto: String): Pair<Indexador?, Percent?> =
        TAXA_POS.find(texto)?.let { m ->
            val idx = if (m.groupValues[2].equals("SELIC", ignoreCase = true)) Indexador.SELIC else Indexador.CDI
            idx to percentual(m.groupValues[1])
        } ?: TAXA_IPCA.find(texto)?.let { Indexador.IPCA to percentual(it.groupValues[1]) }
            ?: TAXA_PRE.find(texto)?.let { Indexador.PRE to percentual(it.groupValues[1]) }
            ?: (null to null)

    private enum class Estrategia {
        POS_FIXADO,
        PRE_FIXADO,
        INFLACAO,
        MULTIMERCADO,
        ALTERNATIVO,
        RENDA_VARIAVEL_BRASIL,
        RENDA_VARIAVEL_GLOBAL,
        FUNDOS_LISTADOS,
        CAIXA,
        OUTRA,
        ;

        /** Classe de um fundo pela estratégia; alternativos da XP são tratados como multimercado. */
        fun classeFundo(): ClasseAtivo? =
            when (this) {
                POS_FIXADO, PRE_FIXADO, INFLACAO -> classeRendaFixa()
                MULTIMERCADO, ALTERNATIVO -> ClasseAtivo.FUNDO_MULTIMERCADO
                RENDA_VARIAVEL_BRASIL -> ClasseAtivo.FUNDO_ACOES
                RENDA_VARIAVEL_GLOBAL -> ClasseAtivo.RV_GLOBAL
                CAIXA -> ClasseAtivo.CAIXA
                FUNDOS_LISTADOS, OUTRA -> null
            }

        fun classeRendaFixa(): ClasseAtivo =
            when (this) {
                POS_FIXADO -> ClasseAtivo.RF_POS
                PRE_FIXADO -> ClasseAtivo.RF_PRE
                INFLACAO -> ClasseAtivo.RF_IPCA
                else -> ClasseAtivo.NAO_CLASSIFICADO
            }

        companion object {
            fun de(nome: String): Estrategia =
                when (normalizar(nome)) {
                    "POS FIXADO" -> POS_FIXADO
                    "PRE FIXADO" -> PRE_FIXADO
                    "INFLACAO" -> INFLACAO
                    "MULTIMERCADO" -> MULTIMERCADO
                    "ALTERNATIVO" -> ALTERNATIVO
                    "RENDA VARIAVEL BRASIL" -> RENDA_VARIAVEL_BRASIL
                    "RENDA VARIAVEL GLOBAL" -> RENDA_VARIAVEL_GLOBAL
                    "FUNDOS LISTADOS" -> FUNDOS_LISTADOS
                    "CAIXA" -> CAIXA
                    else -> OUTRA
                }
        }
    }

    public companion object {
        private val MESES = listOf("JAN", "FEV", "MAR", "ABR", "MAI", "JUN", "JUL", "AGO", "SET", "OUT", "NOV", "DEZ")
        private val TICKER = Regex("""([A-Z]{4})(\d{1,2})""")
        private val SUFIXOS_ACAO = setOf("3", "4", "5", "6", "7", "8")
        private val SUFIXOS_BDR = setOf("31", "32", "33", "34", "35", "39")
        private val CREDITO =
            Regex(
                """(?<tipo>CDB|LCI|LCA|LC|CRI|CRA|DEB|DEBENTURE|DEBÊNTURE)\s+(?<emissor>.+?)\s+-\s+""" +
                    """(?<mes>${MESES.joinToString("|")})/(?<ano>\d{4})(?:\s+-\s*(?<taxa>.*))?""",
                RegexOption.IGNORE_CASE,
            )
        private val TESOURO = Regex("""^Tesouro\s+(Selic|IPCA\+?|Prefixado|Renda\+|Educa\+)""", RegexOption.IGNORE_CASE)
        private val TAXA_POS = Regex("""([\d.]+(?:,\d+)?)%\s*(?:do\s+)?(CDI|SELIC)""", RegexOption.IGNORE_CASE)
        private val TAXA_IPCA = Regex("""IPCA\s*\+\s*([\d.]+(?:,\d+)?)%""", RegexOption.IGNORE_CASE)
        private val TAXA_PRE = Regex("""([\d.]+(?:,\d+)?)%\s*(?:a\.?a\.?|PR[EÉ])""", RegexOption.IGNORE_CASE)
        private val ESPACOS = Regex("""\s+""")
        private val DIACRITICOS = Regex("""\p{M}+""")

        /** Maiúsculas, sem acento e com espaços simples: base das chaves por nome. */
        public fun normalizar(texto: String): String =
            Normalizer
                .normalize(texto, Normalizer.Form.NFD)
                .replace(DIACRITICOS, "")
                .uppercase()
                .replace(ESPACOS, " ")
                .trim()
    }
}

private fun tipoCredito(prefixo: String): TipoAtivo =
    when (ClassificadorAtivos.normalizar(prefixo)) {
        "CDB" -> TipoAtivo.CDB
        "LCI" -> TipoAtivo.LCI
        "LCA" -> TipoAtivo.LCA
        "CRI" -> TipoAtivo.CRI
        "CRA" -> TipoAtivo.CRA
        "LC" -> TipoAtivo.LC
        else -> TipoAtivo.DEBENTURE
    }

private fun grupo(
    m: MatchResult,
    nome: String,
): String = requireNotNull(m.groups[nome]).value

private fun percentual(texto: String): Percent = Percent.of(BigDecimal(texto.replace(".", "").replace(",", ".")))
