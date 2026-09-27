package com.investimentoeasy.core.seguranca

import android.content.SharedPreferences
import androidx.core.content.edit

/** Guarda a chave da API do Claude informada pelo usuário. */
interface CofreDeChave {
    fun ler(): String?

    fun gravar(chave: String)

    fun apagar()
}

/** Só o texto cifrado vai para o disco; a chave de cifra fica no Keystore. */
class CofreDeChaveCifrado(
    private val preferencias: SharedPreferences,
    private val cifrador: Cifrador,
) : CofreDeChave {
    override fun ler(): String? = preferencias.getString(CAMPO, null)?.let(cifrador::decifrar)

    override fun gravar(chave: String) {
        require(chave.isNotBlank()) { "Chave vazia" }
        preferencias.edit { putString(CAMPO, cifrador.cifrar(chave.trim())) }
    }

    override fun apagar() {
        preferencias.edit { remove(CAMPO) }
    }

    private companion object {
        const val CAMPO = "chave_api_claude"
    }
}
