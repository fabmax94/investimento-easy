package com.investimentoeasy.feature.upload

import com.investimentoeasy.core.designsystem.Formatacao
import com.investimentoeasy.core.designsystem.componentes.Tom
import com.investimentoeasy.core.domain.alocacao.FatiaAlocacao
import com.investimentoeasy.core.domain.alocacao.alocacaoPorGrupo
import com.investimentoeasy.core.domain.classificacao.Pendencia
import com.investimentoeasy.core.domain.snapshot.Revisao
import com.investimentoeasy.core.domain.validacao.ConferenciaSoma
import com.investimentoeasy.core.domain.validacao.Problema
import com.investimentoeasy.core.domain.validacao.TipoInconsistencia
import com.investimentoeasy.core.model.MetodoExtracao
import com.investimentoeasy.core.model.Money
import com.investimentoeasy.core.model.Percent
import java.time.LocalDate

/** O que o usuário pode fazer a partir de um aviso. */
enum class AcaoAviso { INFORMAR_DATA, ACEITAR_DIVERGENCIA }

data class AvisoUi(
    val tom: Tom,
    val rotulo: String,
    val texto: String,
    val acao: AcaoAviso? = null,
)

/** Tudo o que a tela "Revisar extração" mostra, já calculado. */
data class RevisaoUi(
    val dataReferencia: LocalDate?,
    val patrimonioInformado: Money?,
    val conferencia: ConferenciaSoma?,
    val metodo: MetodoExtracao,
    val totalPosicoes: Int,
    val alocacao: List<FatiaAlocacao>,
    val avisos: List<AvisoUi>,
    val exigeAceite: Boolean,
    val aceitouDivergencia: Boolean,
    val podeConfirmar: Boolean,
)

/** R3, R6 e R8 como texto para o usuário: bloqueios primeiro, depois alertas e pendências. */
fun montarRevisaoUi(
    revisao: Revisao,
    aceitouDivergencia: Boolean,
): RevisaoUi {
    val rascunho = revisao.rascunho
    val validacao = revisao.validacao
    val posicoes = rascunho?.posicoes.orEmpty()
    val rentabilidadePorNome = posicoes.associate { it.ativo.nome to it.rentabilidadeMes?.valor }
    val avisos =
        validacao.problemas.mapNotNull { avisoDe(it, rentabilidadePorNome) }.sortedBy { it.tom.ordemGravidade() } +
            revisao.classificacao.filter { it.pendencias.isNotEmpty() }.map { classificado ->
                AvisoUi(
                    Tom.INFORMATIVO,
                    "ⓘ CLASSIFICAÇÃO A CONFIRMAR",
                    "${classificado.ativo.nome}: ${classificado.pendencias.joinToString("; ") { it.descricao }}.",
                )
            }
    return RevisaoUi(
        dataReferencia = rascunho?.dataReferencia,
        patrimonioInformado = revisao.extracao.resumo?.patrimonioTotalBruto,
        conferencia = validacao.conferenciaSoma,
        metodo = revisao.extracao.metodo,
        totalPosicoes = revisao.extracao.posicoes.size,
        alocacao = alocacaoPorGrupo(posicoes),
        avisos = avisos,
        exigeAceite = validacao.exigeAceite,
        aceitouDivergencia = aceitouDivergencia,
        podeConfirmar = rascunho != null && !validacao.impeditivo && (!validacao.exigeAceite || aceitouDivergencia),
    )
}

private fun avisoDe(
    problema: Problema,
    rentabilidadePorNome: Map<String, Percent?>,
): AvisoUi? =
    when (problema) {
        Problema.DataReferenciaAusente ->
            AvisoUi(
                Tom.NEGATIVO,
                "● DATA DE REFERÊNCIA AUSENTE",
                "O arquivo não traz a data da posição. Sem ela, a carteira não pode virar base.",
                AcaoAviso.INFORMAR_DATA,
            )
        Problema.SemPosicoes ->
            AvisoUi(Tom.NEGATIVO, "● NENHUMA POSIÇÃO", "Nenhuma posição foi encontrada no arquivo.")
        Problema.PatrimonioAusente ->
            AvisoUi(
                Tom.NEGATIVO,
                "● PATRIMÔNIO AUSENTE",
                "O arquivo não informa o patrimônio total, então a soma das posições não pôde ser conferida.",
                AcaoAviso.ACEITAR_DIVERGENCIA,
            )
        is Problema.DivergenciaSoma -> {
            val c = problema.conferencia
            AvisoUi(
                Tom.NEGATIVO,
                "● SOMA NÃO CONFERE",
                "As posições somam ${Formatacao.reais(c.somaPosicoes)}, mas o patrimônio informado é " +
                    "${Formatacao.reais(c.patrimonioInformado)} (diferença de ${Formatacao.percentual(c.percentual)}, " +
                    "acima da tolerância de 0,5%).",
                AcaoAviso.ACEITAR_DIVERGENCIA,
            )
        }
        is Problema.Inconsistencia -> avisoDeInconsistencia(problema, rentabilidadePorNome[problema.nomeAtivo])
        is Problema.AvisoDeLeitura -> AvisoUi(Tom.INFORMATIVO, "ⓘ LEITURA DO ARQUIVO", problema.aviso.mensagem)
    }

private fun avisoDeInconsistencia(
    problema: Problema.Inconsistencia,
    rentabilidadeMes: Percent?,
): AvisoUi {
    val foraDoRitmo = "Fica fora do cálculo de ritmo até você conferir com o assessor."
    return when (problema.tipo) {
        TipoInconsistencia.POSICAO_FANTASMA ->
            AvisoUi(Tom.ATENCAO, "▲ POSIÇÃO SEM SALDO", "${problema.nomeAtivo} aparece com quantidade, mas saldo zero. $foraDoRitmo")
        TipoInconsistencia.POSICAO_VAZIA ->
            AvisoUi(Tom.ATENCAO, "▲ POSIÇÃO VAZIA", "${problema.nomeAtivo} aparece sem quantidade e sem saldo. $foraDoRitmo")
        TipoInconsistencia.RENTABILIDADE_IMPLAUSIVEL ->
            AvisoUi(
                Tom.ATENCAO,
                "▲ DADO FORA DO PLAUSÍVEL",
                "${problema.nomeAtivo} aparece com ${Formatacao.percentual(rentabilidadeMes, comSinal = true)} no mês. $foraDoRitmo",
            )
    }
}

private fun Tom.ordemGravidade(): Int = listOf(Tom.NEGATIVO, Tom.ATENCAO, Tom.INFORMATIVO, Tom.POSITIVO, Tom.NEUTRO).indexOf(this)

private val Pendencia.descricao: String
    get() =
        when (this) {
            Pendencia.CLASSE_A_CONFIRMAR -> "classe não identificada"
            Pendencia.TIPO_AMBIGUO -> "pode ser ação (unit) ou ETF"
            Pendencia.SEGMENTO_FII_DESCONHECIDO -> "segmento do FII (tijolo, papel ou fundo de fundos) não identificado"
            Pendencia.GESTORA_DESCONHECIDA -> "gestora não identificada"
        }
