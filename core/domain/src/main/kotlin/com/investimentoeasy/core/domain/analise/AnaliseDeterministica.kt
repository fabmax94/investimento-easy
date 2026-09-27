package com.investimentoeasy.core.domain.analise

import com.investimentoeasy.core.domain.alocacao.FatiaAlocacao
import com.investimentoeasy.core.domain.alocacao.GrupoAlocacao
import com.investimentoeasy.core.domain.alocacao.alocacaoPorGrupo
import com.investimentoeasy.core.domain.validacao.ValidadorExtracao
import com.investimentoeasy.core.model.ClasseAtivo
import com.investimentoeasy.core.model.IndiceReferencia
import com.investimentoeasy.core.model.Money
import com.investimentoeasy.core.model.Percent
import com.investimentoeasy.core.model.Posicao
import com.investimentoeasy.core.model.Snapshot
import com.investimentoeasy.core.model.TipoAtivo
import com.investimentoeasy.core.model.soma
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** R23: limites padrão da skill; o usuário poderá mudá-los no perfil. */
public data class LimitesAnalise(
    val concentracaoGestora: Percent = Percent.of("40"),
    val exposicaoGlobal: Percent = Percent.of("30"),
    val limiteFgcPorEmissor: Money = Money.of("250000.00"),
    val mesesAteVencimento: Long = 6,
    val posicaoSimbolica: Money = Money.of("500.00"),
    val saquesSobrePatrimonio: Percent = Percent.of("20"),
    val destaquePercentualCdi: Percent = Percent.of("150"),
    val ipcaMinimo: Percent = Percent.of("5"),
)

public data class AtivoAnalisado(
    val posicao: Posicao,
    val ritmo: RitmoAtivo?,
    val cenario: CenarioAtivo,
    /** R6: dado inconsistente no relatório; fora do Ritmo até conferir com o assessor. */
    val dadoAConferir: Boolean,
)

public data class Concentracao(
    val nome: String,
    val valor: Money,
    val percentual: Percent,
    val quantidadeAtivos: Int,
)

public data class Vencimento(
    val posicao: Posicao,
    val vencimento: YearMonth,
    val mesesRestantes: Long,
)

public data class PadraoDeSaques(
    /** Soma das movimentações negativas nos últimos 12 meses (valor positivo). */
    val resgates12Meses: Money,
    val percentualDoPatrimonio: Percent?,
    val mesesComResgate: Int,
    /** R19: acima do limite, o objetivo sugerido passa a ser consumo. */
    val sugereConsumo: Boolean,
)

/** Lacunas calculadas: toda sugestão de compra da Camada 3 precisa apontar para uma delas. */
public enum class TipoLacuna { PROTECAO_INFLACAO, SEM_PREFIXADO, SEM_LIQUIDEZ_COM_SAQUES }

public data class Lacuna(
    val tipo: TipoLacuna,
    val descricao: String,
    val percentualAtual: Percent?,
)

/** Camada 1 completa: só números calculados a partir do snapshot e do contexto do relatório. */
public data class AnaliseDeterministica(
    val dataReferencia: LocalDate,
    val total: Money,
    val alocacao: List<FatiaAlocacao>,
    val ativos: List<AtivoAnalisado>,
    val gestoras: List<Concentracao>,
    val emissoresGarantidos: List<Concentracao>,
    val exposicaoGlobal: Percent?,
    val vencimentos: List<Vencimento>,
    val simbolicas: List<Posicao>,
    val fundosAbaixoDoCdi: List<AtivoAnalisado>,
    val saques: PadraoDeSaques?,
    val mercado: List<IndiceReferencia>,
    val lacunas: List<Lacuna>,
    val alertas: List<AlertaAnalise>,
)

/** Camadas 1 e 2 do motor de análise: o código calcula e as regras alertam. */
public class AnalisarCarteira(
    private val limites: LimitesAnalise = LimitesAnalise(),
) {
    public operator fun invoke(snapshot: Snapshot): AnaliseDeterministica {
        val posicoes = snapshot.posicoes
        val total = posicoes.map { it.saldo.valor }.soma()
        val foraDoRitmo = ValidadorExtracao().validar(posicoes, null, true).chavesForaDoRitmo
        val ativos =
            posicoes.map { p ->
                val conferir = p.ativo.chave in foraDoRitmo
                AtivoAnalisado(
                    posicao = p,
                    ritmo =
                        if (conferir) {
                            null
                        } else {
                            ritmo(
                                p.rentabilidadeAno?.valor,
                                p.rentabilidade24Meses?.valor,
                                snapshot.dataReferencia,
                            )
                        },
                    cenario = cenario(p.ativo),
                    dadoAConferir = conferir,
                )
            }
        val alocacao = alocacaoPorGrupo(posicoes)
        val base =
            AnaliseDeterministica(
                dataReferencia = snapshot.dataReferencia,
                total = total,
                alocacao = alocacao,
                ativos = ativos,
                gestoras =
                    concentracao(
                        posicoes.filter { it.ativo.tipo == TipoAtivo.FUNDO },
                        total,
                    ) { it.ativo.gestora ?: GESTORA_NAO_IDENTIFICADA },
                emissoresGarantidos =
                    concentracao(posicoes.filter { it.ativo.tipo in TIPOS_FGC && it.ativo.emissor != null }, total) { it.ativo.emissor!! },
                exposicaoGlobal =
                    percentual(
                        posicoes.filter { it.ativo.classe == ClasseAtivo.RV_GLOBAL }.map { it.saldo.valor }.soma(),
                        total,
                    ),
                vencimentos = vencimentos(posicoes, snapshot.dataReferencia),
                simbolicas = posicoes.filter { !it.saldo.valor.isZero && it.saldo.valor < limites.posicaoSimbolica },
                fundosAbaixoDoCdi =
                    ativos.filter { a ->
                        a.posicao.ativo.tipo == TipoAtivo.FUNDO && !a.dadoAConferir &&
                            (a.posicao.percentualCdiAno?.valor?.let { it < CEM_POR_CENTO } ?: false)
                    },
                saques = saques(snapshot, total),
                mercado = snapshot.contexto?.referencias.orEmpty(),
                lacunas = emptyList(),
                alertas = emptyList(),
            )
        val comLacunas = base.copy(lacunas = lacunas(base))
        return comLacunas.copy(alertas = RegrasDeAlerta(limites).avaliar(comLacunas))
    }

    private fun concentracao(
        posicoes: List<Posicao>,
        total: Money,
        chave: (Posicao) -> String,
    ): List<Concentracao> =
        posicoes
            .groupBy(chave)
            .map { (nome, doGrupo) ->
                val valor = doGrupo.map { it.saldo.valor }.soma()
                Concentracao(nome, valor, percentual(valor, total) ?: Percent.ZERO, doGrupo.size)
            }.sortedByDescending { it.valor }

    private fun vencimentos(
        posicoes: List<Posicao>,
        data: LocalDate,
    ): List<Vencimento> {
        val mesReferencia = YearMonth.from(data)
        return posicoes
            .mapNotNull { p -> p.ativo.vencimento?.let { Vencimento(p, it, ChronoUnit.MONTHS.between(mesReferencia, it)) } }
            .filter { it.mesesRestantes in 0..limites.mesesAteVencimento }
            .sortedBy { it.vencimento }
    }

    /** R19 a partir da evolução patrimonial do relatório (últimos 12 meses informados). */
    private fun saques(
        snapshot: Snapshot,
        total: Money,
    ): PadraoDeSaques? {
        val evolucao = snapshot.contexto?.evolucaoMensal?.takeIf { it.isNotEmpty() } ?: return null
        val ultimoMes = YearMonth.from(snapshot.dataReferencia)
        val janela = evolucao.filter { ChronoUnit.MONTHS.between(it.mes, ultimoMes) in 0 until MESES_JANELA_SAQUES }
        val resgates = janela.filter { it.movimentacoes.isNegative }
        val soma = resgates.map { it.movimentacoes.abs() }.soma()
        val patrimonio = snapshot.patrimonioInformado?.valor ?: total
        val percentual = percentual(soma, patrimonio)
        return PadraoDeSaques(
            resgates12Meses = soma,
            percentualDoPatrimonio = percentual,
            mesesComResgate = resgates.size,
            sugereConsumo = percentual != null && percentual > limites.saquesSobrePatrimonio,
        )
    }

    private fun lacunas(analise: AnaliseDeterministica): List<Lacuna> {
        fun percentualDo(grupo: GrupoAlocacao) = analise.alocacao.firstOrNull { it.grupo == grupo }?.percentual ?: Percent.ZERO
        val ipca = percentualDo(GrupoAlocacao.RENDA_FIXA_IPCA)
        val pre = percentualDo(GrupoAlocacao.RENDA_FIXA_PRE)
        val caixa = percentualDo(GrupoAlocacao.CAIXA)
        return listOfNotNull(
            Lacuna(TipoLacuna.PROTECAO_INFLACAO, "Proteção contra inflação (IPCA+) abaixo do mínimo", ipca)
                .takeIf { ipca < limites.ipcaMinimo },
            Lacuna(TipoLacuna.SEM_PREFIXADO, "Nenhuma posição em renda fixa prefixada", pre).takeIf { pre == Percent.ZERO },
            Lacuna(TipoLacuna.SEM_LIQUIDEZ_COM_SAQUES, "Caixa zero com histórico de resgates: falta colchão de liquidez", caixa)
                .takeIf { caixa == Percent.ZERO && (analise.saques?.mesesComResgate ?: 0) > 0 },
        )
    }

    public companion object {
        public const val GESTORA_NAO_IDENTIFICADA: String = "Gestora não identificada"

        /** Produtos cobertos pelo FGC (por CPF e por instituição). */
        public val TIPOS_FGC: Set<TipoAtivo> = setOf(TipoAtivo.CDB, TipoAtivo.LCI, TipoAtivo.LCA, TipoAtivo.LC)
        private val CEM_POR_CENTO = Percent.of("100")
        private const val MESES_JANELA_SAQUES = 12L
    }
}

internal fun percentual(
    valor: Money,
    total: Money,
): Percent? = valor.fracaoDe(total)?.let(Percent::daFracao)
