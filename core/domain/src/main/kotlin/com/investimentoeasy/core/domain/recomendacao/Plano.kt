package com.investimentoeasy.core.domain.recomendacao

import com.investimentoeasy.core.domain.analise.RegraAlerta
import com.investimentoeasy.core.domain.analise.Severidade
import com.investimentoeasy.core.domain.mercado.MotivoQueda
import com.investimentoeasy.core.domain.recomendacao.TextoBr.pct
import com.investimentoeasy.core.domain.recomendacao.TextoBr.reais

/**
 * Até cinco ações, na ordem da skill: urgências, colchão, a maior realocação, FII com queda
 * própria (vira pergunta, não decisão), a sugestão mais prioritária e dados a conferir.
 */
internal fun plano30Dias(
    ctx: Contexto,
    realocacoes: List<Realocacao>,
    novos: List<NovoAtivo>,
): List<Acao> {
    val alertas = ctx.analise.alertas
    val urgentes =
        alertas.filter { it.severidade == Severidade.URGENTE }.map { a ->
            when (a.regra) {
                RegraAlerta.POSICAO_FANTASMA ->
                    Acao(
                        "Resolver ${a.ativos.joinToString()}",
                        "Quantidade sem saldo no extrato: confirme com o assessor.",
                    )
                RegraAlerta.VENCIMENTO_PROXIMO -> Acao("Definir o destino de ${a.ativos.joinToString()}", a.titulo + ".")
                RegraAlerta.CONCENTRACAO_GESTORA ->
                    Acao("Reduzir a concentração em uma gestora", "${a.titulo}: ${a.percentual?.let { pct(it) } ?: ""} da carteira.")
                else -> Acao(a.titulo, a.ativos.joinToString())
            }
        }
    val colchao =
        novos.firstOrNull { it.motivo == MotivoSugestao.COLCHAO_DE_LIQUIDEZ }?.let {
            Acao("Montar o colchão de liquidez", "Levar ${reais(it.valor)} para ${it.sugestao}: seis meses da mediana dos saques.")
        }
    val realocacao =
        realocacoes.firstOrNull()?.let {
            Acao("Reduzir ${it.vender}", "Vender ${reais(it.valor)} e levar para ${it.destino}.")
        }
    val investigar =
        ctx.fiis.filter { it.motivoQueda == MotivoQueda.ESPECIFICA }.map { fii ->
            val ifix = ctx.panorama?.ifix?.retorno12Meses
            Acao(
                "Investigar ${fii.ticker} antes de decidir",
                "Caiu ${fii.retorno12Meses?.let { pct(it) }} em 12 meses contra ${ifix?.let { pct(it) }} do IFIX: " +
                    "procure fato relevante e relatório gerencial.",
            )
        }
    val novo =
        novos.firstOrNull { it.motivo != MotivoSugestao.COLCHAO_DE_LIQUIDEZ }?.let {
            Acao("Começar ${it.sugestao}", "Aporte de ${reais(it.valor)} em ${it.grupo.rotulo.lowercase()}.")
        }
    val conferir =
        alertas.filter { it.regra == RegraAlerta.DADO_A_CONFERIR }.map {
            Acao("Conferir ${it.ativos.joinToString()} com o assessor", "O relatório traz rentabilidade fora do plausível.")
        }
    return (urgentes + listOfNotNull(colchao, realocacao) + investigar + listOfNotNull(novo) + conferir).take(MAXIMO_ACOES)
}

private const val MAXIMO_ACOES = 5
