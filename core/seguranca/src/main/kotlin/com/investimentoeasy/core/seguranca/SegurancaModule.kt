package com.investimentoeasy.core.seguranca

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SegurancaModule {
    private const val PROVEDOR = "AndroidKeyStore"
    private const val APELIDO = "investimento_chave_api"
    private const val BITS_CHAVE = 256
    private const val PREFERENCIAS = "seguranca"

    @Provides
    @Singleton
    fun cofre(
        @ApplicationContext context: Context,
    ): CofreDeChave =
        CofreDeChaveCifrado(
            context.getSharedPreferences(PREFERENCIAS, Context.MODE_PRIVATE),
            Cifrador(::chaveDoKeystore),
        )

    /**
     * Chave AES no Android Keystore, gerada uma vez e nunca exportável. O Robolectric não tem
     * AndroidKeyStore: este trecho só roda no aparelho (os testes usam uma chave de software).
     */
    private fun chaveDoKeystore(): SecretKey {
        val keystore = KeyStore.getInstance(PROVEDOR).apply { load(null) }
        (keystore.getKey(APELIDO, null) as? SecretKey)?.let { return it }
        return KeyGenerator
            .getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVEDOR)
            .apply {
                init(
                    KeyGenParameterSpec
                        .Builder(APELIDO, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(BITS_CHAVE)
                        .build(),
                )
            }.generateKey()
    }
}
