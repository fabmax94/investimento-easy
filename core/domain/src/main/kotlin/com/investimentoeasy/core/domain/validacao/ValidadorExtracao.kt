package com.investimentoeasy.core.domain.validacao

import com.investimentoeasy.core.model.AvisoExtracao
import com.investimentoeasy.core.model.ChaveAtivo
import com.investimentoeasy.core.model.ClasseAtivo
import com.investimentoeasy.core.model.Money
import com.investimentoeasy.core.model.Percent
import com.investimentoeasy.core.model.Posicao
import com.investimentoeasy.core.model.soma
import java.math.BigDecimal

public data class LimitesValidacao(
    /** R3: diferença máxima entre a soma das posições e o patrimônio informado. */
    val toleranciaSoma: Percent = Percent.of("0.5"),
    /** R6: rentabilidade mensal (em módulo) acima disso é implausível em renda fixa e caixa. */
    val rentabilidadeMensalMaximaRendaFixa: Percent = Percent.of("30"),
)

/** Resultado da R3. */
public data class ConferenciaSoma(
    val somaPosicoes: Money,
    val patrimonioInformado: Money,
    val diferenca: Money,
    val percentual: Percent,
    val dentroDaTolerancia: Boolean,
)

public enum class TipoInconsistencia {
    /** Quantidade positiva com saldo zero ("posição fantasma"). */
    POSICAO_FANTASMA,

    /** Quantidade e saldo zerados. */
    POSICAO_VAZIA,

    /** Rentabilidade mensal fora do plausível para a classe. */
    RENTABILIDADE_IMPLAUSIVEL,
}

public sealed interface Problema {
    public val gravidade: Gravidade

    /** R1: sem data de referência o snapshot não é aceito. */
    public data object DataReferenciaAusente : Problema {
        override val gravidade: Gravidade = Gravidade.IMPEDITIVO
    }

    public data object SemPosicoes : Problema {
        override val gravidade: Gravidade = Gravidade.IMPEDITIVO
    }

    /** R3: sem patrimônio informado não há como conferir a soma. */
    public data object PatrimonioAusente : Problema {
        override val gravidade: Gravidade = Gravidade.REQUER_ACEITE
    }

    /** R3: soma fora da tolerância bloqueia até o usuário aceitar a divergência. */
    public data class DivergenciaSoma(
        val conferencia: ConferenciaSoma,
    ) : Problema {
        override val gravidade: Gravidade = Gravidade.REQUER_ACEITE
    }

    /** R6: inconsistência vira alerta ("conferir com o assessor"), não conclusão. */
    public data class Inconsistencia(
        val chave: ChaveAtivo,
        val nomeAtivo: String,
        val tipo: TipoInconsistencia,
    ) : Problema {
        override val gravidade: Gravidade = Gravidade.ALERTA
    }

    /** Aviso do leitor do arquivo (linha não interpretada, subtotal divergente...). */
    public data class AvisoDeLeitura(
        val aviso: AvisoExtracao,
    ) : Problema {
        override val gravidade: Gravidade = Gravidade.ALERTA
    }
}

public enum class Gravidade {
    /** Informativo: não impede a confirmação. */
    ALERTA,

    /** Impede a confirmação até o usuário aceitar explicitamente. */
    REQUER_ACEITE,

    /** Impede a confirmação; precisa ser corrigido. */
    IMPEDITIVO,
}

public data class ResultadoValidacao(
    val problemas: List<Problema>,
    val conferenciaSoma: ConferenciaSoma?,
) {
    val gravidadeMaxima: Gravidade? get() = problemas.maxOfOrNull { it.gravidade }

    val exigeAceite: Boolean get() = problemas.any { it.gravidade == Gravidade.REQUER_ACEITE }

    val impeditivo: Boolean get() = problemas.any { it.gravidade == Gravidade.IMPEDITIVO }

    /** R6: ativos com inconsistência ficam fora dos cálculos de Ritmo. */
    val chavesForaDoRitmo: Set<ChaveAtivo> get() = problemas.filterIsInstance<Problema.Inconsistencia>().map { it.chave }.toSet()
}

/** Validação automática que todo snapshot passa antes da revisão do usuário (R1, R3, R6). */
public class ValidadorExtracao(
    private val limites: LimitesValidacao = LimitesValidacao(),
) {
    public fun validar(
        posicoes: List<Posicao>,
        patrimonioInformado: Money?,
        temDataReferencia: Boolean,
        avisosDeLeitura: List<AvisoExtracao> = emptyList(),
    ): ResultadoValidacao {
        val problemas = mutableListOf<Problema>()
        if (!temDataReferencia) problemas += Problema.DataReferenciaAusente
        if (posicoes.isEmpty()) problemas += Problema.SemPosicoes

        val conferencia = patrimonioInformado?.let { conferirSoma(posicoes, it) }
        when {
            patrimonioInformado == null -> problemas += Problema.PatrimonioAusente
            conferencia != null && !conferencia.dentroDaTolerancia -> problemas += Problema.DivergenciaSoma(conferencia)
        }

        posicoes.forEach { posicao -> inconsistenciaDe(posicao)?.let { problemas += it } }
        avisosDeLeitura.forEach { problemas += Problema.AvisoDeLeitura(it) }
        return ResultadoValidacao(problemas, conferencia)
    }

    public fun conferirSoma(
        posicoes: List<Posicao>,
        patrimonioInformado: Money,
    ): ConferenciaSoma {
        val soma = posicoes.map { it.saldo.valor }.soma()
        val diferenca = patrimonioInformado - soma
        val fracao = diferenca.abs().fracaoDe(patrimonioInformado)
        val percentual = fracao?.let(Percent::daFracao)
        val dentro =
            when {
                percentual != null -> percentual <= limites.toleranciaSoma
                else -> soma.isZero // patrimônio zero: só confere se a soma também for zero
            }
        return ConferenciaSoma(soma, patrimonioInformado, diferenca, percentual ?: Percent.ZERO, dentro)
    }

    private fun inconsistenciaDe(posicao: Posicao): Problema.Inconsistencia? {
        val quantidade = posicao.quantidade?.valor
        val saldoZero = posicao.saldo.valor.isZero
        val tipo =
            when {
                saldoZero && quantidade != null && quantidade.signum() > 0 -> TipoInconsistencia.POSICAO_FANTASMA
                saldoZero && (quantidade == null || quantidade.compareTo(BigDecimal.ZERO) == 0) -> TipoInconsistencia.POSICAO_VAZIA
                rentabilidadeImplausivel(posicao) -> TipoInconsistencia.RENTABILIDADE_IMPLAUSIVEL
                else -> null
            } ?: return null
        return Problema.Inconsistencia(posicao.ativo.chave, posicao.ativo.nome, tipo)
    }

    private fun rentabilidadeImplausivel(posicao: Posicao): Boolean {
        val mes = posicao.rentabilidadeMes?.valor ?: return false
        return posicao.ativo.classe in CLASSES_RENDA_FIXA_E_CAIXA && mes.abs() > limites.rentabilidadeMensalMaximaRendaFixa
    }

    private companion object {
        val CLASSES_RENDA_FIXA_E_CAIXA = setOf(ClasseAtivo.RF_POS, ClasseAtivo.RF_PRE, ClasseAtivo.RF_IPCA, ClasseAtivo.CAIXA)
    }
}
