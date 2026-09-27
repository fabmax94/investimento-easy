package com.investimentoeasy.core.domain.mercado

import com.investimentoeasy.core.domain.alocacao.GrupoAlocacao
import com.investimentoeasy.core.model.Percent

/** Perfil escolhido pelo usuário; define os alvos por grupo de alocação. */
public enum class Perfil(
    public val rotulo: String,
    public val descricao: String,
) {
    CONSERVADOR("Conservador", "Prioriza segurança e liquidez; aceita pouca oscilação."),
    MODERADO("Moderado", "Equilibra renda fixa e renda variável; aceita oscilação no médio prazo."),
    ARROJADO("Arrojado", "Busca retorno no longo prazo; aceita quedas fortes no caminho."),
}

/** Faixa aceitável de um grupo: dentro dela, não há recomendação de mexer. */
public data class Faixa(
    val alvo: Percent,
    val minimo: Percent,
    val maximo: Percent,
)

/**
 * Alvos padrão por perfil (educacionais, somam 100% em cada perfil). Caixa conta como renda
 * fixa pós-fixada; "a classificar" não tem alvo.
 */
public fun alvosDe(perfil: Perfil): Map<GrupoAlocacao, Faixa> =
    when (perfil) {
        Perfil.CONSERVADOR ->
            mapOf(
                GrupoAlocacao.RENDA_FIXA_POS to faixa("45", "35", "60"),
                GrupoAlocacao.RENDA_FIXA_IPCA to faixa("25", "15", "35"),
                GrupoAlocacao.RENDA_FIXA_PRE to faixa("10", "0", "15"),
                GrupoAlocacao.MULTIMERCADO to faixa("5", "0", "10"),
                GrupoAlocacao.FIIS to faixa("7", "0", "10"),
                GrupoAlocacao.ACOES_BRASIL to faixa("5", "0", "10"),
                GrupoAlocacao.EXTERIOR to faixa("3", "0", "10"),
            )
        Perfil.MODERADO ->
            mapOf(
                GrupoAlocacao.RENDA_FIXA_POS to faixa("25", "15", "40"),
                GrupoAlocacao.RENDA_FIXA_IPCA to faixa("20", "10", "30"),
                GrupoAlocacao.RENDA_FIXA_PRE to faixa("5", "0", "10"),
                GrupoAlocacao.MULTIMERCADO to faixa("10", "0", "15"),
                GrupoAlocacao.FIIS to faixa("15", "5", "20"),
                GrupoAlocacao.ACOES_BRASIL to faixa("15", "5", "25"),
                GrupoAlocacao.EXTERIOR to faixa("10", "5", "20"),
            )
        Perfil.ARROJADO ->
            mapOf(
                GrupoAlocacao.RENDA_FIXA_POS to faixa("10", "5", "20"),
                GrupoAlocacao.RENDA_FIXA_IPCA to faixa("15", "5", "25"),
                GrupoAlocacao.RENDA_FIXA_PRE to faixa("5", "0", "10"),
                GrupoAlocacao.MULTIMERCADO to faixa("10", "0", "15"),
                GrupoAlocacao.FIIS to faixa("15", "5", "25"),
                GrupoAlocacao.ACOES_BRASIL to faixa("30", "15", "40"),
                GrupoAlocacao.EXTERIOR to faixa("15", "5", "30"),
            )
    }

/** Alvo, mínimo e máximo em pontos percentuais. */
private fun faixa(
    alvo: String,
    minimo: String,
    maximo: String,
) = Faixa(Percent.of(alvo), Percent.of(minimo), Percent.of(maximo))

/** Onde o perfil escolhido fica guardado (preferência simples, não é dado financeiro). */
public interface RepositorioDePerfil {
    public fun ler(): Perfil?

    public fun gravar(perfil: Perfil)
}
