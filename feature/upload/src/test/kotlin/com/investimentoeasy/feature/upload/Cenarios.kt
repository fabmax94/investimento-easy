package com.investimentoeasy.feature.upload

import com.investimentoeasy.core.domain.snapshot.PrepararRevisao
import com.investimentoeasy.core.domain.snapshot.Revisao
import com.investimentoeasy.core.importacao.ArquivoRecebido
import com.investimentoeasy.core.importacao.ExtratorDeTextoPdf
import com.investimentoeasy.core.importacao.ImportarRelatorio
import com.investimentoeasy.core.importacao.ResultadoImportacao
import com.investimentoeasy.core.testing.geradorSequencial
import com.investimentoeasy.parser.xperformance.FixturesXPerformance

/** Cenários da tela montados a partir do relatório sintético. */
internal object Cenarios {
    val pdf = "%PDF-1.7".toByteArray()

    fun importar(paginas: List<String> = FixturesXPerformance.sintetico()): ImportarRelatorio =
        ImportarRelatorio(ExtratorDeTextoPdf { paginas }, PrepararRevisao(geradorId = geradorSequencial()))

    fun revisao(paginas: List<String> = FixturesXPerformance.sintetico()): Revisao =
        (importar(paginas).importar(ArquivoRecebido("x.pdf", null, pdf)) as ResultadoImportacao.Lido).revisao

    /** Patrimônio informado 10% acima da soma: R3 exige aceite. */
    fun paginasComDivergencia(): List<String> =
        FixturesXPerformance.sintetico().toMutableList().also {
            it[0] = it[0].replace("150.250,00", "165.000,00")
        }

    fun paginasSemData(): List<String> =
        FixturesXPerformance.sintetico().map { it.replace(Regex("Data de referência: \\d{2}/\\d{2}/\\d{4}"), "") }
}
