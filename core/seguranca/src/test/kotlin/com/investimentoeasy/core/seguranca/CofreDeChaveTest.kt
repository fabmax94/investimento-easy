package com.investimentoeasy.core.seguranca

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotContain
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.crypto.KeyGenerator

@RunWith(RobolectricTestRunner::class)
class CofreDeChaveTest {
    private val chave = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    private val preferencias = ApplicationProvider.getApplicationContext<Context>().getSharedPreferences("teste", Context.MODE_PRIVATE)
    private val cofre = CofreDeChaveCifrado(preferencias, Cifrador { chave })

    @Test
    fun `grava, le e apaga a chave`() {
        cofre.ler().shouldBeNull()
        cofre.gravar("  sk-ant-teste-123  ")
        cofre.ler() shouldBe "sk-ant-teste-123"
        cofre.apagar()
        cofre.ler().shouldBeNull()
    }

    @Test
    fun `no disco fica so o texto cifrado`() {
        cofre.gravar("sk-ant-segredo")
        preferencias.all.values.joinToString() shouldNotContain "segredo"
    }

    @Test
    fun `IV aleatorio - cifrar duas vezes da resultados diferentes`() {
        val cifrador = Cifrador { chave }
        cifrador.cifrar("x") shouldNotBe cifrador.cifrar("x")
    }

    @Test
    fun `conteudo adulterado ou de outra chave nao decifra`() {
        val cifrado = Cifrador { chave }.cifrar("sk-ant")
        val outra = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        Cifrador { outra }.decifrar(cifrado).shouldBeNull()
        Cifrador { chave }.decifrar("nao-e-base64!!").shouldBeNull()
    }

    @Test
    fun `chave vazia e recusada`() {
        shouldThrow<IllegalArgumentException> { cofre.gravar("   ") }
    }

    @Test
    fun `chave que recusa IV do chamador, como a do Keystore, cifra e decifra`() {
        ProvedorComoKeystore.instalar()
        val material = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey().encoded
        val cofreComoNoAparelho = CofreDeChaveCifrado(preferencias, Cifrador { ChaveComoKeystore(material) })
        cofreComoNoAparelho.gravar("sk-ant-no-aparelho")
        cofreComoNoAparelho.ler() shouldBe "sk-ant-no-aparelho"
    }

    @Test
    fun `falha do armazenamento seguro vira excecao do cofre, nunca derruba o app`() {
        val quebrado = CofreDeChaveCifrado(preferencias, Cifrador { throw java.security.ProviderException("Keystore indisponível") })
        shouldThrow<CofreIndisponivelException> { quebrado.gravar("sk-ant-x") }
        cofre.gravar("sk-ant-guardada")
        quebrado.ler().shouldBeNull()
    }
}
