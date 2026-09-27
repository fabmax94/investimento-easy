package com.investimentoeasy.core.domain.alocacao

import com.investimentoeasy.core.model.ClasseAtivo
import com.investimentoeasy.core.model.Money
import com.investimentoeasy.core.model.Percent
import com.investimentoeasy.core.model.Posicao
import com.investimentoeasy.core.model.soma

/** Grupos de alocação exibidos ao usuário (os FIIs somam tijolo, papel e FoF). */
public enum class GrupoAlocacao(
    public val classes: Set<ClasseAtivo>,
) {
    RENDA_FIXA_POS(setOf(ClasseAtivo.RF_POS)),
    RENDA_FIXA_IPCA(setOf(ClasseAtivo.RF_IPCA)),
    RENDA_FIXA_PRE(setOf(ClasseAtivo.RF_PRE)),
    MULTIMERCADO(setOf(ClasseAtivo.FUNDO_MULTIMERCADO)),
    FIIS(setOf(ClasseAtivo.FII_TIJOLO, ClasseAtivo.FII_PAPEL, ClasseAtivo.FII_FOF, ClasseAtivo.FII_NAO_CLASSIFICADO)),
    ACOES_BRASIL(setOf(ClasseAtivo.ACAO, ClasseAtivo.ETF, ClasseAtivo.FUNDO_ACOES)),
    EXTERIOR(setOf(ClasseAtivo.RV_GLOBAL)),
    CAIXA(setOf(ClasseAtivo.CAIXA)),
    A_CLASSIFICAR(setOf(ClasseAtivo.NAO_CLASSIFICADO)),
    ;

    public companion object {
        public fun de(classe: ClasseAtivo): GrupoAlocacao = entries.first { classe in it.classes }
    }
}

public data class FatiaAlocacao(
    val grupo: GrupoAlocacao,
    val quantidadeAtivos: Int,
    val valor: Money,
    /** Sobre a soma das posições; `null` quando a soma é zero. */
    val percentual: Percent?,
)

/**
 * Camada 1 (T1.12): alocação por grupo, na ordem de [GrupoAlocacao]; grupos vazios ficam de
 * fora. Posições com saldo zero contam como ativo mas não somam valor.
 */
public fun alocacaoPorGrupo(posicoes: List<Posicao>): List<FatiaAlocacao> {
    val total = posicoes.map { it.saldo.valor }.soma()
    val porGrupo = posicoes.groupBy { GrupoAlocacao.de(it.ativo.classe) }
    return GrupoAlocacao.entries.mapNotNull { grupo ->
        val doGrupo = porGrupo[grupo] ?: return@mapNotNull null
        val valor = doGrupo.map { it.saldo.valor }.soma()
        FatiaAlocacao(grupo, doGrupo.size, valor, valor.fracaoDe(total)?.let(Percent::daFracao))
    }
}
