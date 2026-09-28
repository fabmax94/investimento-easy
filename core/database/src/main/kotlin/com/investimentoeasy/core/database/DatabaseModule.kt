package com.investimentoeasy.core.database

import android.content.Context
import androidx.room.Room
import com.investimentoeasy.core.domain.complemento.RepositorioDeComplementos
import com.investimentoeasy.core.domain.mercado.RepositorioDeMercado
import com.investimentoeasy.core.domain.mercado.RepositorioDePerfil
import com.investimentoeasy.core.domain.snapshot.SnapshotRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
    @Provides
    @Singleton
    fun database(
        @ApplicationContext context: Context,
    ): InvestimentoDatabase =
        Room
            .databaseBuilder(context, InvestimentoDatabase::class.java, InvestimentoDatabase.NOME)
            .addCallback(InvestimentoDatabase.Imutabilidade)
            .addMigrations(
                InvestimentoDatabase.MIGRACAO_1_2,
                InvestimentoDatabase.MIGRACAO_2_3,
                InvestimentoDatabase.MIGRACAO_3_4,
                InvestimentoDatabase.MIGRACAO_4_5,
            )
            .build()

    @Provides
    fun snapshotDao(db: InvestimentoDatabase): SnapshotDao = db.snapshotDao()

    @Provides
    fun mercadoDao(db: InvestimentoDatabase): MercadoDao = db.mercadoDao()

    @Provides
    @Singleton
    fun perfil(
        @ApplicationContext context: Context,
    ): RepositorioDePerfil = PreferenciasDePerfil(context.getSharedPreferences(PreferenciasDePerfil.ARQUIVO, Context.MODE_PRIVATE))

    @Provides
    fun complementoDao(db: InvestimentoDatabase): ComplementoDao = db.complementoDao()
}

@Module
@InstallIn(SingletonComponent::class)
internal interface RepositoryModule {
    @Binds
    fun snapshotRepository(impl: RoomSnapshotRepository): SnapshotRepository

    @Binds
    fun repositorioDeMercado(impl: RoomRepositorioDeMercado): RepositorioDeMercado

    @Binds
    fun repositorioDeComplementos(impl: RoomRepositorioDeComplementos): RepositorioDeComplementos
}
