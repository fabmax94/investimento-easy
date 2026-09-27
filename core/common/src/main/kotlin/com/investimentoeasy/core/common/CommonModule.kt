package com.investimentoeasy.core.common

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.time.Clock
import javax.inject.Qualifier

/** Dispatcher para leitura de arquivo, PDF e banco: injetado para os testes controlarem o tempo. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Io

@Module
@InstallIn(SingletonComponent::class)
object CommonModule {
    @Provides
    @Io
    fun io(): CoroutineDispatcher = Dispatchers.IO

    /** Relógio único do app: nada de `now()` escondido (ADR-0003). */
    @Provides
    fun relogio(): Clock = Clock.systemDefaultZone()
}
