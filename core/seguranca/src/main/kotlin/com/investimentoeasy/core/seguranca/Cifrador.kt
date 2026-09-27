package com.investimentoeasy.core.seguranca

import android.util.Base64
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-GCM com IV aleatório por mensagem, gravado junto do texto cifrado. A chave vem de fora:
 * no aparelho, do Android Keystore (não exportável); nos testes, uma chave de software.
 */
class Cifrador(
    private val chave: () -> SecretKey,
) {
    fun cifrar(texto: String): String {
        val cifra = Cipher.getInstance(TRANSFORMACAO)
        val iv = ByteArray(TAMANHO_IV).also(SecureRandom()::nextBytes)
        cifra.init(Cipher.ENCRYPT_MODE, chave(), GCMParameterSpec(BITS_TAG, iv))
        return Base64.encodeToString(iv + cifra.doFinal(texto.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
    }

    /** `null` se o conteúdo foi adulterado ou cifrado com outra chave. */
    fun decifrar(cifrado: String): String? =
        try {
            val bytes = Base64.decode(cifrado, Base64.NO_WRAP)
            val cifra = Cipher.getInstance(TRANSFORMACAO)
            cifra.init(Cipher.DECRYPT_MODE, chave(), GCMParameterSpec(BITS_TAG, bytes, 0, TAMANHO_IV))
            String(cifra.doFinal(bytes, TAMANHO_IV, bytes.size - TAMANHO_IV), Charsets.UTF_8)
        } catch (_: GeneralSecurityException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }

    private companion object {
        const val TRANSFORMACAO = "AES/GCM/NoPadding"
        const val TAMANHO_IV = 12
        const val BITS_TAG = 128
    }
}
