package com.investimentoeasy.feature.analysis

import com.investimentoeasy.core.ai.Prioridade
import com.investimentoeasy.core.ai.StatusDiagnostico
import com.investimentoeasy.core.designsystem.Formatacao
import com.investimentoeasy.core.designsystem.componentes.Tom
import com.investimentoeasy.core.domain.analise.AlertaAnalise
import com.investimentoeasy.core.domain.analise.Cenario
import com.investimentoeasy.core.domain.analise.Ritmo
import com.investimentoeasy.core.domain.analise.RitmoAtivo
import com.investimentoeasy.core.domain.analise.Severidade
import com.investimentoeasy.core.domain.analise.TipoLacuna
import com.investimentoeasy.core.domain.complemento.BaseDoCusto
import com.investimentoeasy.core.domain.complemento.ComplementoPlanilha
import com.investimentoeasy.core.domain.complemento.resultadoDe
import com.investimentoeasy.core.model.Posicao

/** Badges sempre com ícone e rótulo, nunca só cor (skill, seção 9). */
fun rotuloRitmo(ritmo: RitmoAtivo?): String =
    when (ritmo?.ritmo) {
        Ritmo.ACELERANDO -> "▲ Acelerando"
        Ritmo.ESTAVEL -> "→ Estável"
        Ritmo.DESACELERANDO -> "▼ Desacelerando"
        Ritmo.SEM_BASE -> "— Sem base"
        null -> "Sem dado"
    }

fun detalheRitmo(ritmo: RitmoAtivo?): String =
    when {
        ritmo == null -> "sem retorno no relatório"
        ritmo.ritmo == Ritmo.SEM_BASE -> "posição nova demais"
        else -> "${Formatacao.percentual(ritmo.deltaAoAno, casas = 1, comSinal = true).removeSuffix("%")} pp a.a. vs 24M"
    }

val Cenario.rotulo: String
    get() =
        when (this) {
            Cenario.FAVORAVEL -> "Favorável"
            Cenario.ADVERSO -> "Adverso"
            Cenario.NEUTRO -> "Neutro"
            Cenario.INDEFINIDO -> "Indefinido"
        }

data class EstiloSeveridade(
    val tom: Tom,
    val rotulo: String,
)

val Severidade.estilo: EstiloSeveridade
    get() =
        when (this) {
            Severidade.URGENTE -> EstiloSeveridade(Tom.NEGATIVO, "● URGENTE")
            Severidade.CRITICO -> EstiloSeveridade(Tom.NEGATIVO, "● CRÍTICO")
            Severidade.ATENCAO -> EstiloSeveridade(Tom.ATENCAO, "▲ ATENÇÃO")
            Severidade.INFO -> EstiloSeveridade(Tom.INFORMATIVO, "ⓘ INFO")
            Severidade.DESTAQUE -> EstiloSeveridade(Tom.POSITIVO, "✓ DESTAQUE")
        }

/** Texto do alerta com os números formatados em pt-BR (o domínio não formata). */
fun textoAlerta(alerta: AlertaAnalise): String =
    buildString {
        append(alerta.titulo)
        if (alerta.ativos.isNotEmpty()) append(": ").append(alerta.ativos.joinToString(", "))
        val numeros =
            listOfNotNull(
                alerta.valor?.let { Formatacao.reais(it, centavos = false) },
                alerta.percentual?.let { Formatacao.percentual(it, casas = 1) },
            )
        if (numeros.isNotEmpty()) append(" (").append(numeros.joinToString(" · ")).append(")")
        append('.')
    }

val Prioridade.estilo: EstiloSeveridade
    get() =
        when (this) {
            Prioridade.ALTA -> EstiloSeveridade(Tom.POSITIVO, "Prioridade alta")
            Prioridade.MEDIA -> EstiloSeveridade(Tom.ATENCAO, "Prioridade média")
            Prioridade.ESPECULATIVO -> EstiloSeveridade(Tom.INFORMATIVO, "Especulativo")
        }

val StatusDiagnostico.estilo: EstiloSeveridade
    get() =
        when (this) {
            StatusDiagnostico.OK -> EstiloSeveridade(Tom.POSITIVO, "✓ OK")
            StatusDiagnostico.ATENCAO -> EstiloSeveridade(Tom.ATENCAO, "▲ Atenção")
            StatusDiagnostico.CRITICO -> EstiloSeveridade(Tom.NEGATIVO, "● Crítico")
        }

val TipoLacuna.rotulo: String
    get() =
        when (this) {
            TipoLacuna.PROTECAO_INFLACAO -> "Proteção contra inflação"
            TipoLacuna.SEM_PREFIXADO -> "Renda fixa prefixada"
            TipoLacuna.SEM_LIQUIDEZ_COM_SAQUES -> "Colchão de liquidez"
        }

/** "20000.00" (valor decidido pelo modelo) → "R$ 20.000". */
fun valorSugerido(valor: String): String =
    valor.toBigDecimalOrNull()?.let { Formatacao.reais(com.investimentoeasy.core.model.Money.of(it), centavos = false) } ?: valor

/**
 * Linha de custo e resultado sob o nome do ativo, a partir da planilha: "PM R$ 95,00 · +R$ 1.100 (+5,79%)".
 * `null` sem planilha para a posição (regra zero: nada de zero implícito).
 */
fun linhaResultado(
    complemento: ComplementoPlanilha?,
    posicao: Posicao,
): String? {
    val dados = complemento?.de(posicao.ativo.chave) ?: return null
    if (dados.quantidadeMudou) return "Qtd. mudou desde a planilha"
    val resultado = resultadoDe(posicao, dados) ?: return null
    val custo =
        when (resultado.baseDoCusto) {
            BaseDoCusto.PRECO_MEDIO -> "PM ${Formatacao.reais(dados.precoMedio)}"
            BaseDoCusto.VALOR_APLICADO -> "Aplicado ${Formatacao.reais(resultado.custo, centavos = false)}"
        }
    val ganho = Formatacao.reais(resultado.resultado, centavos = false, comSinal = true)
    return "$custo · $ganho (${Formatacao.percentual(resultado.percentual, comSinal = true)})"
}

/** Junta o apoio de cada aba com a linha de resultado, quando há planilha. */
fun apoioComResultado(
    complemento: ComplementoPlanilha?,
    posicao: Posicao,
    apoio: String,
): String = listOfNotNull(apoio, linhaResultado(complemento, posicao)).joinToString("\n")
