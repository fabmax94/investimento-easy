package com.investimentoeasy.feature.upload

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.investimentoeasy.core.designsystem.Castanha
import com.investimentoeasy.core.designsystem.Formatacao
import com.investimentoeasy.core.designsystem.componentes.BotaoPrimario
import com.investimentoeasy.core.designsystem.componentes.BotaoTexto
import com.investimentoeasy.core.designsystem.componentes.CabecalhoTela
import com.investimentoeasy.core.designsystem.componentes.CartaoAviso
import com.investimentoeasy.core.designsystem.componentes.CartaoSuave
import com.investimentoeasy.core.designsystem.componentes.Etiqueta
import com.investimentoeasy.core.designsystem.componentes.LinhaValor
import com.investimentoeasy.core.designsystem.componentes.TituloSecao
import com.investimentoeasy.core.designsystem.componentes.Tom
import com.investimentoeasy.core.domain.snapshot.DecisaoMesmaData
import com.investimentoeasy.core.model.MetodoExtracao
import com.investimentoeasy.core.ui.ListaAlocacao
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Tela 3 do protótipo: conferir antes de virar base (R3, R6, R8). */
@Composable
fun RevisarExtracaoScreen(
    revisao: RevisaoUi,
    confirmando: Boolean,
    baseMesmaData: LocalDate?,
    aoInformarData: (LocalDate) -> Unit,
    aoAceitarDivergencia: (Boolean) -> Unit,
    aoConfirmar: (DecisaoMesmaData?) -> Unit,
    aoCancelarDecisao: () -> Unit,
    aoVoltar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var escolhendoData by remember { mutableStateOf(false) }
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(Castanha.cores.surfaceDefault)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        BotaoTexto("‹ Enviar carteira", onClick = aoVoltar, alinhadoAoTexto = true)
        CabecalhoTela("Revisar extração", subtitulo = "Confira antes de virar a nova base. Nada muda até você confirmar.")
        Resumo(revisao)

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            TituloSecao("Posições por classe (${revisao.totalPosicoes})")
            ListaAlocacao(revisao.alocacao)
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (revisao.avisos.isEmpty()) {
                CartaoAviso(Tom.POSITIVO, "✓ TUDO CONFERE", "Nenhum dado fora do plausível e nenhuma pendência de classificação.")
            } else {
                TituloSecao("Precisa da sua atenção (${revisao.avisos.size})")
                revisao.avisos.forEach { aviso ->
                    CartaoAviso(aviso.tom, aviso.rotulo, aviso.texto) {
                        when (aviso.acao) {
                            AcaoAviso.INFORMAR_DATA ->
                                BotaoTexto(
                                    "Informar data",
                                    onClick = { escolhendoData = true },
                                    alinhadoAoTexto = true,
                                )
                            AcaoAviso.ACEITAR_DIVERGENCIA -> AceiteDivergencia(revisao.aceitouDivergencia, aoAceitarDivergencia)
                            null -> Unit
                        }
                    }
                }
            }
        }

        BotaoPrimario(
            if (confirmando) "Confirmando…" else "Confirmar e atualizar",
            onClick = { aoConfirmar(null) },
            habilitado = revisao.podeConfirmar && !confirmando,
        )
    }

    if (escolhendoData) {
        SeletorDeData(aoEscolher = { data ->
            escolhendoData = false
            aoInformarData(data)
        }, aoCancelar = { escolhendoData = false })
    }
    baseMesmaData?.let { data ->
        AlertDialog(
            onDismissRequest = aoCancelarDecisao,
            title = { Text("Já existe uma base de ${Formatacao.data(data)}") },
            text = {
                Text("Substituir cria uma nova versão; a anterior continua guardada no histórico. Manter descarta este upload.")
            },
            confirmButton = { BotaoTexto("Substituir", onClick = { aoConfirmar(DecisaoMesmaData.SUBSTITUIR) }) },
            dismissButton = { BotaoTexto("Manter a atual", onClick = { aoConfirmar(DecisaoMesmaData.MANTER_ATUAL) }) },
        )
    }
}

@Composable
private fun Resumo(revisao: RevisaoUi) {
    CartaoSuave(espacamento = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 18.dp)) {
        LinhaValor("Data de referência", Formatacao.data(revisao.dataReferencia))
        LinhaValor("Patrimônio informado", Formatacao.reais(revisao.patrimonioInformado))
        LinhaValor("Soma das posições", Formatacao.reais(revisao.conferencia?.somaPosicoes))
        revisao.conferencia?.let { c ->
            LinhaValor("Diferença") {
                val texto = Formatacao.percentual(c.percentual)
                if (c.dentroDaTolerancia) {
                    Etiqueta(
                        "✓ $texto · dentro de 0,5%",
                        Tom.POSITIVO,
                    )
                } else {
                    Etiqueta("$texto · acima de 0,5%", Tom.NEGATIVO)
                }
            }
        }
        LinhaValor("Leitura", if (revisao.metodo == MetodoExtracao.PARSER) "Parser XPerformance" else "Extraído por IA")
        LinhaValor("Conta e assessor", "não lidos")
    }
}

@Composable
private fun AceiteDivergencia(
    aceito: Boolean,
    aoMudar: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().toggleable(value = aceito, role = Role.Checkbox, onValueChange = aoMudar),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = aceito,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(checkedColor = Castanha.cores.accentSolidMedium),
        )
        Text(
            "Aceito a divergência e quero confirmar assim",
            style = Castanha.tipografia.corpoForte,
            color = Castanha.cores.textIntense,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeletorDeData(
    aoEscolher: (LocalDate) -> Unit,
    aoCancelar: () -> Unit,
) {
    val estado = rememberDatePickerState()
    DatePickerDialog(
        onDismissRequest = aoCancelar,
        confirmButton = {
            BotaoTexto("Usar esta data", onClick = {
                estado.selectedDateMillis?.let { aoEscolher(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
            })
        },
        dismissButton = { BotaoTexto("Cancelar", onClick = aoCancelar) },
    ) {
        DatePicker(state = estado)
    }
}
