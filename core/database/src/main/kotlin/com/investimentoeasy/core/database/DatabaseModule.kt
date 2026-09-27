package com.investimentoeasy.core.database

import android.content.Context
import androidx.room.Room
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
            .addMigrations(*InvestimentoDatabase.MIGRACOES)
            .build()

    @Provides
    fun snapshotDao(db: InvestimentoDatabase): SnapshotDao = db.snapshotDao()
}

@Module
@InstallIn(SingletonComponent::class)
internal interface RepositoryModule {
    @Binds
    fun snapshotRepository(impl: RoomSnapshotRepository): SnapshotRepository
}
