package com.investimentoeasy.core.domain.classificacao

import com.investimentoeasy.core.model.ChaveAtivo
import com.investimentoeasy.core.model.ClasseAtivo
import com.investimentoeasy.core.model.Indexador
import com.investimentoeasy.core.model.Percent
import com.investimentoeasy.core.model.TipoAtivo
import com.investimentoeasy.core.testing.posicaoExtraida
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.YearMonth

class ClassificadorAtivosTest {
    private val classificador = ClassificadorAtivos()

    private fun classificar(
        nome: String,
        estrategia: String,
    ) = classificador.classificar(posicaoExtraida(nome = nome, estrategia = estrategia))

    @ParameterizedTest(name = "{0} em {1} -> {2} / {3}")
    @CsvSource(
        "HGLG11, Fundos Listados, FII, FII_TIJOLO",
        "KNCR11, Fundos Listados, FII, FII_PAPEL",
        "KFOF11, Fundos Listados, FII, FII_FOF",
        "LFTB11, Pós Fixado, ETF, RF_POS",
        "IMAB11, Inflação, ETF, RF_IPCA",
        "IRFM11, Pré Fixado, ETF, RF_PRE",
        "BOVA11, Renda Variável Brasil, ETF, ETF",
        "ITSA4, Renda Variável Brasil, ACAO, ACAO",
        "PETR3, Renda Variável Brasil, ACAO, ACAO",
        "IVVB11, Renda Variável Global, ETF, RV_GLOBAL",
        "AAPL34, Renda Variável Global, BDR, RV_GLOBAL",
    )
    fun `R5 - tickers sao classificados pela estrategia e pelo sufixo`(
        ticker: String,
        estrategia: String,
        tipo: TipoAtivo,
        classe: ClasseAtivo,
    ) {
        val resultado = classificar(ticker, estrategia)
        resultado.ativo.chave shouldBe ChaveAtivo.Ticker(ticker)
        resultado.ativo.tipo shouldBe tipo
        resultado.ativo.classe shouldBe classe
        resultado.pendencias.shouldBeEmpty()
    }

    @Test
    fun `R5 - FII fora do catalogo fica nao classificado com pendencia`() {
        val resultado = classificar("XYZW11", "Fundos Listados")
        resultado.ativo.classe shouldBe ClasseAtivo.FII_NAO_CLASSIFICADO
        resultado.pendencias shouldContainExactly setOf(Pendencia.SEGMENTO_FII_DESCONHECIDO)
    }

    @Test
    fun `R5 - ticker 11 em renda variavel Brasil fora da lista de ETFs e ambiguo (pode ser unit)`() {
        val resultado = classificar("TAEE11", "Renda Variável Brasil")
        resultado.ativo.tipo shouldBe TipoAtivo.ACAO
        resultado.pendencias shouldContainExactly setOf(Pendencia.TIPO_AMBIGUO)
    }

    @Test
    fun `R5 - ticker em estrategia desconhecida pede confirmacao`() {
        val acao = classificar("VALE3", "Cripto")
        acao.ativo.tipo shouldBe TipoAtivo.ACAO
        acao.ativo.classe shouldBe ClasseAtivo.NAO_CLASSIFICADO
        acao.pendencias shouldContainExactly setOf(Pendencia.CLASSE_A_CONFIRMAR)
        classificar("HASH11", "Cripto").ativo.tipo shouldBe TipoAtivo.OUTRO
    }

    @Test
    fun `R4 e R5 - CDB com emissor contendo hifen, vencimento e taxa`() {
        val resultado = classificar("CDB LOJAS EXEMPLO - FINANCEIRA S.A. - DEZ/2027 - 118,00% CDI", "Pós Fixado")
        val ativo = resultado.ativo
        ativo.tipo shouldBe TipoAtivo.CDB
        ativo.classe shouldBe ClasseAtivo.RF_POS
        ativo.emissor shouldBe "LOJAS EXEMPLO - FINANCEIRA S.A."
        ativo.indexador shouldBe Indexador.CDI
        ativo.taxa shouldBe Percent.of("118")
        ativo.vencimento shouldBe YearMonth.of(2027, 12)
        ativo.chave shouldBe
            ChaveAtivo.CreditoPrivado(
                TipoAtivo.CDB,
                "LOJAS EXEMPLO - FINANCEIRA S.A.",
                Indexador.CDI,
                Percent.of("118"),
                YearMonth.of(2027, 12),
            )
        resultado.pendencias.shouldBeEmpty()
    }

    @ParameterizedTest(name = "{0} -> {1} {2} {3}")
    @CsvSource(
        "'LCA BANCO Y - JAN/2029 - IPCA + 6,50%', LCA, IPCA, RF_IPCA",
        "'LCI BANCO Y - FEV/2028 - 12,50% a.a.', LCI, PRE, RF_PRE",
        "'CRA EMPRESA Z - MAR/2030 - 100% SELIC', CRA, SELIC, RF_POS",
        "'CRI EMPRESA Z - ABR/2031 - 95,00% do CDI', CRI, CDI, RF_POS",
        "'DEB EMPRESA W - MAI/2032 - IPCA + 7%', DEBENTURE, IPCA, RF_IPCA",
        "'LC FINANCEIRA - JUN/2027 - 13% PRÉ', LC, PRE, RF_PRE",
    )
    fun `R5 - credito privado deriva a classe do indexador`(
        nome: String,
        tipo: TipoAtivo,
        indexador: Indexador,
        classe: ClasseAtivo,
    ) {
        val ativo = classificar(nome, "Pós Fixado").ativo
        ativo.tipo shouldBe tipo
        ativo.indexador shouldBe indexador
        ativo.classe shouldBe classe
    }

    @Test
    fun `R5 - credito sem taxa legivel usa a estrategia, ou pede confirmacao`() {
        classificar("CDB BANCO Y - DEZ/2027 -", "Inflação").ativo.classe shouldBe ClasseAtivo.RF_IPCA
        val semPista = classificar("CDB BANCO Y - DEZ/2027 -", "Alternativo")
        semPista.ativo.classe shouldBe ClasseAtivo.NAO_CLASSIFICADO
        semPista.pendencias shouldContainExactly setOf(Pendencia.CLASSE_A_CONFIRMAR)
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(
        "Tesouro Selic 2029, RF_POS, SELIC",
        "Tesouro IPCA+ 2035, RF_IPCA, IPCA",
        "Tesouro Prefixado 2027, RF_PRE, PRE",
        "Tesouro Renda+ Aposentadoria Extra 2049, RF_IPCA, IPCA",
    )
    fun `R5 - Tesouro Direto`(
        nome: String,
        classe: ClasseAtivo,
        indexador: Indexador,
    ) {
        val ativo = classificar(nome, "Pós Fixado").ativo
        ativo.tipo shouldBe TipoAtivo.TESOURO
        ativo.classe shouldBe classe
        ativo.indexador shouldBe indexador
        ativo.chave.shouldBeInstanceOf<ChaveAtivo.Tesouro>().titulo shouldBe nome.uppercase()
    }

    @ParameterizedTest(name = "{0} em {1} -> {2}, gestora {3}")
    @CsvSource(
        "Compass Crédito Corporativo FIRF CP, Pós Fixado, RF_POS, Compass",
        "SulAmérica Renda Fixa Ativa FIRF, Pós Fixado, RF_POS, SulAmérica",
        "Trend Multiestratégia FIF Multi, Alternativo, FUNDO_MULTIMERCADO, Trend",
        "TREND S&P 500 FIA, Renda Variável Global, RV_GLOBAL, Trend",
        "Verde FIC FIM, Multimercado, FUNDO_MULTIMERCADO, Verde",
        "Dynamo Cougar FIA, Renda Variável Brasil, FUNDO_ACOES, Dynamo",
        "Itaú Juros IPCA FIRF, Inflação, RF_IPCA, Itaú",
    )
    fun `R5 - fundos recebem classe pela estrategia e gestora pela marca`(
        nome: String,
        estrategia: String,
        classe: ClasseAtivo,
        gestora: String,
    ) {
        val resultado = classificar(nome, estrategia)
        resultado.ativo.tipo shouldBe TipoAtivo.FUNDO
        resultado.ativo.classe shouldBe classe
        resultado.ativo.gestora shouldBe gestora
        resultado.pendencias.shouldBeEmpty()
    }

    @Test
    fun `R4 - fundo sem CNPJ usa o nome normalizado como chave`() {
        classificar("Sparta Infra Incentivado FIC FIF RF", "Pós Fixado").ativo.chave shouldBe
            ChaveAtivo.FundoPorNome("SPARTA INFRA INCENTIVADO FIC FIF RF")
    }

    @Test
    fun `R5 - gestora desconhecida e alternativo sem multi viram pendencias`() {
        val resultado = classificar("Fundo Exemplo Estruturado", "Alternativo")
        resultado.ativo.gestora shouldBe null
        resultado.pendencias shouldContainExactly setOf(Pendencia.CLASSE_A_CONFIRMAR, Pendencia.GESTORA_DESCONHECIDA)
    }

    @Test
    fun `R5 - estrategia caixa e estrategia desconhecida`() {
        val caixa = classificar("Saldo em conta", "Caixa")
        caixa.ativo.tipo shouldBe TipoAtivo.CAIXA
        caixa.ativo.classe shouldBe ClasseAtivo.CAIXA
        caixa.pendencias.shouldBeEmpty()

        classificar("Algo Novo", "Cripto").pendencias shouldContainExactly
            setOf(Pendencia.CLASSE_A_CONFIRMAR, Pendencia.GESTORA_DESCONHECIDA)
    }

    @Test
    fun `normalizacao remove acentos e espacos extras`() {
        ClassificadorAtivos.normalizar("  Pós   Fixado ") shouldBe "POS FIXADO"
    }
}
