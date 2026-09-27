package com.investimentoeasy.core.seguranca

import java.security.AlgorithmParameters
import java.security.InvalidAlgorithmParameterException
import java.security.Key
import java.security.Provider
import java.security.SecureRandom
import java.security.Security
import java.security.spec.AlgorithmParameterSpec
import javax.crypto.Cipher
import javax.crypto.CipherSpi
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * Chave que se comporta como uma do Android Keystore: não exporta o material (`encoded` nulo, então
 * só o provedor abaixo a aceita) e recusa IV informado por quem cifra, como faz o Keystore com
 * `randomizedEncryptionRequired` (o padrão).
 */
internal class ChaveComoKeystore(
    val material: ByteArray,
) : SecretKey {
    override fun getAlgorithm(): String = "AES"

    override fun getFormat(): String? = null

    override fun getEncoded(): ByteArray? = null
}

internal object ProvedorComoKeystore : Provider("ComoKeystore", 1.0, "Imita a regra de IV do AndroidKeyStore") {
    init {
        put("Cipher.AES/GCM/NoPadding", CifraComoKeystore::class.java.name)
        put("Cipher.AES/GCM/NoPadding SupportedKeyClasses", ChaveComoKeystore::class.java.name)
    }

    fun instalar() {
        if (Security.getProvider(name) == null) Security.insertProviderAt(this, 1)
    }
}

internal class CifraComoKeystore : CipherSpi() {
    private val real: Cipher = Cipher.getInstance("AES/GCM/NoPadding", "SunJCE")

    private fun chaveReal(key: Key) = SecretKeySpec((key as ChaveComoKeystore).material, "AES")

    private fun recusarIvNaCifragem(
        modo: Int,
        temParametros: Boolean,
    ) {
        if (modo == Cipher.ENCRYPT_MODE && temParametros) throw InvalidAlgorithmParameterException("Caller-provided IV not permitted")
    }

    override fun engineInit(
        opmode: Int,
        key: Key,
        random: SecureRandom?,
    ) = real.init(opmode, chaveReal(key), random ?: SecureRandom())

    override fun engineInit(
        opmode: Int,
        key: Key,
        params: AlgorithmParameterSpec?,
        random: SecureRandom?,
    ) {
        recusarIvNaCifragem(opmode, params != null)
        real.init(opmode, chaveReal(key), params, random ?: SecureRandom())
    }

    override fun engineInit(
        opmode: Int,
        key: Key,
        params: AlgorithmParameters?,
        random: SecureRandom?,
    ) {
        recusarIvNaCifragem(opmode, params != null)
        real.init(opmode, chaveReal(key), params, random ?: SecureRandom())
    }

    override fun engineSetMode(mode: String?) = Unit

    override fun engineSetPadding(padding: String?) = Unit

    override fun engineGetBlockSize(): Int = real.blockSize

    override fun engineGetOutputSize(inputLen: Int): Int = real.getOutputSize(inputLen)

    override fun engineGetIV(): ByteArray? = real.iv

    override fun engineGetParameters(): AlgorithmParameters? = real.parameters

    override fun engineUpdate(
        input: ByteArray?,
        inputOffset: Int,
        inputLen: Int,
    ): ByteArray? = real.update(input, inputOffset, inputLen)

    override fun engineUpdate(
        input: ByteArray?,
        inputOffset: Int,
        inputLen: Int,
        output: ByteArray?,
        outputOffset: Int,
    ): Int = real.update(input, inputOffset, inputLen, output, outputOffset)

    override fun engineDoFinal(
        input: ByteArray?,
        inputOffset: Int,
        inputLen: Int,
    ): ByteArray = real.doFinal(input, inputOffset, inputLen)

    override fun engineDoFinal(
        input: ByteArray?,
        inputOffset: Int,
        inputLen: Int,
        output: ByteArray?,
        outputOffset: Int,
    ): Int = real.doFinal(input, inputOffset, inputLen, output, outputOffset)
}
