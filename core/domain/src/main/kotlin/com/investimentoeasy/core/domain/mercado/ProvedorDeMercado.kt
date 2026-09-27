package com.investimentoeasy.core.domain.mercado

import com.investimentoeasy.core.model.PanoramaMercado

/** Busca os dados públicos de mercado para os [tickers] da carteira. Nunca lança: falhas vão em `falhas`. */
public fun interface ProvedorDeMercado {
    public suspend fun atualizar(tickers: Set<String>): PanoramaMercado
}

/** Último panorama baixado, para a análise funcionar sem internet. */
public interface RepositorioDeMercado {
    public suspend fun salvar(panorama: PanoramaMercado)

    public suspend fun ultimo(): PanoramaMercado?
}
