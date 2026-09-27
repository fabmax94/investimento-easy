package com.investimentoeasy.core.documentos

import android.content.ContentResolver
import android.content.Context
import com.investimentoeasy.core.importacao.ExtratorDeTextoPdf
import com.investimentoeasy.core.importacao.ImportarRelatorio
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface DocumentosBindings {
    @Binds
    fun extrator(impl: ExtratorPdfBox): ExtratorDeTextoPdf
}

@Module
@InstallIn(SingletonComponent::class)
internal object DocumentosModule {
    @Provides
    fun contentResolver(
        @ApplicationContext context: Context,
    ): ContentResolver = context.contentResolver

    @Provides
    fun importarRelatorio(extrator: ExtratorDeTextoPdf): ImportarRelatorio = ImportarRelatorio(extrator)
}
