package com.investimentoeasy.core.domain.recomendacao

import com.investimentoeasy.core.domain.alocacao.GrupoAlocacao
import com.investimentoeasy.core.domain.analise.TipoLacuna
import com.investimentoeasy.core.domain.mercado.CicloJuros
import com.investimentoeasy.core.domain.mercado.DirecaoCambio
import com.investimentoeasy.core.domain.recomendacao.TextoBr.pct
import com.investimentoeasy.core.domain.recomendacao.TextoBr.taxa
import com.investimentoeasy.core.model.Money
import com.investimentoeasy.core.model.Percent
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * Sugestões de compra só para lacunas reais: colchão de liquidez (com saques), grupos abaixo do
 * mínimo do perfil e prefixado zerado. O tamanho leva o grupo até o alvo do perfil.
 */
internal fun novosAtivos(ctx: Contexto): List<NovoAtivo> {
    val colchao = colchao(ctx)
    val abaixo =
        desvios(ctx).filter { it.abaixoDoMinimo }.sortedByDescending { it.falta }.mapNotNull { d ->
            val valor = ctx.valorDePontos(d.falta).takeIf { it >= VALOR_MINIMO } ?: return@mapNotNull null
            val motivo =
                when (d.grupo) {
                    GrupoAlocacao.RENDA_FIXA_IPCA -> MotivoSugestao.PROTECAO_INFLACAO
                    GrupoAlocacao.RENDA_FIXA_PRE -> MotivoSugestao.SEM_PREFIXADO
                    else -> MotivoSugestao.ABAIXO_DO_PERFIL
                }
            val metade = Percent.of(d.minimo.pontos.divide(DOIS, 2, RoundingMode.HALF_EVEN))
            NovoAtivo(
                sugestao = sugestaoPara(ctx, d.grupo),
                grupo = d.grupo,
                motivo = motivo,
                prioridade = if (d.atual < metade) Prioridade.ALTA else Prioridade.MEDIA,
                valor = valor,
                justificativa =
                    listOfNotNull(
                        "${d.grupo.rotulo} em ${pct(d.atual)}, abaixo do mínimo de ${pct(d.minimo, 0)} do perfil; " +
                            "o valor leva ao alvo de ${pct(d.alvo, 0)}",
                        contextoDoCiclo(ctx, d.grupo),
                    ).joinToString(". ") + ".",
            )
        }
    val semPre =
        prefixadoZerado(ctx).takeIf { abaixo.none { it.grupo == GrupoAlocacao.RENDA_FIXA_PRE } }
    return listOfNotNull(colchao) + abaixo + listOfNotNull(semPre)
}

/**
 * Seis meses da mediana dos resgates mensais dos últimos 12 meses, em Tesouro Selic. A mediana
 * ignora um saque grande isolado (compra de imóvel, por exemplo), que inflaria a média.
 */
private fun colchao(ctx: Contexto): NovoAtivo? {
    if (!ctx.faltaColchao()) return null
    val saques = ctx.analise.saques ?: return null
    val mensal = medianaDosResgates(ctx) ?: return null
    val valor = Money.of(mensal.valor.multiply(BigDecimal(MESES_DE_COLCHAO)).divide(CEM, 0, RoundingMode.DOWN).multiply(CEM))
    if (valor < VALOR_MINIMO) return null
    return NovoAtivo(
        sugestao = "Tesouro Selic",
        grupo = GrupoAlocacao.RENDA_FIXA_POS,
        motivo = MotivoSugestao.COLCHAO_DE_LIQUIDEZ,
        prioridade = Prioridade.ALTA,
        valor = valor,
        justificativa =
            "Caixa zero com ${TextoBr.reais(saques.resgates12Meses)} resgatados em 12 meses (${saques.mesesComResgate} meses com saque, " +
                "mediana de ${TextoBr.reais(mensal)} por mês): o colchão cobre seis meses sem vender nada no momento errado.",
    )
}

private fun medianaDosResgates(ctx: Contexto): Money? {
    val evolucao = ctx.snapshot.contexto?.evolucaoMensal.orEmpty()
    val ultimo = YearMonth.from(ctx.snapshot.dataReferencia)
    val resgates =
        evolucao
            .filter { ChronoUnit.MONTHS.between(it.mes, ultimo) in 0 until DOZE && it.movimentacoes.isNegative }
            .map { it.movimentacoes.abs().valor }
            .sorted()
    if (resgates.isEmpty()) return null
    val meio = resgates.size / 2
    val mediana =
        if (resgates.size % 2 == 1) {
            resgates[meio]
        } else {
            (resgates[meio - 1] + resgates[meio]).divide(
                DOIS,
                2,
                RoundingMode.HALF_EVEN,
            )
        }
    return Money.of(mediana)
}

private fun prefixadoZerado(ctx: Contexto): NovoAtivo? {
    if (ctx.analise.lacunas.none { it.tipo == TipoLacuna.SEM_PREFIXADO }) return null
    val faixa = ctx.alvos[GrupoAlocacao.RENDA_FIXA_PRE] ?: return null
    val valor = ctx.valorDePontos(faixa.alvo).takeIf { it >= VALOR_MINIMO } ?: return null
    return NovoAtivo(
        sugestao = sugestaoPara(ctx, GrupoAlocacao.RENDA_FIXA_PRE),
        grupo = GrupoAlocacao.RENDA_FIXA_PRE,
        motivo = MotivoSugestao.SEM_PREFIXADO,
        prioridade = if (ctx.juros?.ciclo == CicloJuros.CORTE) Prioridade.MEDIA else Prioridade.ESPECULATIVO,
        valor = valor,
        justificativa =
            listOfNotNull(
                "Nenhuma posição prefixada; o alvo do perfil é ${pct(faixa.alvo, 0)}",
                contextoDoCiclo(ctx, GrupoAlocacao.RENDA_FIXA_PRE),
            ).joinToString(". ") + ".",
    )
}

/** Produto sugerido por grupo (lista da skill); se o ticker já está na carteira, "aumentar". */
internal fun sugestaoPara(
    ctx: Contexto,
    grupo: GrupoAlocacao,
): String {
    val opcoes =
        when (grupo) {
            GrupoAlocacao.RENDA_FIXA_IPCA -> listOf("Tesouro IPCA+", "IMAB11")
            GrupoAlocacao.RENDA_FIXA_PRE -> listOf("Tesouro Prefixado", "IRFM11")
            GrupoAlocacao.RENDA_FIXA_POS, GrupoAlocacao.CAIXA -> listOf("Tesouro Selic")
            GrupoAlocacao.FIIS -> listOf("BTLG11 (logística)", "KNIP11 (papel IPCA)")
            GrupoAlocacao.ACOES_BRASIL ->
                if (ctx.juros?.ciclo == CicloJuros.CORTE) listOf("PIBB11", "SMAL11 (small caps)") else listOf("PIBB11")
            GrupoAlocacao.EXTERIOR -> listOf("WRLD11 (global fora dos EUA)")
            GrupoAlocacao.MULTIMERCADO -> listOf("multimercado de gestora diferente das atuais")
            GrupoAlocacao.A_CLASSIFICAR -> listOf("Tesouro Selic")
        }
    return opcoes.joinToString(" ou ") { opcao ->
        val ticker = opcao.substringBefore(" ")
        if (ctx.temTicker(ticker)) "aumentar $opcao" else opcao
    }
}

/** Uma frase ligando o grupo ao ciclo de juros ou ao câmbio que o Focus indica. */
internal fun contextoDoCiclo(
    ctx: Contexto,
    grupo: GrupoAlocacao,
): String? {
    if (grupo == GrupoAlocacao.EXTERIOR) return contextoDoCambio(ctx)
    val juros = ctx.juros ?: return null
    val focus = "Com o Focus projetando Selic de ${taxa(juros.selicAtual)} para ${taxa(juros.selicEsperada)} no fim de ${juros.anoEsperado}"
    val efeito = EFEITO_DO_CICLO[grupo to juros.ciclo] ?: return null
    return "$focus, $efeito"
}

private fun contextoDoCambio(ctx: Contexto): String? {
    val c = ctx.cambio?.takeIf { it.direcao == DirecaoCambio.REAL_MAIS_FRACO } ?: return null
    return "O Focus espera dólar de R$ ${TextoBr.decimal(c.dolarAtual)} para R$ ${TextoBr.decimal(c.dolarEsperado)} no fim de ${c.ano}"
}

private val EFEITO_DO_CICLO =
    mapOf(
        (GrupoAlocacao.RENDA_FIXA_IPCA to CicloJuros.CORTE) to "IPCA+ tende a ganhar marcação",
        (GrupoAlocacao.RENDA_FIXA_IPCA to CicloJuros.ALTA) to "IPCA+ oscila no curto prazo; vale para quem leva até o vencimento",
        (GrupoAlocacao.RENDA_FIXA_PRE to CicloJuros.CORTE) to "prefixado trava a taxa antes do corte",
        (GrupoAlocacao.ACOES_BRASIL to CicloJuros.CORTE) to "juro em queda costuma favorecer a bolsa",
        (GrupoAlocacao.RENDA_FIXA_POS to CicloJuros.ALTA) to "o carrego do pós-fixado aumenta",
    )

private const val DOZE = 12L
private const val MESES_DE_COLCHAO = 6
private val DOIS = BigDecimal(2)
private val CEM = BigDecimal(100)
