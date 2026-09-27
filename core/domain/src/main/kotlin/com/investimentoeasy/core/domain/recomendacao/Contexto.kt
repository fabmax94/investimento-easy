package com.investimentoeasy.core.domain.recomendacao

import com.investimentoeasy.core.domain.alocacao.GrupoAlocacao
import com.investimentoeasy.core.domain.analise.AnaliseDeterministica
import com.investimentoeasy.core.domain.complemento.ComplementoPlanilha
import com.investimentoeasy.core.domain.mercado.FiiNoMercado
import com.investimentoeasy.core.domain.mercado.LeituraDeCambio
import com.investimentoeasy.core.domain.mercado.LeituraDeJuros
import com.investimentoeasy.core.domain.mercado.Perfil
import com.investimentoeasy.core.domain.mercado.alvosDe
import com.investimentoeasy.core.domain.mercado.fiiNoMercado
import com.investimentoeasy.core.domain.mercado.leituraDeCambio
import com.investimentoeasy.core.domain.mercado.leituraDeJuros
import com.investimentoeasy.core.model.ChaveAtivo
import com.investimentoeasy.core.model.Money
import com.investimentoeasy.core.model.PanoramaMercado
import com.investimentoeasy.core.model.Percent
import com.investimentoeasy.core.model.Posicao
import com.investimentoeasy.core.model.Snapshot
import com.investimentoeasy.core.model.TipoAtivo
import com.investimentoeasy.core.model.soma
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

/** Tudo o que as regras de recomendação leem, já cruzado (carteira, perfil, mercado, planilha). */
internal class Contexto(
    val snapshot: Snapshot,
    val analise: AnaliseDeterministica,
    val perfil: Perfil,
    val panorama: PanoramaMercado?,
    val complemento: ComplementoPlanilha?,
    val hoje: LocalDate,
) {
    val total: Money = analise.total
    val alvos = alvosDe(perfil)
    val juros: LeituraDeJuros? = panorama?.let { leituraDeJuros(it, hoje) }
    val cambio: LeituraDeCambio? = panorama?.let { leituraDeCambio(it, hoje) }
    val cdi12Meses: Percent? = snapshot.contexto?.referencia("CDI")?.dozeMeses

    /** Caixa conta junto com a renda fixa pós-fixada (o perfil não tem alvo de caixa). */
    fun grupoDoPerfil(grupo: GrupoAlocacao): GrupoAlocacao = if (grupo == GrupoAlocacao.CAIXA) GrupoAlocacao.RENDA_FIXA_POS else grupo

    fun posicoesDo(grupo: GrupoAlocacao): List<Posicao> =
        snapshot.posicoes.filter { grupoDoPerfil(GrupoAlocacao.de(it.ativo.classe)) == grupo }

    fun valorDo(grupo: GrupoAlocacao): Money = posicoesDo(grupo).map { it.saldo.valor }.soma()

    fun percentualDo(grupo: GrupoAlocacao): Percent = valorDo(grupo).fracaoDe(total)?.let(Percent::daFracao) ?: Percent.ZERO

    /** Valor em reais de [pontos] percentuais da carteira, arredondado para baixo em centenas. */
    fun valorDePontos(pontos: Percent): Money {
        val bruto = total.valor.multiply(pontos.fracao)
        return Money.of(bruto.divide(CENTENA, 0, RoundingMode.DOWN).multiply(CENTENA))
    }

    fun temTicker(ticker: String): Boolean = snapshot.posicoes.any { (it.ativo.chave as? ChaveAtivo.Ticker)?.codigo == ticker }

    val fiis: List<FiiNoMercado> by lazy {
        val p = panorama ?: return@lazy emptyList()
        snapshot.posicoes
            .filter { it.ativo.tipo == TipoAtivo.FII && !it.saldo.valor.isZero }
            .mapNotNull { pos ->
                val ticker = (pos.ativo.chave as? ChaveAtivo.Ticker)?.codigo ?: return@mapNotNull null
                fiiNoMercado(ticker, p.cotacao(ticker), p.informe(ticker), p.ifix)
            }
    }

    private companion object {
        val CENTENA = BigDecimal(100)
    }
}

internal val GrupoAlocacao.rotulo: String
    get() =
        when (this) {
            GrupoAlocacao.RENDA_FIXA_POS -> "Renda fixa pós-fixada"
            GrupoAlocacao.RENDA_FIXA_IPCA -> "Renda fixa IPCA+"
            GrupoAlocacao.RENDA_FIXA_PRE -> "Renda fixa prefixada"
            GrupoAlocacao.MULTIMERCADO -> "Multimercados"
            GrupoAlocacao.FIIS -> "Fundos imobiliários"
            GrupoAlocacao.ACOES_BRASIL -> "Ações Brasil"
            GrupoAlocacao.EXTERIOR -> "Exterior"
            GrupoAlocacao.CAIXA -> "Caixa"
            GrupoAlocacao.A_CLASSIFICAR -> "A classificar"
        }
