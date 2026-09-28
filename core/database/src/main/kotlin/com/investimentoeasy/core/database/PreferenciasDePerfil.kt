package com.investimentoeasy.core.database

import android.content.SharedPreferences
import androidx.core.content.edit
import com.investimentoeasy.core.domain.mercado.Perfil
import com.investimentoeasy.core.domain.mercado.RepositorioDePerfil

/** O perfil é preferência simples (não é dado financeiro): SharedPreferences basta. */
internal class PreferenciasDePerfil(
    private val preferencias: SharedPreferences,
) : RepositorioDePerfil {
    override fun ler(): Perfil? = preferencias.getString(CAMPO, null)?.let { nome -> Perfil.entries.firstOrNull { it.name == nome } }

    override fun gravar(perfil: Perfil) {
        preferencias.edit { putString(CAMPO, perfil.name) }
    }

    companion object {
        const val ARQUIVO = "preferencias"
        private const val CAMPO = "perfil"
    }
}
