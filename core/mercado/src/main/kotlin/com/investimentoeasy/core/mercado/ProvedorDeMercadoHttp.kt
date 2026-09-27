package com.investimentoeasy.core.mercado

import com.investimentoeasy.core.domain.mercado.ProvedorDeMercado
import com.investimentoeasy.core.model.FalhaDeFonte
import com.investimentoeasy.core.model.FonteMercado
import com.investimentoeasy.core.model.PanoramaMercado
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import java.io.IOException
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/** Endereços das fontes; os testes apontam todos para um servidor falso. */
public data class EnderecosDeMercado(
    val bancoCentral: HttpUrl = "https://api.bcb.gov.br".toHttpUrl(),
    val focus: HttpUrl = "https://olinda.bcb.gov.br".toHttpUrl(),
    val yahoo: HttpUrl = "https://query1.finance.yahoo.com".toHttpUrl(),
    val cvm: HttpUrl = "https://dados.cvm.gov.br".toHttpUrl(),
)

/**
 * Busca as fontes gratuitas em paralelo. Cada fonte que falha vira uma [FalhaDeFonte] e as demais
 * seguem: a análise mostra o que chegou e diz o que faltou.
 *
 * Privacidade: só os tickers vão para o Yahoo (nunca valores ou quantidades); Banco Central e CVM
 * são baixados inteiros, sem nada da carteira.
 */
public class ProvedorDeMercadoHttp(
    private val relogio: Clock,
    http: OkHttpClient = clientePadrao(),
    enderecos: EnderecosDeMercado = EnderecosDeMercado(),
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : ProvedorDeMercado {
    private val bancoCentral = BancoCentral(http, enderecos.bancoCentral)
    private val focus = Focus(http, enderecos.focus)
    private val yahoo = Yahoo(http, enderecos.yahoo)
    private val cvm = Cvm(http, enderecos.cvm)

    override suspend fun atualizar(tickers: Set<String>): PanoramaMercado =
        coroutineScope {
            val hoje = LocalDate.now(relogio)
            val falhas = java.util.concurrent.ConcurrentLinkedQueue<FalhaDeFonte>()
            val limite = Semaphore(PARALELISMO)

            suspend fun <T> tentar(
                fonte: FonteMercado,
                oque: String,
                bloco: () -> T,
            ): T? =
                limite.withPermit {
                    withContext(io) {
                        try {
                            bloco()
                        } catch (e: IOException) {
                            falhas += FalhaDeFonte(fonte, "$oque: ${e.message ?: e::class.java.simpleName}")
                            null
                        } catch (_: SerializationException) {
                            falhas += FalhaDeFonte(fonte, "$oque: resposta em formato inesperado")
                            null
                        } catch (_: IllegalArgumentException) {
                            falhas += FalhaDeFonte(fonte, "$oque: resposta em formato inesperado")
                            null
                        }
                    }
                }

            val selic = async { tentar(FonteMercado.BANCO_CENTRAL, "Selic") { bancoCentral.ultimo(BancoCentral.SELIC_META, hoje) } }
            val ipca = async { tentar(FonteMercado.BANCO_CENTRAL, "IPCA") { bancoCentral.ultimo(BancoCentral.IPCA_12_MESES, hoje) } }
            val dolar = async { tentar(FonteMercado.BANCO_CENTRAL, "Dólar") { bancoCentral.ultimo(BancoCentral.DOLAR_PTAX_VENDA, hoje) } }
            val expectativas = async { tentar(FonteMercado.FOCUS, "Focus") { focus.expectativas(hoje) } }
            val ibov = async { tentar(FonteMercado.YAHOO, "Ibovespa") { yahoo.cotacao(Yahoo.IBOVESPA) } }
            val ifix = async { tentar(FonteMercado.YAHOO, "XFIX11") { yahoo.cotacao(Yahoo.IFIX_ETF) } }
            val papeis = tickers.map { it.uppercase() }.distinct()
            val cotacoes =
                papeis.map { t -> async { t to tentar(FonteMercado.YAHOO, t) { yahoo.cotacao(Yahoo.simboloB3(t)) } } }
            val raizesFii = papeis.filter { it.endsWith("11") }.map { it.take(RAIZ) }.toSet()
            val informes = async { tentar(FonteMercado.CVM, "Informe de FIIs") { cvm.informes(raizesFii, hoje) } }

            PanoramaMercado(
                obtidoEm = relogio.instant(),
                selicMeta = selic.await(),
                ipca12Meses = ipca.await(),
                dolar = dolar.await(),
                focus = expectativas.await(),
                ibovespa = ibov.await(),
                ifix = ifix.await(),
                cotacoes = cotacoes.awaitAll().mapNotNull { (t, c) -> c?.let { t to it } }.toMap(),
                fiis = informes.await().orEmpty(),
                falhas = falhas.toList().sortedBy { it.fonte.ordinal },
            )
        }

    public companion object {
        private const val PARALELISMO = 4
        private const val RAIZ = 4
        private val TIMEOUT: Duration = Duration.ofSeconds(30)

        public fun clientePadrao(): OkHttpClient =
            OkHttpClient
                .Builder()
                .connectTimeout(TIMEOUT.seconds, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT.seconds, TimeUnit.SECONDS)
                .callTimeout(TIMEOUT.seconds * 2, TimeUnit.SECONDS)
                .build()
    }
}
