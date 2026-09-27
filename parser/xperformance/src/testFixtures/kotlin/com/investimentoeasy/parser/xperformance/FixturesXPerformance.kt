package com.investimentoeasy.parser.xperformance

/** Relatório XPerformance sintético (texto por página), compartilhado entre módulos de teste. */
public object FixturesXPerformance {
    public const val PAGINAS: Int = 10

    public fun sintetico(): List<String> =
        (1..PAGINAS).map { n ->
            val caminho = "/xperformance/sintetico/pagina-%02d.txt".format(n)
            requireNotNull(FixturesXPerformance::class.java.getResource(caminho)) { "Fixture ausente: $caminho" }.readText()
        }
}
