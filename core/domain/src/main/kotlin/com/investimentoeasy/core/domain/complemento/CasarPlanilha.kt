package com.investimentoeasy.core.domain.complemento

import com.investimentoeasy.core.domain.classificacao.ClassificadorAtivos
import com.investimentoeasy.core.model.ChaveAtivo
import com.investimentoeasy.core.model.ItemPlanilha
import com.investimentoeasy.core.model.Money
import com.investimentoeasy.core.model.Percent
import com.investimentoeasy.core.model.PlanilhaPosicao
import com.investimentoeasy.core.model.Posicao
import com.investimentoeasy.core.model.PosicaoExtraida
import com.investimentoeasy.core.model.ProventoPrevisto
import com.investimentoeasy.core.model.SecaoPlanilha
import com.investimentoeasy.core.model.Snapshot
import com.investimentoeasy.core.model.SnapshotId
import com.investimentoeasy.core.model.TipoAtivo
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/**
 * O que a planilha acrescenta a uma posição da base. Fica ao lado do snapshot, nunca dentro dele:
 * snapshot confirmado é imutável (R16).
 */
public data class DadosDaPlanilha(
    val chave: ChaveAtivo,
    val nomeNaPlanilha: String,
    val saldoNaPlanilha: Money,
    val quantidadeNaPlanilha: BigDecimal?,
    val valorAplicado: Money?,
    val precoMedio: Money?,
    val rentabilidadeDesdeInicio: Percent?,
    val valorLiquido: Money?,
    val ir: Money?,
    val iof: Money?,
    val taxa: String?,
    val dataAplicacao: LocalDate?,
    val vencimento: LocalDate?,
    /**
     * A quantidade da planilha é diferente da base: houve compra ou venda entre as duas datas, então
     * preço médio e valor aplicado podem não valer para a posição de hoje.
     */
    val quantidadeMudou: Boolean,
)

public data class ComplementoPlanilha(
    val snapshotId: SnapshotId,
    val dataPlanilha: LocalDate?,
    val recebidoEm: Instant,
    val dados: List<DadosDaPlanilha>,
    val proventos: List<ProventoPrevisto>,
) {
    public fun de(chave: ChaveAtivo): DadosDaPlanilha? = dados.firstOrNull { it.chave == chave }
}

/** Resultado do casamento, para o usuário conferir antes de guardar. */
public data class Casamento(
    val complemento: ComplementoPlanilha,
    /** Na planilha, mas não na base (vendido ou vencido entre as datas, ou nome que não casou). */
    val soNaPlanilha: List<ItemPlanilha>,
    /** Na base, sem dado da planilha. */
    val soNaBase: List<Posicao>,
) {
    val casadas: Int get() = complemento.dados.size
}

/**
 * Casa cada linha da planilha com uma posição da base.
 *
 * 1. Pela mesma chave R4 que o PDF gera: ticker, crédito (emissor + taxa + vencimento; a planilha
 *    traz a taxa numa coluna, então o nome é remontado como no PDF) e nome exato do fundo.
 * 2. Fundos que sobram: pelas palavras que identificam o fundo, sem as siglas de classe e de
 *    regulamento, que a XP escreve diferente no PDF e na planilha ("FIA" x "FIM RL").
 *    Só casa quando o par é único dos dois lados; na dúvida, fica de fora para o usuário ver.
 */
public class CasarPlanilha(
    private val classificador: ClassificadorAtivos = ClassificadorAtivos(),
) {
    public operator fun invoke(
        base: Snapshot,
        planilha: PlanilhaPosicao,
        agora: Instant,
    ): Casamento {
        val pares = mutableMapOf<ItemPlanilha, Posicao>()
        val porChave = base.posicoes.associateBy { it.ativo.chave.id }
        planilha.itens.forEach { item -> porChave[chaveDe(item).id]?.let { pares[item] = it } }
        casarFundosPorPalavras(planilha.itens - pares.keys, base.posicoes - pares.values.toSet(), pares)

        val dados = pares.map { (item, posicao) -> dados(item, posicao) }
        return Casamento(
            complemento = ComplementoPlanilha(base.id, planilha.dataPosicao, agora, dados, planilha.proventos),
            soNaPlanilha = planilha.itens.filter { it !in pares },
            soNaBase = base.posicoes.filter { it !in pares.values },
        )
    }

    private fun chaveDe(item: ItemPlanilha): ChaveAtivo {
        val nome = if (item.secao == SecaoPlanilha.RENDA_FIXA && item.taxa != null) "${item.nome} - ${item.taxa}" else item.nome
        val estrategia = item.subgrupo.replace('-', ' ')
        return classificador.classificar(
            PosicaoExtraida(nome, estrategia, item.saldo, item.quantidade, null, null, null, null, null, null, null),
        ).ativo.chave
    }

    private fun casarFundosPorPalavras(
        itens: List<ItemPlanilha>,
        posicoes: List<Posicao>,
        pares: MutableMap<ItemPlanilha, Posicao>,
    ) {
        val fundos = posicoes.filter { it.ativo.tipo == TipoAtivo.FUNDO }
        val candidatos = itens.filter { it.secao == SecaoPlanilha.FUNDOS }
        val melhorDoItem = candidatos.associateWith { item -> unicoMelhor(palavras(item.nome), fundos) { palavras(it.ativo.nome) } }
        melhorDoItem.forEach { (item, posicao) ->
            if (posicao == null) return@forEach
            val melhorDaPosicao = unicoMelhor(palavras(posicao.ativo.nome), candidatos) { palavras(it.nome) }
            if (melhorDaPosicao == item) pares[item] = posicao
        }
    }

    private fun <T> unicoMelhor(
        alvo: Set<String>,
        opcoes: List<T>,
        palavrasDe: (T) -> Set<String>,
    ): T? {
        val notas = opcoes.map { it to semelhanca(alvo, palavrasDe(it)) }.filter { it.second >= SEMELHANCA_MINIMA }
        val maior = notas.maxOfOrNull { it.second } ?: return null
        return notas.filter { it.second == maior }.singleOrNull()?.first
    }

    private fun dados(
        item: ItemPlanilha,
        posicao: Posicao,
    ): DadosDaPlanilha {
        val quantidadeBase = posicao.quantidade?.valor
        val quantidadeMudou =
            quantidadeBase != null && item.quantidade != null && quantidadeBase.compareTo(item.quantidade) != 0
        return DadosDaPlanilha(
            chave = posicao.ativo.chave,
            nomeNaPlanilha = item.nome,
            saldoNaPlanilha = item.saldo,
            quantidadeNaPlanilha = item.quantidade,
            valorAplicado = item.valorAplicado,
            precoMedio = item.precoMedio,
            rentabilidadeDesdeInicio = item.rentabilidadeDesdeInicio,
            valorLiquido = item.valorLiquido,
            ir = item.ir,
            iof = item.iof,
            taxa = item.taxa,
            dataAplicacao = item.dataAplicacao,
            vencimento = item.vencimento,
            quantidadeMudou = quantidadeMudou,
        )
    }

    internal companion object {
        private const val SEMELHANCA_MINIMA = 0.6

        /** Siglas de classe, regulamento e ligação que não identificam o fundo. */
        private val SIGLAS =
            setOf(
                "FI", "FIC", "FIF", "FIM", "FIA", "CIA", "RL", "RF", "CP", "LP", "FIRF", "MULTI", "MULTIMERCADO", "INFRA", "IE",
                "DE", "DA", "DO", "EM", "E",
            )
        private val SEPARADORES = Regex("""[^A-Z0-9]+""")

        internal fun palavras(nome: String): Set<String> =
            ClassificadorAtivos.normalizar(nome).split(SEPARADORES).filter { it.isNotEmpty() && it !in SIGLAS }.toSet()

        /** Jaccard: palavras em comum sobre palavras no total. */
        internal fun semelhanca(
            a: Set<String>,
            b: Set<String>,
        ): Double = if (a.isEmpty() || b.isEmpty()) 0.0 else (a intersect b).size.toDouble() / (a union b).size
    }
}
