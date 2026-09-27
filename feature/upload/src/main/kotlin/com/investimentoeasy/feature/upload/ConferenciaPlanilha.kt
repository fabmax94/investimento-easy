package com.investimentoeasy.feature.upload

import com.investimentoeasy.core.designsystem.Formatacao
import com.investimentoeasy.core.domain.classificacao.ClassificadorAtivos
import com.investimentoeasy.core.domain.complemento.Casamento
import com.investimentoeasy.core.domain.complemento.DadosDaPlanilha
import com.investimentoeasy.core.model.Snapshot
import java.time.LocalDate

data class LinhaConferida(
    val nome: String,
    /** Nome como a planilha escreve, quando é outro (o usuário confere o casamento por nome). */
    val nomeNaPlanilha: String?,
    val detalhe: String?,
    val quantidadeMudou: Boolean,
)

/** Tela "Conferir planilha": o que casou com a base e o que ficou de fora, antes de guardar. */
data class ConferenciaPlanilhaUi(
    val dataPlanilha: LocalDate?,
    val dataBase: LocalDate,
    val casadas: List<LinhaConferida>,
    val soNaPlanilha: List<String>,
    val soNaBase: List<String>,
    val proventos: Int,
) {
    val planilhaMaisAntiga: Boolean get() = dataPlanilha != null && dataPlanilha.isBefore(dataBase)
    val comQuantidadeDiferente: Int get() = casadas.count { it.quantidadeMudou }
    val podeGuardar: Boolean get() = casadas.isNotEmpty()
}

fun montarConferencia(
    base: Snapshot,
    casamento: Casamento,
): ConferenciaPlanilhaUi {
    val nomes = base.posicoes.associate { it.ativo.chave to it.ativo.nome }
    return ConferenciaPlanilhaUi(
        dataPlanilha = casamento.complemento.dataPlanilha,
        dataBase = base.dataReferencia,
        casadas =
            casamento.complemento.dados.map { dados ->
                val nome = nomes[dados.chave] ?: dados.nomeNaPlanilha
                LinhaConferida(
                    nome = nome,
                    nomeNaPlanilha = dados.nomeNaPlanilha.takeIf { !mesmoNome(it, nome) },
                    detalhe = detalhe(dados),
                    quantidadeMudou = dados.quantidadeMudou,
                )
            },
        soNaPlanilha = casamento.soNaPlanilha.map { it.nome },
        soNaBase = casamento.soNaBase.map { it.ativo.nome },
        proventos = casamento.complemento.proventos.size,
    )
}

private fun detalhe(dados: DadosDaPlanilha): String? =
    listOfNotNull(
        dados.precoMedio?.let { "Preço médio ${Formatacao.reais(it)}" },
        dados.valorAplicado?.let { "Aplicado ${Formatacao.reais(it)}" },
        dados.taxa,
        dados.vencimento?.let { "vence ${Formatacao.data(it)}" },
    ).joinToString(" · ").ifEmpty { null }

/** O PDF acrescenta a taxa ao nome do crédito; isso não é "outro nome". */
private fun mesmoNome(
    planilha: String,
    base: String,
): Boolean {
    val a = ClassificadorAtivos.normalizar(planilha)
    val b = ClassificadorAtivos.normalizar(base)
    return a == b || b.startsWith("$a -")
}
