package com.investimentoeasy.core.model

import java.time.YearMonth

/** Instrumento financeiro (o "veículo"). */
public enum class TipoAtivo { ACAO, FII, ETF, BDR, FUNDO, TESOURO, CDB, LCI, LCA, LC, DEBENTURE, CRI, CRA, CAIXA, OUTRO }

/** Classe econômica do ativo (R5). */
public enum class ClasseAtivo {
    RF_POS,
    RF_IPCA,
    RF_PRE,
    FUNDO_MULTIMERCADO,
    FUNDO_ACOES,
    FII_TIJOLO,
    FII_PAPEL,
    FII_FOF,
    FII_NAO_CLASSIFICADO,
    ACAO,
    ETF,
    RV_GLOBAL,
    CAIXA,
    NAO_CLASSIFICADO,
}

public enum class Indexador { CDI, SELIC, IPCA, PRE }

/**
 * Identificação única de um ativo (R4). É a chave que permite comparar dois uploads.
 *
 * O vencimento usa [YearMonth] porque o PDF XPerformance só informa mês/ano; assim a
 * mesma aplicação gera a mesma chave vindo do PDF ou da planilha.
 */
public sealed interface ChaveAtivo {
    public val id: String

    /** Ações, FIIs, ETFs e BDRs. */
    public data class Ticker(val codigo: String) : ChaveAtivo {
        override val id: String get() = "TICKER:$codigo"
    }

    /** Fundos com CNPJ conhecido (14 dígitos, sem máscara). */
    public data class Cnpj(val numero: String) : ChaveAtivo {
        init {
            require(numero.length == CNPJ_DIGITS && numero.all(Char::isDigit)) { "CNPJ deve ter 14 dígitos" }
        }

        override val id: String get() = "CNPJ:$numero"
    }

    /**
     * Fundo sem CNPJ no arquivo (o XPerformance não traz CNPJ). Usa o nome normalizado
     * até que o CNPJ seja resolvido; a troca de chave é tratada na reconciliação.
     */
    public data class FundoPorNome(val nomeNormalizado: String) : ChaveAtivo {
        override val id: String get() = "FUNDO:$nomeNormalizado"
    }

    public data class Tesouro(val titulo: String, val vencimento: YearMonth?) : ChaveAtivo {
        override val id: String get() = "TESOURO:$titulo:${vencimento ?: "?"}"
    }

    /** CDB, LCI, LCA, LC, debêntures, CRI e CRA: emissor + indexador + taxa + vencimento. */
    public data class CreditoPrivado(
        val tipo: TipoAtivo,
        val emissor: String,
        val indexador: Indexador?,
        val taxa: Percent?,
        val vencimento: YearMonth,
    ) : ChaveAtivo {
        override val id: String get() = "CREDITO:$tipo:$emissor:${indexador ?: "?"}:${taxa ?: "?"}:$vencimento"
    }

    private companion object {
        const val CNPJ_DIGITS = 14
    }
}

public data class Ativo(
    val chave: ChaveAtivo,
    val nome: String,
    val tipo: TipoAtivo,
    val classe: ClasseAtivo,
    val gestora: String? = null,
    val emissor: String? = null,
    val indexador: Indexador? = null,
    val taxa: Percent? = null,
    val vencimento: YearMonth? = null,
)
