package com.investimentoeasy.parser.xperformance

internal object Fixtures {
    fun sintetico(): List<String> =
        (1..10).map { n ->
            val caminho = "/xperformance/sintetico/pagina-%02d.txt".format(n)
            requireNotNull(Fixtures::class.java.getResource(caminho)) { "Fixture ausente: $caminho" }.readText()
        }
}
