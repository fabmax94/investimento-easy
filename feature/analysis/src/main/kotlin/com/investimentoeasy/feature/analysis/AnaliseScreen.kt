package com.investimentoeasy.feature.analysis

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.investimentoeasy.core.designsystem.Castanha
import com.investimentoeasy.core.designsystem.Formatacao
import com.investimentoeasy.core.designsystem.componentes.BotaoTexto
import com.investimentoeasy.core.designsystem.componentes.CabecalhoTela
import com.investimentoeasy.core.designsystem.componentes.CartaoSuave
import com.investimentoeasy.core.designsystem.componentes.ChipOrigem
import com.investimentoeasy.core.designsystem.componentes.Raios
import com.investimentoeasy.core.model.Origem

/** Tela 5 do protótipo: as 7 abas da skill, com a origem dos números e o disclaimer. */
@Composable
fun AnaliseScreen(
    estado: EstadoAnalise,
    aoSelecionarAba: (AbaAnalise) -> Unit,
    aoGerar: () -> Unit,
    aoSalvarChave: (String) -> Unit,
    aoApagarChave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var editandoChave by remember { mutableStateOf(false) }
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(Castanha.cores.surfaceDefault)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        CabecalhoTela("Análise", subtitulo = estado.base?.let { "Base ${Formatacao.data(it.dataReferencia)}" })
        val d = estado.deterministica
        when {
            estado.carregando ->
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Castanha.cores.accentSolidMedium)
                }
            estado.semCarteira || d == null ->
                CartaoSuave {
                    Text("Sem carteira para analisar", style = Castanha.tipografia.secao, color = Castanha.cores.textIntense)
                    Text(
                        "Envie o relatório XPerformance na aba Enviar.",
                        style = Castanha.tipografia.corpo,
                        color = Castanha.cores.textMedium,
                    )
                }
            else -> {
                BarraDeAbas(estado.aba, aoSelecionarAba)
                when (estado.aba) {
                    AbaAnalise.O_QUE_FAZER -> AbaOQueFazer(estado, d, aoGerar, aoConfigurarChave = { editandoChave = true })
                    AbaAnalise.MERCADO -> AbaMercado(estado, d)
                    AbaAnalise.ALOCACAO -> AbaAlocacao(estado, d)
                    AbaAnalise.FUNDOS -> AbaFundos(estado, d)
                    AbaAnalise.FIIS -> AbaFiis(estado, d)
                    AbaAnalise.ACOES_ETFS -> AbaAcoesEtfs(estado, d)
                    AbaAnalise.ALERTAS -> AbaAlertas(estado, d)
                }
                OrigemDosNumeros()
                ChaveDaApi(estado.temChave, aoEditar = { editandoChave = true }, aoApagar = aoApagarChave)
            }
        }
        Text(
            "Análise informacional e educacional. Não constitui recomendação formal de investimento. " +
                "Consulte seu assessor antes de operar. Rentabilidade passada não garante retorno futuro.",
            style = Castanha.tipografia.legenda,
            color = Castanha.cores.textMedium,
        )
    }
    if (editandoChave) {
        DialogoChave(aoSalvar = {
            editandoChave = false
            aoSalvarChave(it)
        }, aoCancelar = { editandoChave = false })
    }
}

@Composable
private fun BarraDeAbas(
    selecionada: AbaAnalise,
    aoSelecionar: (AbaAnalise) -> Unit,
) {
    val cores = Castanha.cores
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AbaAnalise.entries.forEach { aba ->
            val ativa = aba == selecionada
            Box(
                modifier =
                    Modifier
                        .heightIn(min = 40.dp)
                        .clip(Raios.pilula)
                        .background(if (ativa) cores.surfaceInversed else cores.surfaceDefault)
                        .border(1.dp, if (ativa) cores.surfaceInversed else cores.borderSemiSoft, Raios.pilula)
                        .selectable(selected = ativa, role = Role.Tab, onClick = { aoSelecionar(aba) })
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    aba.rotulo,
                    style = if (ativa) Castanha.tipografia.acao else Castanha.tipografia.corpo,
                    color = if (ativa) cores.textInversed else cores.textIntense,
                )
            }
        }
    }
}

@Composable
private fun OrigemDosNumeros() {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Origem dos números:", style = Castanha.tipografia.legenda, color = Castanha.cores.textMedium)
        ChipOrigem(Origem.RELATORIO)
        ChipOrigem(Origem.CALCULADO)
    }
}

@Composable
private fun ChaveDaApi(
    temChave: Boolean,
    aoEditar: () -> Unit,
    aoApagar: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (temChave) "Chave da API do Claude: configurada" else "Chave da API do Claude: não configurada",
            style = Castanha.tipografia.legenda,
            color = Castanha.cores.textMedium,
            modifier = Modifier.weight(1f),
        )
        if (temChave) BotaoTexto("Remover", onClick = aoApagar) else BotaoTexto("Configurar", onClick = aoEditar)
    }
}

@Composable
internal fun DialogoChave(
    aoSalvar: (String) -> Unit,
    aoCancelar: () -> Unit,
) {
    var chave by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = aoCancelar,
        title = { Text("Chave da API do Claude") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Crie uma chave em console.anthropic.com. Ela fica cifrada no aparelho (Android Keystore) e só é usada " +
                        "para gerar a análise.",
                    style = Castanha.tipografia.corpo,
                )
                OutlinedTextField(
                    value = chave,
                    onValueChange = { chave = it },
                    singleLine = true,
                    label = { Text("Chave (sk-ant-…)") },
                    visualTransformation = PasswordVisualTransformation(),
                )
            }
        },
        confirmButton = { BotaoTexto("Salvar", onClick = { aoSalvar(chave) }) },
        dismissButton = { BotaoTexto("Cancelar", onClick = aoCancelar) },
    )
}
