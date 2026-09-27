package com.investimentoeasy.core.seguranca

import android.content.SharedPreferences
import androidx.core.content.edit
import java.security.GeneralSecurityException
import java.security.ProviderException

/** O Keystore do aparelho falhou (chave invalidada, hardware indisponível, erro do provedor). */
class CofreIndisponivelException(
    causa: Throwable,
) : Exception("Não foi possível usar o armazenamento seguro do aparelho", causa)

/** Guarda a chave da API do Claude informada pelo usuário. */
interface CofreDeChave {
    /** `null` se não há chave ou se ela não pôde ser lida (nesse caso, basta configurar de novo). */
    fun ler(): String?

    /** @throws CofreIndisponivelException se o armazenamento seguro falhar. */
    fun gravar(chave: String)

    fun apagar()
}

/** Só o texto cifrado vai para o disco; a chave de cifra fica no Keystore. */
class CofreDeChaveCifrado(
    private val preferencias: SharedPreferences,
    private val cifrador: Cifrador,
) : CofreDeChave {
    override fun ler(): String? =
        preferencias.getString(CAMPO, null)?.let { cifrado ->
            try {
                cifrador.decifrar(cifrado)
            } catch (_: GeneralSecurityException) {
                null
            } catch (_: ProviderException) {
                null
            }
        }

    override fun gravar(chave: String) {
        require(chave.isNotBlank()) { "Chave vazia" }
        val cifrado =
            try {
                cifrador.cifrar(chave.trim())
            } catch (e: GeneralSecurityException) {
                throw CofreIndisponivelException(e)
            } catch (e: ProviderException) {
                throw CofreIndisponivelException(e)
            } catch (e: IllegalStateException) {
                throw CofreIndisponivelException(e)
            }
        preferencias.edit { putString(CAMPO, cifrado) }
    }

    override fun apagar() {
        preferencias.edit { remove(CAMPO) }
    }

    private companion object {
        const val CAMPO = "chave_api_claude"
    }
}
