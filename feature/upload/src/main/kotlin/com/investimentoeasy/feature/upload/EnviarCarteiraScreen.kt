package com.investimentoeasy.feature.upload

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.investimentoeasy.core.designsystem.Castanha
import com.investimentoeasy.core.designsystem.Formatacao
import com.investimentoeasy.core.designsystem.Icones
import com.investimentoeasy.core.designsystem.componentes.BotaoContorno
import com.investimentoeasy.core.designsystem.componentes.BotaoPrimario
import com.investimentoeasy.core.designsystem.componentes.BotaoTexto
import com.investimentoeasy.core.designsystem.componentes.CabecalhoTela
import com.investimentoeasy.core.designsystem.componentes.CartaoAviso
import com.investimentoeasy.core.designsystem.componentes.CartaoContorno
import com.investimentoeasy.core.designsystem.componentes.Etiqueta
import com.investimentoeasy.core.designsystem.componentes.Raios
import com.investimentoeasy.core.designsystem.componentes.Tom
import com.investimentoeasy.core.importacao.MotivoFalha
import java.util.Locale

/** Tela 2 do protótipo: escolha do arquivo e etapas da leitura (R1, R2, R3, R7). */
@Composable
fun EnviarCarteiraScreen(
    estado: EstadoImportacao,
    aoEscolherArquivo: () -> Unit,
    aoRevisar: () -> Unit,
    aoVoltar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(Castanha.cores.surfaceDefault)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        BotaoTexto("‹ Carteira", onClick = aoVoltar, alinhadoAoTexto = true)
        CabecalhoTela(
            "Enviar carteira",
            subtitulo = "Exporte a posição no site da corretora e envie aqui. O app confere os números antes de qualquer mudança.",
        )
        AreaDeArquivo(lendo = estado.leitura == Leitura.Lendo, aoEscolherArquivo = aoEscolherArquivo)

        when (val leitura = estado.leitura) {
            is Leitura.Lida -> estado.revisaoUi?.let { ResumoDaLeitura(leitura, it) }
            is Leitura.Falhou -> CartaoAviso(Tom.NEGATIVO, "● NÃO FOI POSSÍVEL LER", mensagemDe(leitura.motivo))
            Leitura.Aguardando, Leitura.Lendo -> Unit
        }

        CartaoAviso(
            Tom.INFORMATIVO,
            "ⓘ PRIVACIDADE",
            "Número da conta e nome do assessor não são lidos do arquivo. Nada é enviado para fora do aparelho nesta etapa.",
        )
        Spacer(Modifier.weight(1f))
        BotaoPrimario("Revisar extração", onClick = aoRevisar, habilitado = estado.leitura is Leitura.Lida)
    }
}

@Composable
private fun AreaDeArquivo(
    lendo: Boolean,
    aoEscolherArquivo: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(2.dp, Castanha.cores.borderMedium, Raios.grande)
                .padding(horizontal = 20.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icones.arquivo, contentDescription = null, tint = Castanha.cores.iconsMedium, modifier = Modifier.size(40.dp))
        Text("Escolha o relatório", style = Castanha.tipografia.secao, color = Castanha.cores.textIntense)
        Text(
            "PDF XPerformance da XP · até 20 MB",
            style = Castanha.tipografia.legenda,
            color = Castanha.cores.textMedium,
            textAlign = TextAlign.Center,
        )
        if (lendo) {
            CircularProgressIndicator(
                color = Castanha.cores.accentSolidMedium,
                modifier = Modifier.size(32.dp).semantics { contentDescription = "Lendo o arquivo" },
            )
        } else {
            BotaoContorno("Escolher arquivo", onClick = aoEscolherArquivo)
        }
    }
}

@Composable
private fun ResumoDaLeitura(
    leitura: Leitura.Lida,
    revisao: RevisaoUi,
) {
    val etapas = etapasDaLeitura(leitura, revisao)
    CartaoContorno {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(leitura.nomeArquivo, style = Castanha.tipografia.corpoForte, color = Castanha.cores.textIntense)
                Text(tamanho(leitura.tamanhoBytes), style = Castanha.tipografia.legenda, color = Castanha.cores.textMedium)
            }
            if (etapas.all { it.ok }) Etiqueta("Pronto", Tom.POSITIVO) else Etiqueta("Revisar", Tom.ATENCAO)
        }
        HorizontalDivider(color = Castanha.cores.borderSemiSoft, modifier = Modifier.padding(vertical = 6.dp))
        etapas.forEach { EtapaLinha(it) }
    }
}

data class Etapa(
    val titulo: String,
    val detalhe: String,
    val ok: Boolean,
)

/** Etapas do protótipo, com o que realmente aconteceu na leitura. */
fun etapasDaLeitura(
    leitura: Leitura.Lida,
    revisao: RevisaoUi,
): List<Etapa> {
    val paginas = if (leitura.paginas == 1) "1 página" else "${leitura.paginas} páginas"
    val data = revisao.dataReferencia
    val conferencia = revisao.conferencia
    return listOf(
        Etapa("Arquivo lido", paginas, ok = true),
        Etapa("Formato identificado: XPerformance (XP)", "Leitura por parser dedicado, sem IA", ok = true),
        Etapa(
            "${revisao.totalPosicoes} posições extraídas",
            if (data != null) "Data de referência: ${Formatacao.data(data)}" else "Sem data de referência: informe na revisão",
            ok = data != null,
        ),
        when {
            conferencia == null -> Etapa("Patrimônio não informado no arquivo", "A soma não pôde ser conferida", ok = false)
            conferencia.dentroDaTolerancia ->
                Etapa(
                    "Soma confere com o patrimônio",
                    "Diferença de ${Formatacao.percentual(conferencia.percentual)} (tolerância 0,5%)",
                    ok = true,
                )
            else ->
                Etapa(
                    "Soma não confere com o patrimônio",
                    "Diferença de ${Formatacao.percentual(conferencia.percentual)}, acima da tolerância de 0,5%",
                    ok = false,
                )
        },
    )
}

@Composable
private fun EtapaLinha(etapa: Etapa) {
    val (fundo, texto, simbolo) =
        if (etapa.ok) {
            Triple(Castanha.cores.feedbackPositiveSoft, Castanha.cores.feedbackPositiveSemiIntense, "✓")
        } else {
            Triple(Castanha.cores.feedbackWarningSoft, Castanha.cores.feedbackWarningIntense, "!")
        }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier.size(20.dp).background(fundo, CircleShape).clearAndSetSemantics { },
            contentAlignment = Alignment.Center,
        ) {
            Text(simbolo, style = Castanha.tipografia.rotuloForte, color = texto)
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.semantics(mergeDescendants = true) { },
        ) {
            Text(etapa.titulo, style = Castanha.tipografia.corpo, color = Castanha.cores.textIntense)
            Text(etapa.detalhe, style = Castanha.tipografia.legenda, color = Castanha.cores.textMedium)
        }
    }
}

fun mensagemDe(motivo: MotivoFalha): String =
    when (motivo) {
        MotivoFalha.FORMATO_NAO_SUPORTADO -> "Por enquanto o app lê só o PDF XPerformance da XP. A planilha .xlsx chega em breve."
        MotivoFalha.RELATORIO_NAO_RECONHECIDO ->
            "Este PDF não é um relatório XPerformance. Outros formatos serão lidos com ajuda de IA numa próxima versão."
        MotivoFalha.PDF_PROTEGIDO -> "O PDF está protegido por senha. Exporte o relatório sem senha e tente de novo."
        MotivoFalha.ARQUIVO_ILEGIVEL -> "O arquivo está corrompido ou não pôde ser aberto."
        MotivoFalha.ARQUIVO_GRANDE_DEMAIS -> "O arquivo passa de 20 MB."
        MotivoFalha.ARQUIVO_VAZIO -> "O arquivo está vazio."
    }

private fun tamanho(bytes: Int): String {
    val kb = bytes / BYTES_POR_KB
    return if (kb < BYTES_POR_KB) {
        "${kb.coerceAtLeast(1.0).toInt()} KB"
    } else {
        String.format(Locale.forLanguageTag("pt-BR"), "%.1f MB", kb / BYTES_POR_KB)
    }
}

private const val BYTES_POR_KB = 1024.0
