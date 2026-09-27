package com.investimentoeasy.core.domain.recomendacao

import com.investimentoeasy.core.domain.analise.AnaliseDeterministica
import com.investimentoeasy.core.domain.complemento.ComplementoPlanilha
import com.investimentoeasy.core.domain.mercado.Perfil
import com.investimentoeasy.core.model.PanoramaMercado
import com.investimentoeasy.core.model.Snapshot
import java.time.LocalDate

/** O que o motor lê: a base e sua análise, o perfil, o mercado (se baixado) e a planilha (se enviada). */
public data class EntradaDoMotor(
    val snapshot: Snapshot,
    val analise: AnaliseDeterministica,
    val perfil: Perfil,
    val panorama: PanoramaMercado?,
    val complemento: ComplementoPlanilha?,
    val hoje: LocalDate,
)

/**
 * Gera a análise inteira no aparelho, por regras: nada é inventado e nada sai do celular (além
 * dos tickers pedidos ao Yahoo). Sem mercado, faz o que dá com o relatório e o perfil.
 */
public class MotorDeRecomendacao {
    public operator fun invoke(entrada: EntradaDoMotor): Recomendacao {
        val ctx = with(entrada) { Contexto(snapshot, analise, perfil, panorama, complemento, hoje) }
        val perfil = entrada.perfil
        val panorama = entrada.panorama
        val diagnosticos = diagnosticos(ctx)
        val realocacoes = realocacoes(ctx)
        val novos = novosAtivos(ctx)
        return Recomendacao(
            perfil = perfil,
            mercadoObtidoEm = panorama?.obtidoEm,
            veredicto = veredicto(ctx, diagnosticos),
            acoes30Dias = plano30Dias(ctx, realocacoes, novos),
            realocacoes = realocacoes,
            novosAtivos = novos,
            mercado = textosDeMercado(ctx),
            alocacao = diagnosticos,
            fundos = notasDeFundos(ctx),
            fiis = notasDeFiis(ctx),
            acoesEtfs = notasDeAcoesEtfs(ctx),
            comentariosAlertas = comentariosDosAlertas(ctx),
        )
    }
}
