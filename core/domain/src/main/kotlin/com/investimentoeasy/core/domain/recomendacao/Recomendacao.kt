package com.investimentoeasy.core.domain.recomendacao

import com.investimentoeasy.core.domain.alocacao.GrupoAlocacao
import com.investimentoeasy.core.domain.analise.RegraAlerta
import com.investimentoeasy.core.domain.mercado.Perfil
import com.investimentoeasy.core.model.Money
import java.time.Instant

/** Uma ação concreta para os próximos 30 dias. */
public data class Acao(
    val titulo: String,
    val detalhe: String,
)

/** Venda sempre com destino: dinheiro parado depois da venda é a perda mais silenciosa. */
public data class Realocacao(
    val vender: String,
    val motivo: String,
    val destino: String,
    val valor: Money,
)

public enum class Prioridade { ALTA, MEDIA, ESPECULATIVO }

/** Por que o app sugere comprar: toda sugestão aponta para uma lacuna ou para o perfil. */
public enum class MotivoSugestao { PROTECAO_INFLACAO, SEM_PREFIXADO, COLCHAO_DE_LIQUIDEZ, ABAIXO_DO_PERFIL }

public data class NovoAtivo(
    val sugestao: String,
    val grupo: GrupoAlocacao,
    val motivo: MotivoSugestao,
    val prioridade: Prioridade,
    val valor: Money,
    val justificativa: String,
)

public enum class StatusDiagnostico { OK, ATENCAO, CRITICO }

public data class Diagnostico(
    val status: StatusDiagnostico,
    val texto: String,
)

/**
 * Análise completa gerada no aparelho (substitui a Camada 3 com IA). Todo número dos textos vem
 * do relatório, de um cálculo do app ou de uma fonte de mercado com data.
 */
public data class Recomendacao(
    val perfil: Perfil,
    /** Quando os dados de mercado usados foram baixados; `null` se a análise saiu sem mercado. */
    val mercadoObtidoEm: Instant?,
    val veredicto: String,
    val acoes30Dias: List<Acao>,
    val realocacoes: List<Realocacao>,
    val novosAtivos: List<NovoAtivo>,
    val mercado: List<String>,
    val alocacao: List<Diagnostico>,
    val fundos: List<String>,
    val fiis: List<String>,
    val acoesEtfs: List<String>,
    val comentariosAlertas: Map<RegraAlerta, String>,
)
