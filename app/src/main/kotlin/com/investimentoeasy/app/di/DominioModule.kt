package com.investimentoeasy.app.di

import com.investimentoeasy.core.domain.complemento.ComplementarBase
import com.investimentoeasy.core.domain.complemento.RepositorioDeComplementos
import com.investimentoeasy.core.domain.mercado.ProvedorDeMercado
import com.investimentoeasy.core.domain.snapshot.ConfirmarSnapshot
import com.investimentoeasy.core.domain.snapshot.SnapshotRepository
import com.investimentoeasy.core.mercado.ProvedorDeMercadoHttp
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

/** Casos de uso do domínio (Kotlin puro, sem Hilt) montados com as dependências do app. */
@Module
@InstallIn(SingletonComponent::class)
object DominioModule {
    @Provides
    fun confirmarSnapshot(
        repositorio: SnapshotRepository,
        relogio: Clock,
    ): ConfirmarSnapshot = ConfirmarSnapshot(repositorio, relogio)

    /** Planilha "Posição Detalhada" complementando a base atual. */
    @Provides
    fun complementarBase(
        repositorio: SnapshotRepository,
        complementos: RepositorioDeComplementos,
        relogio: Clock,
    ): ComplementarBase = ComplementarBase(repositorio, complementos, relogio)

    /** Dados de mercado de fontes gratuitas (Banco Central, Focus, Yahoo Finance, CVM). */
    @Provides
    @Singleton
    fun provedorDeMercado(relogio: Clock): ProvedorDeMercado = ProvedorDeMercadoHttp(relogio)
}
