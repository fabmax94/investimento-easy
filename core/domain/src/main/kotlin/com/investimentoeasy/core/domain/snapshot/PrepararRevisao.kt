package com.investimentoeasy.core.domain.snapshot

import com.investimentoeasy.core.domain.classificacao.AtivoClassificado
import com.investimentoeasy.core.domain.classificacao.ClassificadorAtivos
import com.investimentoeasy.core.domain.validacao.ResultadoValidacao
import com.investimentoeasy.core.domain.validacao.ValidadorExtracao
import com.investimentoeasy.core.model.ContextoRelatorio
import com.investimentoeasy.core.model.ExtracaoCarteira
import com.investimentoeasy.core.model.MetodoExtracao
import com.investimentoeasy.core.model.Origem
import com.investimentoeasy.core.model.Posicao
import com.investimentoeasy.core.model.Snapshot
import com.investimentoeasy.core.model.SnapshotId
import com.investimentoeasy.core.model.Sourced
import java.time.LocalDate
import java.util.UUID

public fun interface GeradorId {
    public fun novo(): SnapshotId

    public companion object {
        public val Uuid: GeradorId = GeradorId { SnapshotId(UUID.randomUUID().toString()) }
    }
}

/**
 * O que a tela "Revisar extração" mostra: o rascunho (quando há data de referência),
 * a validação automática e o que a classificação deixou para o usuário confirmar.
 */
public data class Revisao(
    val rascunho: Snapshot?,
    val validacao: ResultadoValidacao,
    val classificacao: List<AtivoClassificado>,
    val extracao: ExtracaoCarteira,
)

/** Transforma uma extração em rascunho de snapshot: classifica (R4/R5) e valida (R1/R3/R6). */
public class PrepararRevisao(
    private val classificador: ClassificadorAtivos = ClassificadorAtivos(),
    private val validador: ValidadorExtracao = ValidadorExtracao(),
    private val geradorId: GeradorId = GeradorId.Uuid,
) {
    /**
     * @param dataInformadaPeloUsuario R1: usada quando o arquivo não traz a data de referência.
     */
    public operator fun invoke(
        extracao: ExtracaoCarteira,
        dataInformadaPeloUsuario: LocalDate? = null,
    ): Revisao {
        val origem = if (extracao.metodo == MetodoExtracao.IA) Origem.IA else Origem.RELATORIO
        val classificados = extracao.posicoes.map(classificador::classificar)
        val posicoes =
            extracao.posicoes.zip(classificados) { p, c ->
                Posicao(
                    ativo = c.ativo,
                    saldo = Sourced(p.saldo, origem),
                    quantidade = p.quantidade?.let { Sourced(it, origem) },
                    rentabilidadeMes = p.rentabilidadeMes?.let { Sourced(it, origem) },
                    rentabilidadeAno = p.rentabilidadeAno?.let { Sourced(it, origem) },
                    rentabilidade24Meses = p.rentabilidade24Meses?.let { Sourced(it, origem) },
                    percentualCdiMes = p.percentualCdiMes?.let { Sourced(it, origem) },
                    percentualCdiAno = p.percentualCdiAno?.let { Sourced(it, origem) },
                    percentualCdi24Meses = p.percentualCdi24Meses?.let { Sourced(it, origem) },
                )
            }
        val data = extracao.dataReferencia ?: dataInformadaPeloUsuario
        val patrimonio = extracao.resumo?.patrimonioTotalBruto
        val validacao = validador.validar(posicoes, patrimonio, temDataReferencia = data != null, extracao.avisos)
        val rascunho =
            data?.let {
                Snapshot(
                    id = geradorId.novo(),
                    dataReferencia = it,
                    patrimonioInformado = patrimonio?.let { valor -> Sourced(valor, origem) },
                    posicoes = posicoes,
                    metodo = extracao.metodo,
                    contexto =
                        ContextoRelatorio(
                            resumo = extracao.resumo,
                            referencias = extracao.referencias,
                            rentabilidadeMensal = extracao.rentabilidadeMensal,
                            evolucaoMensal = extracao.evolucaoMensal,
                        ),
                )
            }
        return Revisao(rascunho, validacao, classificados, extracao)
    }
}
