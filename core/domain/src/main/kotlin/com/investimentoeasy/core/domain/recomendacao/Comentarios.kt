package com.investimentoeasy.core.domain.recomendacao

import com.investimentoeasy.core.domain.analise.RegraAlerta
import com.investimentoeasy.core.domain.recomendacao.TextoBr.pct
import com.investimentoeasy.core.domain.recomendacao.TextoBr.reais

/** Um comentário por alerta, com os números que o sustentam. */
internal fun comentariosDosAlertas(ctx: Contexto): Map<RegraAlerta, String> =
    ctx.analise.alertas.groupBy { it.regra }.mapValues { (regra, alertas) ->
        val a = alertas.first()
        val quanto = listOfNotNull(a.valor?.let(::reais), a.percentual?.let { pct(it) }).joinToString(", ")
        when (regra) {
            RegraAlerta.EXPOSICAO_GLOBAL ->
                "$quanto em renda variável global, acima do limite de 30%: uma queda da bolsa americana atinge tudo junto."
            RegraAlerta.CONCENTRACAO_GESTORA -> "$quanto em uma só gestora: um problema operacional dela afeta várias posições."
            RegraAlerta.CAIXA_ZERO_COM_SAQUES -> "Sem caixa, o próximo saque obriga a vender algo, talvez na hora errada."
            RegraAlerta.EMISSOR_ACIMA_DO_FGC -> "O FGC cobre R$ 250 mil por CPF e instituição; o que passar disso fica sem garantia."
            RegraAlerta.VENCIMENTO_PROXIMO -> "Sem destino definido, o dinheiro do vencimento fica parado na conta."
            RegraAlerta.POSICAO_FANTASMA -> "Pode ser direito de subscrição não exercido ou resíduo de evento: só o assessor confirma."
            RegraAlerta.FUNDO_ABAIXO_DO_CDI ->
                "${alertas.size} ${if (alertas.size == 1) "fundo rendeu" else "fundos renderam"} menos que o CDI no ano; " +
                    "compare com a taxa de administração antes de decidir."
            RegraAlerta.POSICAO_SIMBOLICA ->
                "Posições pequenas demais não mudam o resultado e complicam a declaração: aportar ou consolidar."
            RegraAlerta.DADO_A_CONFERIR -> "Número fora do plausível no relatório; o app não tira conclusão sobre ele."
            RegraAlerta.PERFORMANCE_EXCEPCIONAL ->
                "Bom desempenho recente; vale revisar se a posição não cresceu além do que o perfil comporta."
        }
    }
