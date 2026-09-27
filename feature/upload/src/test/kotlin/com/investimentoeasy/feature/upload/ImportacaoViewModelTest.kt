package com.investimentoeasy.feature.upload

import android.net.Uri
import com.investimentoeasy.core.documentos.LeitorDeArquivo
import com.investimentoeasy.core.domain.snapshot.ConfirmarSnapshot
import com.investimentoeasy.core.domain.snapshot.DecisaoMesmaData
import com.investimentoeasy.core.importacao.ArquivoRecebido
import com.investimentoeasy.core.importacao.MotivoFalha
import com.investimentoeasy.core.testing.FakeSnapshotRepository
import com.investimentoeasy.core.testing.relogioFixo
import com.investimentoeasy.core.testing.snapshotConfirmado
import com.investimentoeasy.parser.xperformance.FixturesXPerformance
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ImportacaoViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val uri = mockk<Uri>()
    private val leitor = mockk<LeitorDeArquivo>()
    private val repositorio = FakeSnapshotRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { leitor.ler(uri) } returns ArquivoRecebido("XPerformance.pdf", "application/pdf", Cenarios.pdf)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(
        paginas: List<String> = FixturesXPerformance.sintetico(),
        repo: FakeSnapshotRepository = repositorio,
    ) = ImportacaoViewModel(leitor, Cenarios.importar(paginas), ConfirmarSnapshot(repo, relogioFixo()), dispatcher)

    @Test
    fun `arquivo lido vira revisao`() {
        val vm = viewModel()
        vm.arquivoEscolhido(uri)
        val estado = vm.estado.value
        estado.leitura shouldBe Leitura.Lida("XPerformance.pdf", Cenarios.pdf.size, FixturesXPerformance.PAGINAS)
        estado.revisaoUi!!.podeConfirmar shouldBe true
    }

    @Test
    fun `falha de leitura do arquivo e de formato viram mensagem, sem revisao`() {
        every { leitor.ler(uri) } throws IOException("sem permissão")
        val vm = viewModel()
        vm.arquivoEscolhido(uri)
        vm.estado.value.leitura shouldBe Leitura.Falhou(MotivoFalha.ARQUIVO_ILEGIVEL)
        vm.estado.value.revisao.shouldBeNull()

        val outroPdf = viewModel(paginas = listOf("Extrato de outro banco"))
        every { leitor.ler(uri) } returns ArquivoRecebido("x.pdf", null, Cenarios.pdf)
        outroPdf.arquivoEscolhido(uri)
        outroPdf.estado.value.leitura shouldBe Leitura.Falhou(MotivoFalha.RELATORIO_NAO_RECONHECIDO)
    }

    @Test
    fun `R8 - confirmar grava a base e conclui`() =
        runTest(dispatcher) {
            val vm = viewModel()
            vm.arquivoEscolhido(uri)
            vm.confirmar()
            vm.estado.value.conclusao shouldBe Conclusao.NOVA_BASE
            repositorio.base()!!.dataReferencia shouldBe LocalDate.of(2026, 9, 3)
            vm.conclusaoTratada()
            vm.estado.value shouldBe EstadoImportacao()
        }

    @Test
    fun `R3 - com divergencia so confirma depois do aceite`() =
        runTest(dispatcher) {
            val vm = viewModel(Cenarios.paginasComDivergencia())
            vm.arquivoEscolhido(uri)
            vm.confirmar()
            repositorio.confirmados() shouldHaveSize 0
            vm.estado.value.conclusao.shouldBeNull()

            vm.aceitarDivergencia(true)
            vm.confirmar()
            vm.estado.value.conclusao shouldBe Conclusao.NOVA_BASE
            repositorio.base()!!.divergenciaAceita shouldBe true
        }

    @Test
    fun `R1 - data informada pelo usuario libera a confirmacao`() =
        runTest(dispatcher) {
            val vm = viewModel(Cenarios.paginasSemData())
            vm.arquivoEscolhido(uri)
            vm.estado.value.revisaoUi!!.podeConfirmar shouldBe false
            vm.informarDataReferencia(LocalDate.of(2026, 8, 31))
            vm.estado.value.revisaoUi!!.podeConfirmar shouldBe true
            vm.confirmar()
            repositorio.base()!!.dataReferencia shouldBe LocalDate.of(2026, 8, 31)
        }

    @Test
    fun `R15 - mesma data pergunta e respeita a decisao`() =
        runTest(dispatcher) {
            val atual = snapshotConfirmado("atual", LocalDate.of(2026, 9, 3))
            val repo = FakeSnapshotRepository(listOf(atual), base = atual.id)
            val vm = viewModel(repo = repo)
            vm.arquivoEscolhido(uri)
            vm.confirmar()
            vm.estado.value.baseMesmaData shouldBe LocalDate.of(2026, 9, 3)

            vm.cancelarDecisaoMesmaData()
            vm.estado.value.baseMesmaData.shouldBeNull()

            vm.confirmar()
            vm.confirmar(DecisaoMesmaData.MANTER_ATUAL)
            vm.estado.value.conclusao shouldBe Conclusao.MANTIDA_BASE_ATUAL
            repo.confirmados() shouldHaveSize 1

            vm.conclusaoTratada()
            vm.arquivoEscolhido(uri)
            vm.confirmar(DecisaoMesmaData.SUBSTITUIR)
            vm.estado.value.conclusao shouldBe Conclusao.NOVA_BASE
            repo.base()!!.versao shouldBe 2
        }

    @Test
    fun `R15 - upload mais antigo vai para o historico`() =
        runTest(dispatcher) {
            val outubro = snapshotConfirmado("out", LocalDate.of(2026, 10, 1))
            val repo = FakeSnapshotRepository(listOf(outubro), base = outubro.id)
            val vm = viewModel(repo = repo)
            vm.arquivoEscolhido(uri)
            vm.confirmar()
            vm.estado.value.conclusao shouldBe Conclusao.HISTORICO
        }

    @Test
    fun `sem revisao, confirmar e informar data nao fazem nada`() {
        val vm = viewModel()
        vm.confirmar()
        vm.informarDataReferencia(LocalDate.of(2026, 1, 1))
        vm.estado.value shouldBe EstadoImportacao()
        vm.estado.value.leitura.shouldBeInstanceOf<Leitura.Aguardando>()
    }
}
