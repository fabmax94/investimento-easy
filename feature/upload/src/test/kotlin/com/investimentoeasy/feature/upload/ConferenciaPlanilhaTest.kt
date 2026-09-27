package com.investimentoeasy.feature.upload

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test
import java.time.LocalDate

class ConferenciaPlanilhaTest {
    private val complemento = Cenarios.complemento()
    private val conferencia = montarConferencia(complemento.base, complemento.casamento)

    private fun linha(nome: String) = conferencia.casadas.single { it.nome == nome }

    @Test
    fun `datas e contagens`() {
        conferencia.dataPlanilha shouldBe LocalDate.of(2026, 6, 21)
        conferencia.dataBase shouldBe LocalDate.of(2026, 9, 3)
        conferencia.planilhaMaisAntiga shouldBe true
        conferencia.comQuantidadeDiferente shouldBe 0
        conferencia.proventos shouldBe 2
        conferencia.podeGuardar shouldBe true
        conferencia.soNaPlanilha shouldContainExactly listOf("CDB OUTRO BANCO S.A. - AGO/2026")
        conferencia.soNaBase shouldContainExactlyInAnyOrder listOf("LFTB11", "IMAB11")
    }

    @Test
    fun `nome da planilha so aparece quando e outro`() {
        linha("Trend Nasdaq 100 FIA").nomeNaPlanilha shouldBe "Trend Nasdaq 100 FIM RL"
        linha("BOVA11").nomeNaPlanilha.shouldBeNull()
        conferencia.casadas.single { it.nome.startsWith("CDB BANCO EXEMPLO") }.nomeNaPlanilha.shouldBeNull()
    }

    @Test
    fun `detalhe traz o que a planilha acrescenta`() {
        linha("BOVA11").detalhe shouldBe "Preço médio R$ 151,23"
        linha("Trend Nasdaq 100 FIA").detalhe shouldBe "Aplicado R$ 27.000,00"
        conferencia.casadas.single { it.nome.startsWith("CDB BANCO EXEMPLO") }.detalhe shouldBe
            "Aplicado R$ 8.000,00 · 112,00% CDI · vence 10/03/2028"
    }

    @Test
    fun `planilha sem data nao e tratada como mais antiga`() {
        conferencia.copy(dataPlanilha = null).planilhaMaisAntiga shouldBe false
    }
}
