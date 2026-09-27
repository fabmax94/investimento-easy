package com.investimentoeasy.core.designsystem

import com.investimentoeasy.core.model.ClasseAtivo

/** Nomes em português das classes de ativo (R5), como no protótipo. */
public val ClasseAtivo.rotulo: String
    get() =
        when (this) {
            ClasseAtivo.RF_POS -> "Renda fixa pós-fixada"
            ClasseAtivo.RF_IPCA -> "Renda fixa IPCA+"
            ClasseAtivo.RF_PRE -> "Renda fixa prefixada"
            ClasseAtivo.FUNDO_MULTIMERCADO -> "Multimercado"
            ClasseAtivo.FUNDO_ACOES -> "Fundo de ações"
            ClasseAtivo.FII_TIJOLO -> "FII tijolo"
            ClasseAtivo.FII_PAPEL -> "FII papel"
            ClasseAtivo.FII_FOF -> "FII fundo de fundos"
            ClasseAtivo.FII_NAO_CLASSIFICADO -> "FII (segmento a confirmar)"
            ClasseAtivo.ACAO -> "Ação"
            ClasseAtivo.ETF -> "ETF"
            ClasseAtivo.RV_GLOBAL -> "Exterior"
            ClasseAtivo.CAIXA -> "Caixa"
            ClasseAtivo.NAO_CLASSIFICADO -> "A classificar"
        }
