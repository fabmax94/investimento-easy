package com.investimentoeasy.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.investimentoeasy.core.domain.mercado.Perfil
import com.investimentoeasy.core.model.Cotacao
import com.investimentoeasy.core.model.ExpectativasFocus
import com.investimentoeasy.core.model.FalhaDeFonte
import com.investimentoeasy.core.model.FonteMercado
import com.investimentoeasy.core.model.InformeFii
import com.investimentoeasy.core.model.PanoramaMercado
import com.investimentoeasy.core.model.Percent
import com.investimentoeasy.core.model.ValorDeMercado
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class RoomRepositorioDeMercadoTest {
    private val db =
        Room
            .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), InvestimentoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    private val repo = RoomRepositorioDeMercado(db.mercadoDao())

    @After
    fun fechar() = db.close()

    private val dia = LocalDate.of(2026, 9, 25)
    private val panorama =
        PanoramaMercado(
            obtidoEm = Instant.parse("2026-09-26T15:00:00Z"),
            selicMeta = ValorDeMercado(BigDecimal("13.75"), dia, FonteMercado.BANCO_CENTRAL),
            ipca12Meses = null,
            dolar = ValorDeMercado(BigDecimal("5.1991"), dia, FonteMercado.BANCO_CENTRAL),
            focus = ExpectativasFocus(dia, sortedMapOf(2026 to BigDecimal("13.50"), 2027 to BigDecimal("12.00")), emptyMap(), emptyMap()),
            ibovespa = Cotacao("^BVSP", BigDecimal("183476.86"), dia, Percent.of("6.68"), null, Percent.of("26.27"), BigDecimal("199355")),
            ifix = null,
            cotacoes = mapOf("KNCR11" to Cotacao("KNCR11.SA", BigDecimal("106.38"), dia, null, null, null, null)),
            fiis = mapOf("KNCR" to InformeFii("KNCR", LocalDate.of(2026, 8, 1), BigDecimal("102.64"), Percent.of("13.70"), null, 12)),
            falhas = listOf(FalhaDeFonte(FonteMercado.YAHOO, "RECR12: HTTP 404")),
        )

    @Test
    fun `guarda o ultimo panorama sem perder nenhum campo`() =
        runTest {
            repo.ultimo().shouldBeNull()
            repo.salvar(panorama.copy(obtidoEm = Instant.parse("2026-09-25T10:00:00Z")))
            repo.salvar(panorama)
            repo.ultimo() shouldBe panorama
        }

    @Test
    fun `cache ilegivel conta como sem cache`() =
        runTest {
            db.mercadoDao().gravar(
                MercadoEntity(obtidoEm = 1, conteudo = "{\"obtidoEm\":1,\"falhas\":[{\"fonte\":\"OUTRA\",\"motivo\":\"x\"}]}"),
            )
            repo.ultimo().shouldBeNull()
        }

    @Test
    fun `perfil fica nas preferencias`() {
        val prefs = ApplicationProvider.getApplicationContext<Context>().getSharedPreferences("teste-perfil", Context.MODE_PRIVATE)
        val repositorio = PreferenciasDePerfil(prefs)
        repositorio.ler().shouldBeNull()
        repositorio.gravar(Perfil.ARROJADO)
        repositorio.ler() shouldBe Perfil.ARROJADO
        prefs.edit().putString("perfil", "INEXISTENTE").commit()
        repositorio.ler().shouldBeNull()
    }
}
