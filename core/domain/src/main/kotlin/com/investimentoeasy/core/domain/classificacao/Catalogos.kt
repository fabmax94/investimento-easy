package com.investimentoeasy.core.domain.classificacao

public enum class SegmentoFii { TIJOLO, PAPEL, FOF }

/** Segmento de um FII pela raiz do ticker (as 4 letras). `null` quando desconhecido. */
public fun interface CatalogoFii {
    public fun segmento(raizTicker: String): SegmentoFii?
}

/**
 * Catálogo inicial de segmentos de FII. Um FII fora dele fica como não classificado e
 * gera pendência para o usuário confirmar, em vez de o app adivinhar.
 */
public object CatalogoFiiPadrao : CatalogoFii {
    private val segmentos: Map<String, SegmentoFii> =
        buildMap {
            listOf("HGLG", "BRCO", "BTLG", "XPML", "VISC", "HGBS", "HGRU", "KNRI", "HGRE", "TVRI", "TEPP", "ALZR", "PVBI", "RBRP")
                .forEach { put(it, SegmentoFii.TIJOLO) }
            listOf("KNCR", "KNIP", "RECR", "VGIR", "RBRY", "MXRF", "CPTS", "IRDM", "HGCR", "KNSC", "VRTA", "MCCI")
                .forEach { put(it, SegmentoFii.PAPEL) }
            listOf("KFOF", "BCFF", "RBRF", "HFOF", "MGFF", "XPSF")
                .forEach { put(it, SegmentoFii.FOF) }
        }

    override fun segmento(raizTicker: String): SegmentoFii? = segmentos[raizTicker.uppercase()]
}

/**
 * FIIs de tijolo em lajes corporativas ou agências bancárias: no Cenário ficam neutros
 * (o juro ajuda, a vacância estrutural não).
 */
public val FIIS_LAJE_OU_AGENCIA: Set<String> = setOf("TEPP", "TVRI", "PVBI", "RBRP", "HGRE", "BRCR", "JSRE", "RCRB")

/** ETFs de renda variável brasileira conhecidos; ticker 11 fora da lista é ambíguo (ETF ou unit). */
public val ETFS_BRASIL_CONHECIDOS: Set<String> =
    setOf(
        "BOVA11", "BOVV11", "BOVX11", "BOVB11", "XBOV11", "SMAL11", "SMAC11", "DIVO11", "AUVP11",
        "PIBB11", "BRAX11", "FIND11", "MATB11", "ISUS11", "ECOO11", "GOVE11", "DIVD11", "NSDV11",
    )

/** Marcas de gestoras reconhecidas no início do nome do fundo (usadas na concentração por gestora). */
public val GESTORAS_CONHECIDAS: List<String> =
    listOf(
        "Trend", "Compass", "Sparta", "SulAmérica", "Itaú", "BTG Pactual", "Kinea", "Verde", "SPX", "Legacy", "Ibiuna",
        "Absolute", "ARX", "AZ Quest", "Capitânia", "Kapitalo", "Genoa", "Vinci", "XP", "Bradesco", "Santander", "Safra",
        "Riza", "Augme", "Mauá", "JGP", "Clave", "Navi", "Real Investor", "Constellation", "Dynamo", "Alaska", "Squadra",
        "Warren", "Icatu", "Occam", "Gávea", "Quasar", "Porto", "Western Asset", "BNP Paribas", "Daycoval",
    )
