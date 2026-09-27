package com.investimentoeasy.core.domain.analise

import com.investimentoeasy.core.model.ClasseAtivo
import com.investimentoeasy.core.model.Money
import com.investimentoeasy.core.model.Percent

/** Severidades da skill; push (Fase 2) só nas duas mais altas. */
public enum class Severidade { URGENTE, CRITICO, ATENCAO, INFO, DESTAQUE }

public enum class RegraAlerta {
    CONCENTRACAO_GESTORA,
    VENCIMENTO_PROXIMO,
    POSICAO_FANTASMA,
    EXPOSICAO_GLOBAL,
    CAIXA_ZERO_COM_SAQUES,
    EMISSOR_ACIMA_DO_FGC,
    FUNDO_ABAIXO_DO_CDI,
    POSICAO_SIMBOLICA,
    DADO_A_CONFERIR,
    PERFORMANCE_EXCEPCIONAL,
}

/**
 * Um alerta com os números que o sustentam (todos calculados ou do relatório). O título não
 * leva números: a interface formata [valor] e [percentual] no padrão brasileiro.
 */
public data class AlertaAnalise(
    val severidade: Severidade,
    val regra: RegraAlerta,
    val titulo: String,
    val ativos: List<String> = emptyList(),
    val valor: Money? = null,
    val percentual: Percent? = null,
)

/** Camada 2: as regras de alerta da seção 8 da skill, com limites configuráveis (R23). */
public class RegrasDeAlerta(
    limites: LimitesAnalise,
) {
    private val concentracao = RegrasDeConcentracao(limites)
    private val posicoes = RegrasDePosicao(limites)

    public fun avaliar(a: AnaliseDeterministica): List<AlertaAnalise> =
        (concentracao.avaliar(a) + posicoes.avaliar(a)).sortedBy { it.severidade }
}

/** Concentração e liquidez: gestora, renda variável global, emissor/FGC, caixa com saques. */
private class RegrasDeConcentracao(
    private val limites: LimitesAnalise,
) {
    fun avaliar(a: AnaliseDeterministica): List<AlertaAnalise> = gestoras(a) + global(a) + fgc(a) + caixa(a)

    private fun gestoras(a: AnaliseDeterministica) =
        a.gestoras
            .filter { it.nome != AnalisarCarteira.GESTORA_NAO_IDENTIFICADA && it.percentual > limites.concentracaoGestora }
            .map {
                AlertaAnalise(
                    Severidade.URGENTE,
                    RegraAlerta.CONCENTRACAO_GESTORA,
                    "Concentração em fundos da gestora ${it.nome}",
                    valor = it.valor,
                    percentual = it.percentual,
                )
            }

    private fun global(a: AnaliseDeterministica) =
        listOfNotNull(
            a.exposicaoGlobal?.takeIf { it > limites.exposicaoGlobal }?.let {
                AlertaAnalise(
                    Severidade.CRITICO,
                    RegraAlerta.EXPOSICAO_GLOBAL,
                    "Exposição alta a renda variável global, provavelmente correlacionada " +
                        "(sem a carteira dos fundos, a sobreposição exata não é medida)",
                    ativos = a.ativos.filter { x -> x.posicao.ativo.classe == ClasseAtivo.RV_GLOBAL }.map { x -> x.posicao.ativo.nome },
                    percentual = it,
                )
            },
        )

    private fun fgc(a: AnaliseDeterministica) =
        a.emissoresGarantidos.filter { it.valor > limites.limiteFgcPorEmissor }.map {
            AlertaAnalise(
                Severidade.CRITICO,
                RegraAlerta.EMISSOR_ACIMA_DO_FGC,
                "Exposição ao emissor ${it.nome} acima do limite do FGC",
                valor = it.valor,
            )
        }

    private fun caixa(a: AnaliseDeterministica) =
        a.lacunas.filter { it.tipo == TipoLacuna.SEM_LIQUIDEZ_COM_SAQUES }.map {
            AlertaAnalise(
                Severidade.CRITICO,
                RegraAlerta.CAIXA_ZERO_COM_SAQUES,
                "Caixa zero com resgates nos últimos 12 meses: falta colchão de liquidez",
                valor = a.saques?.resgates12Meses,
                percentual = a.saques?.percentualDoPatrimonio,
            )
        }
}

/** Regras por posição: vencimentos, fantasmas, abaixo do CDI, simbólicas, dados a conferir, destaques. */
private class RegrasDePosicao(
    private val limites: LimitesAnalise,
) {
    fun avaliar(a: AnaliseDeterministica): List<AlertaAnalise> =
        vencimentos(a) + fantasmas(a) + abaixoDoCdi(a) + simbolicas(a) + aConferir(a) + destaques(a)

    private fun vencimentos(a: AnaliseDeterministica) =
        a.vencimentos.map { v ->
            AlertaAnalise(
                Severidade.URGENTE,
                RegraAlerta.VENCIMENTO_PROXIMO,
                "Vence em ${v.vencimento.monthValue.toString().padStart(2, '0')}/${v.vencimento.year}: defina o destino do dinheiro",
                ativos = listOf(v.posicao.ativo.nome),
                valor = v.posicao.saldo.valor,
            )
        }

    private fun fantasmas(a: AnaliseDeterministica) =
        a.ativos
            .filter { it.dadoAConferir && it.posicao.saldo.valor.isZero && (it.posicao.quantidade?.valor?.signum() ?: 0) > 0 }
            .map {
                AlertaAnalise(
                    Severidade.URGENTE,
                    RegraAlerta.POSICAO_FANTASMA,
                    "Posição com quantidade e saldo zero no extrato: resolver com o assessor",
                    ativos = listOf(it.posicao.ativo.nome),
                )
            }

    private fun abaixoDoCdi(a: AnaliseDeterministica) =
        a.fundosAbaixoDoCdi.map {
            AlertaAnalise(
                Severidade.ATENCAO,
                RegraAlerta.FUNDO_ABAIXO_DO_CDI,
                "Fundo abaixo do CDI no ano",
                ativos = listOf(it.posicao.ativo.nome),
                percentual = it.posicao.percentualCdiAno?.valor,
            )
        }

    private fun simbolicas(a: AnaliseDeterministica) =
        a.simbolicas.map {
            AlertaAnalise(
                Severidade.ATENCAO,
                RegraAlerta.POSICAO_SIMBOLICA,
                "Posição simbólica: aportar ou consolidar",
                ativos = listOf(it.ativo.nome),
                valor = it.saldo.valor,
            )
        }

    private fun aConferir(a: AnaliseDeterministica) =
        a.ativos.filter { it.dadoAConferir && !it.posicao.saldo.valor.isZero }.map {
            AlertaAnalise(
                Severidade.INFO,
                RegraAlerta.DADO_A_CONFERIR,
                "Rentabilidade fora do plausível no relatório: conferir com o assessor antes de qualquer decisão",
                ativos = listOf(it.posicao.ativo.nome),
                percentual = it.posicao.rentabilidadeMes?.valor,
            )
        }

    private fun destaques(a: AnaliseDeterministica) =
        a.ativos
            .filter {
                it.ritmo?.ritmo == Ritmo.ACELERANDO && (
                    it.posicao.percentualCdiAno?.valor?.let {
                            p ->
                        p > limites.destaquePercentualCdi
                    } ?: false
                )
            }
            .map {
                AlertaAnalise(
                    Severidade.DESTAQUE,
                    RegraAlerta.PERFORMANCE_EXCEPCIONAL,
                    "Ritmo acelerando e bem acima do CDI no ano",
                    ativos = listOf(it.posicao.ativo.nome),
                    percentual = it.posicao.percentualCdiAno?.valor,
                )
            }
}
