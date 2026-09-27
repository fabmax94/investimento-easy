package com.investimentoeasy.core.mercado

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.time.Clock

/**
 * Chama as fontes de verdade. Só roda com `-PfontesReais` (não roda no CI nem no check): serve
 * para conferir, de tempos em tempos, que os formatos das fontes não mudaram.
 */
class FontesReaisTest {
    @Test
    fun `fontes reais respondem no formato esperado`() {
        assumeTrue(System.getProperty("fontesReais") == "true", "Use -PfontesReais para chamar as fontes reais")
        val p = runBlocking { ProvedorDeMercadoHttp(Clock.systemDefaultZone()).atualizar(setOf("BOVA11", "KNCR11", "HGLG11", "ITSA4")) }
        println("Selic=${p.selicMeta} IPCA12m=${p.ipca12Meses} Dolar=${p.dolar}")
        println("Focus=${p.focus}")
        println("Ibov=${p.ibovespa}\nIFIX=${p.ifix}")
        p.cotacoes.forEach { println("${it.key} = ${it.value}") }
        p.fiis.forEach { println("${it.key} = ${it.value}") }
        println("Falhas=${p.falhas}")
        check(p.falhas.isEmpty()) { "Fontes com falha: ${p.falhas}" }
    }
}
