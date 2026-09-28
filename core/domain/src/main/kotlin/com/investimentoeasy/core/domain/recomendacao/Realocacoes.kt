package com.investimentoeasy.core.domain.recomendacao

import com.investimentoeasy.core.domain.alocacao.GrupoAlocacao
import com.investimentoeasy.core.domain.analise.AtivoAnalisado
import com.investimentoeasy.core.domain.analise.Cenario
import com.investimentoeasy.core.domain.complemento.resultadoDe
import com.investimentoeasy.core.domain.mercado.MotivoQueda
import com.investimentoeasy.core.domain.recomendacao.TextoBr.pct
import com.investimentoeasy.core.domain.recomendacao.TextoBr.reais
import com.investimentoeasy.core.model.ChaveAtivo
import com.investimentoeasy.core.model.Money
import com.investimentoeasy.core.model.Posicao
import com.investimentoeasy.core.model.TipoAtivo
import java.math.BigDecimal

/**
 * Grupos acima do máximo do perfil voltam para a faixa. Vende primeiro o que tem cenário adverso
 * ou queda específica, depois as maiores posições; no máximo duas por grupo. O destino é o grupo
 * mais abaixo do perfil (ou o colchão de liquidez, se faltar).
 */
internal fun realocacoes(ctx: Contexto): List<Realocacao> {
    val todos = desvios(ctx)
    val destinoPadrao = destino(ctx, todos)
    return todos.filter { it.acimaDoMaximo }.flatMap { d ->
        var restante = ctx.valorDePontos(d.excesso)
        if (restante < VALOR_MINIMO_VENDA) return@flatMap emptyList()
        candidatos(ctx, d.grupo).take(MAXIMO_POR_GRUPO).mapNotNull { a ->
            val valor = minOf(restante, arredondar(a.posicao.saldo.valor))
            if (valor < VALOR_MINIMO_VENDA) return@mapNotNull null
            restante -= valor
            Realocacao(
                vender = a.posicao.ativo.nome,
                motivo = motivo(ctx, d, a.posicao, a.cenario.cenario == Cenario.ADVERSO, a.cenario.driver),
                destino = destinoPadrao,
                valor = valor,
            )
        }
    }
}

private fun candidatos(
    ctx: Contexto,
    grupo: GrupoAlocacao,
) = ctx.analise.ativos
    .filter { ctx.grupoDoPerfil(GrupoAlocacao.de(it.posicao.ativo.classe)) == grupo }
    .filter { !it.dadoAConferir && !it.posicao.saldo.valor.isZero }
    .sortedWith(
        compareByDescending<AtivoAnalisado> { pontosParaVender(ctx, it) }
            .thenByDescending { it.posicao.saldo.valor },
    )

/**
 * Quanto mais pontos, mais cedo sai: cenário adverso, queda própria do FII e renda encolhendo
 * pesam a favor; FII com desconto em queda cíclica pesa contra (vender realizaria a queda), e
 * FII que distribui acima do CDI também (é a renda que a carteira quer manter).
 */
private fun pontosParaVender(
    ctx: Contexto,
    ativo: AtivoAnalisado,
): Int {
    val ticker = (ativo.posicao.ativo.chave as? ChaveAtivo.Ticker)?.codigo
    val fii = ctx.fiis.firstOrNull { it.ticker == ticker }
    var pontos = 0
    if (ativo.cenario.cenario == Cenario.ADVERSO) pontos += 2
    if (fii?.motivoQueda == MotivoQueda.ESPECIFICA) pontos += 2
    if (fii?.distribuicaoEncolhendo == true) pontos += 1
    if (fii?.motivoQueda == MotivoQueda.CICLICA && fii.pvp < BigDecimal.ONE) pontos -= 2
    val dy = fii?.dividendYield12Meses
    val cdi = ctx.cdi12Meses
    if (dy != null && cdi != null && dy > cdi) pontos -= 1
    return pontos
}

private fun destino(
    ctx: Contexto,
    desvios: List<Desvio>,
): String {
    if (ctx.faltaColchao()) return "Tesouro Selic (colchão de liquidez)"
    val grupo = desvios.filter { it.falta.pontos.signum() > 0 }.maxByOrNull { it.falta }?.grupo ?: GrupoAlocacao.RENDA_FIXA_POS
    return "${sugestaoPara(ctx, grupo)} (${grupo.rotuloNaFrase})"
}

private fun motivo(
    ctx: Contexto,
    d: Desvio,
    posicao: Posicao,
    adverso: Boolean,
    driver: String,
): String {
    val partes =
        listOfNotNull(
            "${d.grupo.rotulo} em ${pct(d.atual)}, acima do máximo de ${pct(d.maximo, 0)} do perfil ${ctx.perfil.rotulo.lowercase()}",
            "cenário adverso: $driver".takeIf { adverso },
            efeitoFiscal(ctx, posicao),
        )
    return partes.joinToString("; ") + "."
}

/** Com a planilha, diz se a venda realiza lucro ou prejuízo e a regra de IR da classe. */
private fun efeitoFiscal(
    ctx: Contexto,
    posicao: Posicao,
): String? {
    val resultado = resultadoDe(posicao, ctx.complemento?.de(posicao.ativo.chave)) ?: return null
    val regra =
        when (posicao.ativo.tipo) {
            TipoAtivo.FII -> "FII paga 20% sobre o ganho, sem isenção"
            TipoAtivo.ACAO -> "ações: isento se as vendas do mês somarem até R$ 20 mil; acima, 15%"
            else -> "15% de IR sobre o ganho"
        }
    return if (resultado.resultado.isNegative) {
        "realiza prejuízo estimado de ${reais(resultado.resultado.abs())}, que compensa ganhos futuros da mesma classe"
    } else {
        "realiza lucro estimado de ${reais(resultado.resultado)} ($regra)"
    }
}

internal fun Contexto.faltaColchao(): Boolean =
    analise.lacunas.any { it.tipo == com.investimentoeasy.core.domain.analise.TipoLacuna.SEM_LIQUIDEZ_COM_SAQUES }

private fun arredondar(valor: Money): Money = Money.of(valor.valor.divide(CEM, 0, java.math.RoundingMode.DOWN).multiply(CEM))

private val CEM = BigDecimal(100)
internal val VALOR_MINIMO: Money = Money.of("500.00")

/** Venda menor que isso não muda a alocação e custa corretagem e atenção. */
private val VALOR_MINIMO_VENDA: Money = Money.of("1000.00")
private const val MAXIMO_POR_GRUPO = 2
