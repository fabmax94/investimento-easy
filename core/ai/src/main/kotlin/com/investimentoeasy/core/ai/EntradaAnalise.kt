package com.investimentoeasy.core.ai

import com.investimentoeasy.core.domain.analise.AnaliseDeterministica
import com.investimentoeasy.core.domain.analise.AtivoAnalisado
import com.investimentoeasy.core.domain.complemento.ComplementoPlanilha
import com.investimentoeasy.core.domain.complemento.DadosDaPlanilha
import com.investimentoeasy.core.domain.complemento.resultadoDe
import com.investimentoeasy.core.model.Money
import com.investimentoeasy.core.model.Percent
import com.investimentoeasy.core.model.Posicao
import com.investimentoeasy.core.model.Snapshot
import kotlinx.serialization.Serializable

/**
 * O que o Claude recebe (Camada 3): o snapshot já classificado e tudo o que as camadas 1 e 2
 * calcularam. Números como texto decimal simples ("150250.00", "13.33"), para não perder
 * precisão. Nenhum dado identificável: sem conta, sem assessor, sem nome do arquivo (R7).
 */
@Serializable
public data class EntradaAnalise(
    val dataReferencia: String,
    val patrimonioInformado: String?,
    val somaDasPosicoes: String,
    val posicoes: List<PosicaoEntrada>,
    val alocacao: List<FatiaEntrada>,
    val gestoras: List<ConcentracaoEntrada>,
    val emissoresComFgc: List<ConcentracaoEntrada>,
    val exposicaoGlobalPercentual: String?,
    val vencimentosProximos: List<String>,
    val posicoesSimbolicas: List<String>,
    val fundosAbaixoDoCdiNoAno: List<String>,
    val saques12Meses: SaquesEntrada?,
    val indicesDoRelatorio: List<IndiceEntrada>,
    val lacunas: List<LacunaEntrada>,
    val alertas: List<AlertaEntrada>,
    val dadosAusentes: List<String>,
    /** Data da planilha "Posição Detalhada" que complementa a base, se houver. */
    val dataPlanilha: String? = null,
    val proventosPrevistos: List<ProventoEntrada> = emptyList(),
)

@Serializable
public data class PosicaoEntrada(
    val nome: String,
    val tipo: String,
    val classe: String,
    val gestora: String?,
    val emissor: String?,
    val vencimento: String?,
    val saldo: String,
    val quantidade: String?,
    val rentabilidadeMes: String?,
    val rentabilidadeAno: String?,
    val rentabilidade24Meses: String?,
    val percentualCdiAno: String?,
    val ritmo: String?,
    val ritmoDeltaAoAno: String?,
    val cenario: String,
    val cenarioDriver: String,
    val dadoAConferirComAssessor: Boolean,
    val planilha: PlanilhaEntrada? = null,
)

/** O que a planilha acrescenta à posição; o resultado é estimado pelo app (saldo da base menos o custo). */
@Serializable
public data class PlanilhaEntrada(
    val valorAplicado: String?,
    val precoMedio: String?,
    val rentabilidadeDesdeInicio: String?,
    val dataAplicacao: String?,
    val vencimento: String?,
    val irEstimado: String?,
    val quantidadeMudouDesdeAPlanilha: Boolean,
    val custoEstimado: String?,
    val resultadoEstimado: String?,
    val resultadoEstimadoPercentual: String?,
)

@Serializable
public data class ProventoEntrada(
    val ativo: String,
    val evento: String,
    val valorLiquido: String,
    val dataPagamento: String?,
)

@Serializable
public data class FatiaEntrada(
    val grupo: String,
    val quantidadeAtivos: Int,
    val valor: String,
    val percentual: String?,
)

@Serializable
public data class ConcentracaoEntrada(
    val nome: String,
    val valor: String,
    val percentual: String,
    val quantidadeAtivos: Int,
)

@Serializable
public data class SaquesEntrada(
    val resgates: String,
    val percentualDoPatrimonio: String?,
    val mesesComResgate: Int,
    val sugereObjetivoConsumo: Boolean,
)

@Serializable
public data class IndiceEntrada(
    val nome: String,
    val mes: String?,
    val ano: String?,
    val dozeMeses: String?,
    val vinteQuatroMeses: String?,
)

@Serializable
public data class LacunaEntrada(
    val id: String,
    val descricao: String,
    val percentualAtual: String?,
)

@Serializable
public data class AlertaEntrada(
    val severidade: String,
    val regra: String,
    val titulo: String,
    val ativos: List<String>,
    val valor: String?,
    val percentual: String?,
)

public fun montarEntrada(
    snapshot: Snapshot,
    analise: AnaliseDeterministica,
    complemento: ComplementoPlanilha? = null,
): EntradaAnalise =
    EntradaAnalise(
        dataReferencia = snapshot.dataReferencia.toString(),
        patrimonioInformado = snapshot.patrimonioInformado?.valor?.t(),
        somaDasPosicoes = analise.total.t(),
        posicoes = analise.ativos.map { posicaoEntrada(it, complemento) },
        alocacao = analise.alocacao.map { FatiaEntrada(it.grupo.name, it.quantidadeAtivos, it.valor.t(), it.percentual?.t()) },
        gestoras = analise.gestoras.map { ConcentracaoEntrada(it.nome, it.valor.t(), it.percentual.t(), it.quantidadeAtivos) },
        emissoresComFgc =
            analise.emissoresGarantidos.map {
                ConcentracaoEntrada(
                    it.nome,
                    it.valor.t(),
                    it.percentual.t(),
                    it.quantidadeAtivos,
                )
            },
        exposicaoGlobalPercentual = analise.exposicaoGlobal?.t(),
        vencimentosProximos = analise.vencimentos.map { "${it.posicao.ativo.nome} vence em ${it.vencimento}" },
        posicoesSimbolicas = analise.simbolicas.map { it.ativo.nome },
        fundosAbaixoDoCdiNoAno = analise.fundosAbaixoDoCdi.map { it.posicao.ativo.nome },
        saques12Meses =
            analise.saques?.let {
                SaquesEntrada(it.resgates12Meses.t(), it.percentualDoPatrimonio?.t(), it.mesesComResgate, it.sugereConsumo)
            },
        indicesDoRelatorio =
            analise.mercado.map {
                IndiceEntrada(
                    it.nome,
                    it.mes?.t(),
                    it.ano?.t(),
                    it.dozeMeses?.t(),
                    it.vinteQuatroMeses?.t(),
                )
            },
        lacunas = analise.lacunas.map { LacunaEntrada(it.tipo.name, it.descricao, it.percentualAtual?.t()) },
        alertas =
            analise.alertas.map {
                AlertaEntrada(it.severidade.name, it.regra.name, it.titulo, it.ativos, it.valor?.t(), it.percentual?.t())
            },
        dadosAusentes = dadosAusentes(snapshot, analise, complemento),
        dataPlanilha = complemento?.dataPlanilha?.toString(),
        proventosPrevistos =
            complemento?.proventos.orEmpty().map {
                ProventoEntrada(
                    it.ativo,
                    it.evento,
                    it.valorLiquido.t(),
                    it.dataPagamento?.toString(),
                )
            },
    )

private fun dadosAusentes(
    snapshot: Snapshot,
    analise: AnaliseDeterministica,
    complemento: ComplementoPlanilha?,
): List<String> {
    val semPlanilha = analise.ativos.map { it.posicao }.filter { complemento?.de(it.ativo.chave) == null }.map { it.ativo.nome }
    val custo =
        when {
            complemento == null -> listOf(SEM_PRECO_MEDIO)
            semPlanilha.isNotEmpty() -> listOf("$SEM_PRECO_MEDIO, para: ${semPlanilha.joinToString(", ")}")
            else -> emptyList()
        }
    return custo + DADOS_AUSENTES + (if (snapshot.contexto == null) listOf(SEM_CONTEXTO) else emptyList())
}

private fun posicaoEntrada(
    a: AtivoAnalisado,
    complemento: ComplementoPlanilha?,
): PosicaoEntrada {
    val p = a.posicao
    val dados = complemento?.de(p.ativo.chave)
    return PosicaoEntrada(
        nome = p.ativo.nome,
        tipo = p.ativo.tipo.name,
        classe = p.ativo.classe.name,
        gestora = p.ativo.gestora,
        emissor = p.ativo.emissor,
        vencimento = p.ativo.vencimento?.toString(),
        saldo = p.saldo.valor.t(),
        quantidade = p.quantidade?.valor?.stripTrailingZeros()?.toPlainString(),
        rentabilidadeMes = p.rentabilidadeMes?.valor?.t(),
        rentabilidadeAno = p.rentabilidadeAno?.valor?.t(),
        rentabilidade24Meses = p.rentabilidade24Meses?.valor?.t(),
        percentualCdiAno = p.percentualCdiAno?.valor?.t(),
        ritmo = a.ritmo?.ritmo?.name,
        ritmoDeltaAoAno = a.ritmo?.deltaAoAno?.t(),
        cenario = a.cenario.cenario.name,
        cenarioDriver = a.cenario.driver,
        dadoAConferirComAssessor = a.dadoAConferir,
        planilha = dados?.let { planilhaEntrada(p, it) },
    )
}

private fun planilhaEntrada(
    posicao: Posicao,
    dados: DadosDaPlanilha,
): PlanilhaEntrada {
    val resultado = resultadoDe(posicao, dados)
    return PlanilhaEntrada(
        valorAplicado = dados.valorAplicado?.t(),
        precoMedio = dados.precoMedio?.t(),
        rentabilidadeDesdeInicio = dados.rentabilidadeDesdeInicio?.t(),
        dataAplicacao = dados.dataAplicacao?.toString(),
        vencimento = dados.vencimento?.toString(),
        irEstimado = dados.ir?.t(),
        quantidadeMudouDesdeAPlanilha = dados.quantidadeMudou,
        custoEstimado = resultado?.custo?.t(),
        resultadoEstimado = resultado?.resultado?.t(),
        resultadoEstimadoPercentual = resultado?.percentual?.t(),
    )
}

/** Lacunas conhecidas da fonte (seção 2 da skill): o Claude não deve afirmar nada sobre elas. */
private val DADOS_AUSENTES =
    listOf(
        "Cotação, P/VP, dividend yield e vacância dos FIIs (dados de mercado ainda não integrados)",
        "Selic atual e expectativas de mercado",
        "Retornos de 3 e 6 meses e de 12 meses por ativo",
        "Indexador dos CRIs dentro dos FIIs de papel",
        "Composição interna dos fundos (sobreposição exata entre fundos e ETFs)",
        "Perfil do investidor e alocação-alvo (ainda não informados)",
    )

private const val SEM_PRECO_MEDIO = "Preço médio e valor aplicado (sem eles não há cálculo de lucro ou prejuízo)"

private const val SEM_CONTEXTO = "Índices de referência e evolução mensal (snapshot anterior à versão que guarda esses dados)"

private fun Money.t(): String = valor.toPlainString()

private fun Percent.t(): String = pontos.stripTrailingZeros().toPlainString()
