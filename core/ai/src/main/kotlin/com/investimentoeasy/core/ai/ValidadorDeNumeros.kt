package com.investimentoeasy.core.ai

import com.investimentoeasy.core.domain.analise.TipoLacuna
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import java.math.BigDecimal
import java.math.RoundingMode

public sealed interface Violacao {
    /** Número no texto que não existe nos dados enviados (regra zero). */
    public data class NumeroSemOrigem(val numero: String, val trecho: String) : Violacao

    public data class ValorInvalido(val campo: String, val valor: String) : Violacao

    /** Sugestão de compra para uma lacuna que a Camada 1 não calculou. */
    public data class LacunaInexistente(val lacuna: TipoLacuna) : Violacao

    /** Venda de um ativo que não está na carteira, ou sem destino. */
    public data class VendaInvalida(val ativo: String) : Violacao
}

/**
 * Barreira contra número inventado: todo número citado nos textos precisa existir na entrada
 * (com o arredondamento que o texto usa). Os valores das sugestões são decisões, não dados:
 * ficam nos campos `valor`, que precisam ser positivos e caber na carteira ou no saldo vendido.
 */
public class ValidadorDeNumeros(
    private val entrada: EntradaAnalise,
) {
    private val permitidos: List<BigDecimal> = (numerosDe(Json.encodeToJsonElement(entrada)) + CONSTANTES).distinct()
    private val total = BigDecimal(entrada.patrimonioInformado ?: entrada.somaDasPosicoes)
    private val saldos = entrada.posicoes.associate { it.nome to BigDecimal(it.saldo) }

    public fun validar(saida: SaidaAnalise): List<Violacao> =
        textos(saida).flatMap(::numerosSemOrigem) + valores(saida) + lacunas(saida) + vendas(saida)

    private fun numerosSemOrigem(texto: String): List<Violacao> =
        NUMERO.findAll(texto).mapNotNull { m ->
            val bruto = m.groupValues[1]
            val multiplicador = if (m.groupValues[2].isNotEmpty()) MIL else BigDecimal.ONE
            val valor = numeroBr(bruto) ?: return@mapNotNull null
            val casas = bruto.substringAfter(',', "").length
            if (existe(valor, casas, multiplicador)) {
                null
            } else {
                Violacao.NumeroSemOrigem(m.value.trim(), texto.trecho(m.range))
            }
        }.toList()

    private fun existe(
        valor: BigDecimal,
        casas: Int,
        multiplicador: BigDecimal,
    ): Boolean {
        val alvo = valor.abs()
        return permitidos.any { p ->
            val base = p.abs().divide(multiplicador)
            ARREDONDAMENTOS.any { modo -> base.setScale(casas, modo).compareTo(alvo) == 0 }
        }
    }

    private fun valores(saida: SaidaAnalise): List<Violacao> {
        fun invalido(
            campo: String,
            valor: String,
            teto: BigDecimal,
        ): Violacao? {
            val numero = valor.toBigDecimalOrNull()
            return if (numero == null || numero.signum() <= 0 || numero > teto) Violacao.ValorInvalido(campo, valor) else null
        }
        return saida.oQueFazer.novosAtivos.mapNotNull { invalido("novosAtivos.valor", it.valor, total) } +
            saida.oQueFazer.realocacoes.mapNotNull { r -> invalido("realocacoes.valor", r.valor, saldos[r.vender] ?: total) }
    }

    private fun lacunas(saida: SaidaAnalise): List<Violacao> {
        val calculadas = entrada.lacunas.map { it.id }.toSet()
        return saida.oQueFazer.novosAtivos.filter { it.lacuna.name !in calculadas }.map { Violacao.LacunaInexistente(it.lacuna) }
    }

    private fun vendas(saida: SaidaAnalise): List<Violacao> =
        saida.oQueFazer.realocacoes.filter { it.vender !in saldos || it.destino.isBlank() }.map { Violacao.VendaInvalida(it.vender) }

    private fun textos(s: SaidaAnalise): List<String> =
        listOf(s.veredicto, s.mercado.analise) +
            s.oQueFazer.acoes30Dias.flatMap { listOf(it.titulo, it.detalhe) } +
            s.oQueFazer.realocacoes.flatMap { listOf(it.motivo, it.destino) } +
            s.oQueFazer.novosAtivos.flatMap { listOf(it.sugestao, it.justificativa) } +
            s.alocacao.diagnostico.map { it.texto } + s.fundos.notas + s.fiis.notas + s.acoesEtfs.notas +
            s.alertas.comentarios.map { it.texto }

    public companion object {
        /**
         * Número isolado (não colado em letras, como em HGLG11 ou 24M), no formato brasileiro,
         * com "mil" opcional depois.
         */
        private val NUMERO = Regex("""(?<![\p{L}\d.,])(-?\d{1,3}(?:\.\d{3})+(?:,\d+)?|-?\d+(?:,\d+)?)(?![\p{L}\d])(\s*mil\b)?""")
        private val MIL = BigDecimal(1000)
        private val ARREDONDAMENTOS = listOf(RoundingMode.HALF_UP, RoundingMode.HALF_EVEN, RoundingMode.DOWN)
        private const val CONTEXTO_TRECHO = 30

        /**
         * Sempre permitidos: contagens e prazos pequenos (dias, meses), anos, e os limites das
         * regras de alerta que o texto pode citar ao explicar um alerta.
         */
        private val CONSTANTES: List<BigDecimal> =
            (0..31).map(::BigDecimal) + (1990..2100).map(::BigDecimal) +
                listOf("40", "45", "50", "60", "90", "100", "150", "180", "250000", "500", "0.5").map(::BigDecimal)

        private fun numerosDe(elemento: JsonElement): List<BigDecimal> =
            when (elemento) {
                is JsonObject -> elemento.values.flatMap(::numerosDe)
                is JsonArray -> elemento.flatMap(::numerosDe)
                is JsonPrimitive -> listOfNotNull(elemento.content.toBigDecimalOrNull())
            }

        private fun numeroBr(texto: String): BigDecimal? = texto.replace(".", "").replace(",", ".").toBigDecimalOrNull()

        private fun String.trecho(faixa: IntRange): String =
            substring((faixa.first - CONTEXTO_TRECHO).coerceAtLeast(0), (faixa.last + CONTEXTO_TRECHO).coerceAtMost(length - 1) + 1)
    }
}
