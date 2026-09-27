package com.investimentoeasy.core.database

import com.investimentoeasy.core.model.Ativo
import com.investimentoeasy.core.model.ChaveAtivo
import com.investimentoeasy.core.model.ClasseAtivo
import com.investimentoeasy.core.model.Indexador
import com.investimentoeasy.core.model.MetodoExtracao
import com.investimentoeasy.core.model.Money
import com.investimentoeasy.core.model.Origem
import com.investimentoeasy.core.model.Percent
import com.investimentoeasy.core.model.Posicao
import com.investimentoeasy.core.model.Snapshot
import com.investimentoeasy.core.model.SnapshotId
import com.investimentoeasy.core.model.Sourced
import com.investimentoeasy.core.model.StatusSnapshot
import com.investimentoeasy.core.model.TipoAtivo
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

internal fun Snapshot.paraEntidade(): SnapshotEntity {
    check(status == StatusSnapshot.CONFIRMADO) { "Só snapshots confirmados são persistidos" }
    return SnapshotEntity(
        id = id.valor,
        dataReferencia = dataReferencia.toString(),
        patrimonio = patrimonioInformado?.valor?.valor?.toPlainString(),
        patrimonioOrigem = patrimonioInformado?.origem?.name,
        metodo = metodo.name,
        versao = versao,
        confirmadoEm = confirmadoEm?.toEpochMilli(),
        divergenciaAceita = divergenciaAceita,
    )
}

internal fun Snapshot.posicoesParaEntidades(): List<PosicaoEntity> =
    posicoes.mapIndexed { ordem, p ->
        PosicaoEntity(
            snapshotId = id.valor,
            ordem = ordem,
            chave = p.ativo.chave.id,
            nome = p.ativo.nome,
            tipo = p.ativo.tipo.name,
            classe = p.ativo.classe.name,
            gestora = p.ativo.gestora,
            emissor = p.ativo.emissor,
            indexador = p.ativo.indexador?.name,
            taxa = p.ativo.taxa?.pontos?.toPlainString(),
            vencimento = p.ativo.vencimento?.toString(),
            saldo = p.saldo.valor.valor.toPlainString(),
            saldoOrigem = p.saldo.origem.name,
            quantidade = p.quantidade?.valor?.toPlainString(),
            quantidadeOrigem = p.quantidade?.origem?.name,
            rentabilidadeMes = p.rentabilidadeMes?.valor?.pontos?.toPlainString(),
            rentabilidadeMesOrigem = p.rentabilidadeMes?.origem?.name,
            rentabilidadeAno = p.rentabilidadeAno?.valor?.pontos?.toPlainString(),
            rentabilidadeAnoOrigem = p.rentabilidadeAno?.origem?.name,
            rentabilidade24Meses = p.rentabilidade24Meses?.valor?.pontos?.toPlainString(),
            rentabilidade24MesesOrigem = p.rentabilidade24Meses?.origem?.name,
        )
    }

internal fun SnapshotEntity.paraModelo(posicoes: List<PosicaoEntity>): Snapshot =
    Snapshot(
        id = SnapshotId(id),
        dataReferencia = LocalDate.parse(dataReferencia),
        patrimonioInformado = sourced(patrimonio, patrimonioOrigem, Money::of),
        posicoes = posicoes.sortedBy { it.ordem }.map { it.paraModelo() },
        metodo = MetodoExtracao.valueOf(metodo),
        status = StatusSnapshot.CONFIRMADO,
        versao = versao,
        confirmadoEm = confirmadoEm?.let(Instant::ofEpochMilli),
        divergenciaAceita = divergenciaAceita,
    )

private fun PosicaoEntity.paraModelo(): Posicao =
    Posicao(
        ativo =
            Ativo(
                chave = ChaveAtivo.deId(chave),
                nome = nome,
                tipo = TipoAtivo.valueOf(tipo),
                classe = ClasseAtivo.valueOf(classe),
                gestora = gestora,
                emissor = emissor,
                indexador = indexador?.let(Indexador::valueOf),
                taxa = taxa?.let(Percent::of),
                vencimento = vencimento?.let(YearMonth::parse),
            ),
        saldo = Sourced(Money.of(saldo), Origem.valueOf(saldoOrigem)),
        quantidade = sourced(quantidade, quantidadeOrigem, ::BigDecimal),
        rentabilidadeMes = sourced(rentabilidadeMes, rentabilidadeMesOrigem, Percent::of),
        rentabilidadeAno = sourced(rentabilidadeAno, rentabilidadeAnoOrigem, Percent::of),
        rentabilidade24Meses = sourced(rentabilidade24Meses, rentabilidade24MesesOrigem, Percent::of),
    )

private fun <T> sourced(
    valor: String?,
    origem: String?,
    converter: (String) -> T,
): Sourced<T>? = if (valor == null || origem == null) null else Sourced(converter(valor), Origem.valueOf(origem))
