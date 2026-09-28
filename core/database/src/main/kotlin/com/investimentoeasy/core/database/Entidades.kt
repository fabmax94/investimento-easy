package com.investimentoeasy.core.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Snapshot confirmado. Valores monetários e percentuais são gravados como texto
 * (`BigDecimal.toPlainString`) para não perder precisão.
 */
@Entity(tableName = "snapshot")
internal data class SnapshotEntity(
    @PrimaryKey val id: String,
    val dataReferencia: String,
    val patrimonio: String?,
    val patrimonioOrigem: String?,
    val metodo: String,
    val versao: Int,
    val confirmadoEm: Long?,
    val divergenciaAceita: Boolean,
    /** [ContextoJson] serializado; nulo nos snapshots gravados na versão 1. */
    val contexto: String? = null,
)

@Entity(
    tableName = "posicao",
    primaryKeys = ["snapshotId", "ordem"],
    foreignKeys = [ForeignKey(entity = SnapshotEntity::class, parentColumns = ["id"], childColumns = ["snapshotId"])],
    indices = [Index("chave")],
)
internal data class PosicaoEntity(
    val snapshotId: String,
    val ordem: Int,
    /** [com.investimentoeasy.core.model.ChaveAtivo.id]: permite comparar a mesma posição entre snapshots (R4). */
    val chave: String,
    val nome: String,
    val tipo: String,
    val classe: String,
    val gestora: String?,
    val emissor: String?,
    val indexador: String?,
    val taxa: String?,
    val vencimento: String?,
    val saldo: String,
    val saldoOrigem: String,
    val quantidade: String?,
    val quantidadeOrigem: String?,
    val rentabilidadeMes: String?,
    val rentabilidadeMesOrigem: String?,
    val rentabilidadeAno: String?,
    val rentabilidadeAnoOrigem: String?,
    val rentabilidade24Meses: String?,
    val rentabilidade24MesesOrigem: String?,
    val percentualCdiMes: String? = null,
    val percentualCdiMesOrigem: String? = null,
    val percentualCdiAno: String? = null,
    val percentualCdiAnoOrigem: String? = null,
    val percentualCdi24Meses: String? = null,
    val percentualCdi24MesesOrigem: String? = null,
)

/** Linha única que aponta qual snapshot é a base atual da carteira. */
@Entity(tableName = "base")
internal data class BaseEntity(
    @PrimaryKey val unica: Int = UNICA,
    val snapshotId: String,
) {
    companion object {
        const val UNICA = 1
    }
}

/** Último panorama de mercado baixado (uma linha só), para a análise funcionar sem internet. */
@Entity(tableName = "mercado")
internal data class MercadoEntity(
    @PrimaryKey val unica: Int = UNICA,
    val obtidoEm: Long,
    val conteudo: String,
) {
    companion object {
        const val UNICA = 1
    }
}

/** Dados da planilha "Posição Detalhada" guardados ao lado de um snapshot (R16: o snapshot não muda). */
@Entity(
    tableName = "complemento",
    foreignKeys = [ForeignKey(entity = SnapshotEntity::class, parentColumns = ["id"], childColumns = ["snapshotId"])],
    indices = [Index("snapshotId")],
)
internal data class ComplementoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val snapshotId: String,
    val dataPlanilha: String?,
    val recebidoEm: Long,
    val conteudo: String,
)
