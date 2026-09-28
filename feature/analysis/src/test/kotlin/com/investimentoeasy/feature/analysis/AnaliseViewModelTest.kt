package com.investimentoeasy.feature.analysis

import com.investimentoeasy.core.domain.mercado.Perfil
import com.investimentoeasy.core.domain.mercado.ProvedorDeMercado
import com.investimentoeasy.core.testing.FakeRepositorioDeComplementos
import com.investimentoeasy.core.testing.FakeRepositorioDeMercado
import com.investimentoeasy.core.testing.FakeRepositorioDePerfil
import com.investimentoeasy.core.testing.FakeSnapshotRepository
import com.investimentoeasy.core.testing.relogioFixo
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
class AnaliseViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val complementos = FakeRepositorioDeComplementos()
    private val mercado = FakeRepositorioDeMercado()
    private val perfil = FakeRepositorioDePerfil()
    private val pedidos = mutableListOf<Set<String>>()
    private val provedor =
        ProvedorDeMercado { tickers ->
            pedidos += tickers
            Cenarios.panorama
        }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(
        comBase: Boolean = true,
        agora: java.time.Instant = Cenarios.instante,
    ): AnaliseViewModel {
        val repo = if (comBase) FakeSnapshotRepository(listOf(Cenarios.snapshot), Cenarios.snapshot.id) else FakeSnapshotRepository()
        return AnaliseViewModel(FontesDaAnalise(repo, complementos, mercado, perfil), provedor, relogioFixo(agora), dispatcher)
            .apply { carregar() }
    }

    @Test
    fun `sem carteira mostra estado vazio e nao busca mercado`() {
        val estado = viewModel(comBase = false).estado.value
        estado.carregando shouldBe false
        estado.semCarteira shouldBe true
        pedidos shouldHaveSize 0
    }

    @Test
    fun `sem perfil calcula as camadas 1 e 2 mas nao recomenda`() {
        val estado = viewModel().estado.value
        estado.deterministica.shouldNotBeNull()
        estado.recomendacao.shouldBeNull()
        estado.perfil.shouldBeNull()
    }

    @Test
    fun `sem cache busca o mercado ao abrir, so com tickers, e guarda`() =
        runTest {
            val vm = viewModel()
            pedidos.single() shouldBe tickers(Cenarios.snapshot)
            pedidos.single().none { it.startsWith("RECR12") } shouldBe true
            mercado.ultimo() shouldBe Cenarios.panorama
            vm.estado.value.panorama shouldBe Cenarios.panorama
            vm.estado.value.atualizandoMercado shouldBe false
        }

    @Test
    fun `cache recente nao busca de novo e cache velho busca`() =
        runTest {
            mercado.salvar(Cenarios.panorama)
            viewModel(agora = Cenarios.instante.plus(Duration.ofHours(1)))
            pedidos shouldHaveSize 0
            viewModel(agora = Cenarios.instante.plus(Duration.ofHours(7)))
            pedidos shouldHaveSize 1
        }

    @Test
    fun `escolher perfil gera a recomendacao e fica guardado`() {
        val vm = viewModel()
        vm.escolherPerfil(Perfil.ARROJADO)
        perfil.ler() shouldBe Perfil.ARROJADO
        with(vm.estado.value) {
            recomendacao!!.perfil shouldBe Perfil.ARROJADO
            recomendacao!!.veredicto shouldContain "Focus"
            escolhendoPerfil shouldBe false
        }
        vm.trocarPerfil()
        vm.estado.value.escolhendoPerfil shouldBe true
    }

    @Test
    fun `mercado atualiza o cenario pelo ciclo real`() {
        perfil.gravar(Perfil.MODERADO)
        val vm = viewModel()
        val ipca = vm.estado.value.deterministica!!.ativos.first { it.posicao.ativo.nome == "IMAB11" }
        ipca.cenario.driver shouldBe "ganha marcação com juro ↓"
        vm.estado.value.recomendacao!!.mercado.first() shouldContain "Selic de 13,75%"
    }

    @Test
    fun `planilha guardada entra no estado`() =
        runTest {
            complementos.salvar(Cenarios.complemento)
            viewModel().estado.value.complemento shouldBe Cenarios.complemento
        }

    @Test
    fun `selecionar aba`() {
        val vm = viewModel()
        vm.selecionarAba(AbaAnalise.FIIS)
        vm.estado.value.aba shouldBe AbaAnalise.FIIS
    }
}
