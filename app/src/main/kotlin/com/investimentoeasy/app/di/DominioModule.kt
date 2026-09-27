package com.investimentoeasy.app.di

import com.investimentoeasy.core.ai.ClaudeModelo
import com.investimentoeasy.core.ai.FabricaDeModelo
import com.investimentoeasy.core.domain.snapshot.ConfirmarSnapshot
import com.investimentoeasy.core.domain.snapshot.SnapshotRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock

/** Casos de uso do domínio (Kotlin puro, sem Hilt) montados com as dependências do app. */
@Module
@InstallIn(SingletonComponent::class)
object DominioModule {
    @Provides
    fun confirmarSnapshot(
        repositorio: SnapshotRepository,
        relogio: Clock,
    ): ConfirmarSnapshot = ConfirmarSnapshot(repositorio, relogio)

    /** Camada 3: Claude pela API, com a chave que o usuário configurou. */
    @Provides
    fun fabricaDeModelo(): FabricaDeModelo = FabricaDeModelo { chave -> ClaudeModelo.comChave(chave) }
}
