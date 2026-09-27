package com.investimentoeasy.core.database

import com.investimentoeasy.core.domain.complemento.ComplementoPlanilha
import com.investimentoeasy.core.domain.complemento.DadosDaPlanilha
import com.investimentoeasy.core.model.ChaveAtivo
import com.investimentoeasy.core.model.Money
import com.investimentoeasy.core.model.Percent
import com.investimentoeasy.core.model.ProventoPrevisto
import com.investimentoeasy.core.model.SnapshotId
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/** Conteúdo da coluna `complemento.conteudo`. Números como texto, para não perder precisão. */
@Serializable
internal data class ComplementoJson(
    val dados: List<DadoJson> = emptyList(),
    val proventos: List<ProventoJson> = emptyList(),
) {
    fun paraModelo(
        snapshotId: String,
        dataPlanilha: String?,
        recebidoEm: Long,
    ): ComplementoPlanilha =
        ComplementoPlanilha(
            snapshotId = SnapshotId(snapshotId),
            dataPlanilha = dataPlanilha?.let(LocalDate::parse),
            recebidoEm = Instant.ofEpochMilli(recebidoEm),
            dados = dados.map { it.paraModelo() },
            proventos = proventos.map { it.paraModelo() },
        )

    fun texto(): String = JSON.encodeToString(serializer(), this)

    companion object {
        private val JSON = Json { ignoreUnknownKeys = true }

        fun de(texto: String): ComplementoJson = JSON.decodeFromString(serializer(), texto)

        fun de(c: ComplementoPlanilha): ComplementoJson =
            ComplementoJson(
                dados =
                    c.dados.map {
                        DadoJson(
                            chave = it.chave.id,
                            nome = it.nomeNaPlanilha,
                            saldo = it.saldoNaPlanilha.valor.toPlainString(),
                            quantidade = it.quantidadeNaPlanilha?.toPlainString(),
                            valorAplicado = it.valorAplicado?.valor?.toPlainString(),
                            precoMedio = it.precoMedio?.valor?.toPlainString(),
                            rentabilidadeDesdeInicio = it.rentabilidadeDesdeInicio?.pontos?.toPlainString(),
                            valorLiquido = it.valorLiquido?.valor?.toPlainString(),
                            ir = it.ir?.valor?.toPlainString(),
                            iof = it.iof?.valor?.toPlainString(),
                            taxa = it.taxa,
                            dataAplicacao = it.dataAplicacao?.toString(),
                            vencimento = it.vencimento?.toString(),
                            quantidadeMudou = it.quantidadeMudou,
                        )
                    },
                proventos =
                    c.proventos.map {
                        ProventoJson(
                            it.ativo,
                            it.evento,
                            it.quantidade?.toPlainString(),
                            it.valorLiquido.valor.toPlainString(),
                            it.dataPagamento?.toString(),
                        )
                    },
            )
    }
}

@Serializable
internal data class DadoJson(
    val chave: String,
    val nome: String,
    val saldo: String,
    val quantidade: String? = null,
    val valorAplicado: String? = null,
    val precoMedio: String? = null,
    val rentabilidadeDesdeInicio: String? = null,
    val valorLiquido: String? = null,
    val ir: String? = null,
    val iof: String? = null,
    val taxa: String? = null,
    val dataAplicacao: String? = null,
    val vencimento: String? = null,
    val quantidadeMudou: Boolean = false,
) {
    fun paraModelo() =
        DadosDaPlanilha(
            chave = ChaveAtivo.deId(chave),
            nomeNaPlanilha = nome,
            saldoNaPlanilha = Money.of(saldo),
            quantidadeNaPlanilha = quantidade?.let(::BigDecimal),
            valorAplicado = valorAplicado?.let(Money::of),
            precoMedio = precoMedio?.let(Money::of),
            rentabilidadeDesdeInicio = rentabilidadeDesdeInicio?.let(Percent::of),
            valorLiquido = valorLiquido?.let(Money::of),
            ir = ir?.let(Money::of),
            iof = iof?.let(Money::of),
            taxa = taxa,
            dataAplicacao = dataAplicacao?.let(LocalDate::parse),
            vencimento = vencimento?.let(LocalDate::parse),
            quantidadeMudou = quantidadeMudou,
        )
}

@Serializable
internal data class ProventoJson(
    val ativo: String,
    val evento: String,
    val quantidade: String? = null,
    val valorLiquido: String,
    val dataPagamento: String? = null,
) {
    fun paraModelo() =
        ProventoPrevisto(
            ativo,
            evento,
            quantidade?.let(::BigDecimal),
            Money.of(valorLiquido),
            dataPagamento?.let(LocalDate::parse),
        )
}
