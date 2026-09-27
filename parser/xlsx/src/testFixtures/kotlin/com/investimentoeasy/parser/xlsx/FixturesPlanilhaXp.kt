package com.investimentoeasy.parser.xlsx

/**
 * "Posição Detalhada" sintética com o mesmo layout da planilha real da XP e as mesmas posições do
 * relatório XPerformance sintético, com as diferenças que aparecem nos arquivos reais:
 * - os fundos mudam de sufixo ("Trend Nasdaq 100 FIA" no PDF, "Trend Nasdaq 100 FIM RL" aqui);
 * - o CDB vem sem a taxa no nome (ela fica numa coluna própria);
 * - há um CDB que já venceu na data do PDF (só existe na planilha);
 * - a planilha é mais antiga que o PDF, então os saldos são outros.
 * Conta, titular e assessor são inventados, mas ficam onde a XP os coloca (R7).
 */
public object FixturesPlanilhaXp {
    public const val DATA: String = "21/06/2026"

    private val SEPARADOR = List(COLUNAS) { " " }

    public fun linhas(): List<List<String>> =
        listOf(
            listOf("", "", "", "", "", "Conta: 0000000 | $DATA, 18:53"),
            SEPARADOR,
            listOf(
                "Titular Exemplo, este é o seu patrimônio",
                "Total investido",
                "Saldo Disponível",
                "Saldo projetado",
                "",
                "Código do Assessor",
                "Nome do assessor",
            ),
            listOf("R$ 143.000,00", "R$ 142.700,00", "R$ 300,00", "R$ 300,00", "", "A00000", "Assessor Exemplo"),
            SEPARADOR,
            secao("Fundos de Investimentos", "R$ 65.400,00"),
            SEPARADOR,
            listOf("26,7% | Renda Variável Global") + CABECALHO_FUNDOS,
            listOf("Trend Nasdaq 100 FIM RL", "R$ 38.000,00", "26,6%", "40,00%", "47,00%", "R$ 27.000,00", "R$ 36.200,00"),
            SEPARADOR,
            listOf("13,7% | Alternativos") + CABECALHO_FUNDOS,
            listOf("Trend Ouro FIF Multi RL", "R$ 19.500,00", "13,7%", "38,00%", "39,00%", "R$ 14.000,00", "R$ 19.400,00"),
            SEPARADOR,
            listOf("5,5% | Pós-Fixado") + CABECALHO_FUNDOS,
            listOf(
                "Fundo Exemplo Debêntures Incentivadas FIC FIF-Infra RF RL",
                "R$ 7.900,00",
                "5,5%",
                "20,00%",
                "22,00%",
                "R$ 6.500,00",
                "R$ 7.900,00",
            ),
            SEPARADOR,
            secao("Ações", "R$ 24.400,00"),
            SEPARADOR,
            listOf("10,3% | Renda Variável Brasil") + CABECALHO_ACOES,
            listOf("BOVA11", "R$ 9.800,00", "6,9%", "8,00%", "R$ 151,23", "R$ 163,33", "60"),
            listOf("ITSA4", "R$ 4.900,00", "3,4%", "-2,00%", "R$ 10,00", "R$ 9,80", "500"),
            SEPARADOR,
            listOf("6,8% | Renda Variável Global") + CABECALHO_ACOES,
            listOf("IVVB11", "R$ 9.700,00", "6,8%", "5,00%", "R$ 369,52", "R$ 388,00", "25"),
            SEPARADOR,
            secao("Renda Fixa", "R$ 12.900,00"),
            SEPARADOR,
            listOf("9% | Pós-Fixado") + CABECALHO_RENDA_FIXA,
            listOf(
                "CDB BANCO EXEMPLO S.A. - MAR/2028", "R$ 9.900,00", "6,9%", "R$ 8.000,00", "R$ 8.000,00", "112,00% CDI",
                "10/03/2023", "10/03/2028", "10", "R$ 990,00", "R$ 285,00", "R$ 0,00", "R$ 9.615,00",
            ),
            listOf(
                "CDB OUTRO BANCO S.A. - AGO/2026", "R$ 3.000,00", "2,1%", "R$ 2.500,00", "R$ 2.500,00", "110,00% CDI",
                "01/08/2022", "03/08/2026", "3", "R$ 1.000,00", "R$ 75,00", "R$ 0,00", "R$ 2.925,00",
            ),
            SEPARADOR,
            secao("Fundos Imobiliários", "R$ 40.000,00"),
            SEPARADOR,
            listOf("28% | Fundos Listados") + CABECALHO_FIIS,
            listOf("HGLG11", "R$ 15.800,00", "11,1%", "12,00%", "15,00%", "R$ 160,00", "R$ 158,00", "100"),
            listOf("KNCR11", "R$ 20.100,00", "14,1%", "30,00%", "35,00%", "R$ 95,00", "R$ 100,50", "200"),
            listOf("XYZW11", "R$ 4.100,00", "2,9%", "-8,00%", "-6,00%", "R$ 110,00", "R$ 102,50", "40"),
            listOf("RECR12", "R$ 0,00", "0%", "-", "-", "Indefinido", "R$ 0,00", "9"),
            SEPARADOR,
            listOf("Dividendos, proventos e outras distribuições"),
            SEPARADOR,
            listOf("Proventos"),
            SEPARADOR,
            secao("Fundos Imobiliários", "R$ 200,00"),
            listOf("0,1% | Fundos Listados") + CABECALHO_PROVENTOS,
            listOf("KNCR11", "200", "0,14%", "R$ 200,00", "R$ 200,00", "RENDIMENTO", "25/06/2026"),
            SEPARADOR,
            secao("Ações", "R$ 10,00"),
            listOf("0% | Renda Variável Brasil") + CABECALHO_PROVENTOS,
            listOf("ITSA4", "500", "0%", "R$ 10,00", "R$ 10,00", "DIVIDENDO", "30/06/2026"),
            SEPARADOR,
            listOf("Custódia Remunerada"),
            SEPARADOR,
            secao("Ações e Fundos Imobiliários", "R$ 9.800,00"),
            listOf("6,9% | Renda Variável Brasil", "", "Valor total", "% Alocação", "Valor atual (PU)", "Quantidade", "Data de vencimento"),
            listOf("BOVA11", "", "R$ 9.800,00", "6,9%", "R$ 163,33", "60", "19/06/2026"),
        )

    public fun bytes(): ByteArray = EscritorXlsx.gerar(linhas())

    private fun secao(
        titulo: String,
        total: String,
    ) = listOf(titulo, "", "", "", "", "", total)

    private const val COLUNAS = 7
    private val CABECALHO_FUNDOS =
        listOf("Posição", "% Alocação", "Rentabilidade Líquida", "Rentabilidade Bruta", "Valor aplicado", "Valor líquido")
    private val CABECALHO_ACOES = listOf("Posição", "% Alocação", "Rentabilidade (%)", "Preço médio", "Último preço (R$)", "Qtd. total")
    private val CABECALHO_RENDA_FIXA =
        listOf(
            "Posição a mercado", "% Alocação", "Valor aplicado", "Valor aplicado original", "Taxa a mercado", "Data aplicação",
            "Data vencimento", "Quantidade", "Preço Unitário", "IR", "IOF", "Valor Líquido",
        )
    private val CABECALHO_FIIS =
        listOf(
            "Posição",
            "% Alocação",
            "Rentabilidade c/ proventos",
            "Rentabilidade Bruta",
            "Preço médio (abertura)",
            "Última cotação",
            "Quantidade de Cotas",
        )
    private val CABECALHO_PROVENTOS =
        listOf("Provisionado", "% Alocação", "Valor provisionado bruto", "Valor provisionado líquido", "Evento", "Previsão pagamento")
}
