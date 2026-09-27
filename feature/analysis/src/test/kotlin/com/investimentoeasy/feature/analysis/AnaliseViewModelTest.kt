package com.investimentoeasy.feature.analysis

import com.investimentoeasy.core.ai.FabricaDeModelo
import com.investimentoeasy.core.ai.RespostaDoModelo
import com.investimentoeasy.core.ai.paraJson
import com.investimentoeasy.core.domain.analise.AnaliseGuardada
import com.investimentoeasy.core.seguranca.CofreDeChave
import com.investimentoeasy.core.seguranca.CofreIndisponivelException
import com.investimentoeasy.core.testing.FakeRepositorioDeAnalises
import com.investimentoeasy.core.testing.FakeRepositorioDeComplementos
import com.investimentoeasy.core.testing.FakeSnapshotRepository
import com.investimentoeasy.core.testing.INSTANTE_FIXO
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

@OptIn(ExperimentalCoroutinesApi::class)
class AnaliseViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val analises = FakeRepositorioDeAnalises()
    private val cofre =
        object : CofreDeChave {
            var chave: String? = null
            var falhar = false

            override fun ler() = chave

            override fun gravar(chave: String) {
                if (falhar) throw CofreIndisponivelException(java.security.ProviderException("Keystore"))
                this.chave = chave
            }

            override fun apagar() {
                chave = null
            }
        }
    private val chavesUsadas = mutableListOf<String>()
    private val pedidos = mutableListOf<com.investimentoeasy.core.ai.PedidoAoModelo>()
    private val complementos = FakeRepositorioDeComplementos()
    private var resposta: RespostaDoModelo =
        RespostaDoModelo.Texto(
            Cenarios.saida.paraJson(),
            "claude-opus-5",
        )

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(comBase: Boolean = true): AnaliseViewModel {
        val repo =
            if (comBase) FakeSnapshotRepository(listOf(Cenarios.snapshot), Cenarios.snapshot.id) else FakeSnapshotRepository()
        val fabrica =
            FabricaDeModelo { chave ->
                chavesUsadas += chave
                com.investimentoeasy.core.ai.ModeloDeLinguagem { pedido ->
                    pedidos += pedido
                    resposta
                }
            }
        return AnaliseViewModel(
            FontesDaAnalise(repo, analises, complementos),
            cofre,
            fabrica,
            relogioFixo(),
            dispatcher,
        ).apply { carregar() }
    }

    @Test
    fun `sem carteira mostra estado vazio`() {
        val estado = viewModel(comBase = false).estado.value
        estado.carregando shouldBe false
        estado.semCarteira shouldBe true
    }

    @Test
    fun `camadas 1 e 2 rodam sem chave e sem custo`() {
        val estado = viewModel().estado.value
        estado.deterministica.shouldNotBeNull().alertas shouldHaveSize 8
        estado.ia.shouldBeNull()
        estado.temChave shouldBe false
    }

    @Test
    fun `sem chave, gerar pede a configuracao e nao chama o modelo`() {
        val vm = viewModel()
        vm.gerarAnalise()
        vm.estado.value.erro!! shouldContain "Configure a chave"
        chavesUsadas shouldHaveSize 0
    }

    @Test
    fun `com chave, gera, valida, guarda e mostra a analise`() {
        val vm = viewModel()
        vm.salvarChave("sk-ant-123")
        vm.estado.value.temChave shouldBe true
        vm.gerarAnalise()
        chavesUsadas shouldBe listOf("sk-ant-123")
        vm.estado.value.ia!!.saida shouldBe Cenarios.saida
        vm.estado.value.gerando shouldBe false
        analises.salvas.single().let {
            it.snapshotId shouldBe Cenarios.snapshot.id
            it.geradaEm shouldBe INSTANTE_FIXO
            it.versaoPrompt shouldBe "analise-v2"
        }
    }

    @Test
    fun `analise guardada volta sem nova chamada`() {
        kotlinx.coroutines.runBlocking {
            analises.salvar(AnaliseGuardada(Cenarios.snapshot.id, INSTANTE_FIXO, "claude-opus-5", "analise-v1", Cenarios.saida.paraJson()))
        }
        viewModel().estado.value.ia!!.saida shouldBe Cenarios.saida
        chavesUsadas shouldHaveSize 0
    }

    @Test
    fun `numero inventado nas duas tentativas nao vira analise nem e salvo`() {
        resposta =
            RespostaDoModelo.Texto(Cenarios.saida.copy(veredicto = "Rendeu 99,9%.").paraJson(), "m")
        val vm = viewModel()
        vm.salvarChave("sk")
        vm.gerarAnalise()
        vm.estado.value.ia.shouldBeNull()
        vm.estado.value.erro!! shouldContain "foi descartada"
        analises.salvas shouldHaveSize 0
    }

    @Test
    fun `falhas da API viram mensagens`() {
        mapOf(
            RespostaDoModelo.ChaveInvalida to "chave da API foi recusada",
            RespostaDoModelo.SemConexao to "Sem conexão",
            RespostaDoModelo.LimiteDeUso to "Limite de uso",
        ).forEach { (falha, texto) ->
            resposta = falha
            val vm = viewModel()
            vm.salvarChave("sk")
            vm.gerarAnalise()
            vm.estado.value.erro!! shouldContain texto
        }
    }

    @Test
    fun `abas e chave`() {
        val vm = viewModel()
        vm.selecionarAba(AbaAnalise.FIIS)
        vm.estado.value.aba shouldBe AbaAnalise.FIIS
        vm.salvarChave("  ")
        vm.estado.value.temChave shouldBe false
        vm.salvarChave("sk")
        vm.apagarChave()
        vm.estado.value.temChave shouldBe false
        cofre.chave.shouldBeNull()
    }

    @Test
    fun `planilha guardada entra no estado e na entrada do Claude`() =
        runTest {
            complementos.salvar(Cenarios.complemento)
            cofre.gravar("sk-ant-teste")
            val vm = viewModel()
            vm.estado.value.complemento shouldBe Cenarios.complemento
            vm.gerarAnalise()
            pedidos.single().mensagem shouldContain "\"dataPlanilha\":\"2026-06-21\""
            pedidos.single().mensagem shouldContain "\"precoMedio\":\"151.23\""
        }

    @Test
    fun `falha do armazenamento seguro ao salvar a chave vira mensagem, sem derrubar o app`() {
        cofre.falhar = true
        val vm = viewModel()
        vm.salvarChave("sk-ant-teste")
        vm.estado.value.temChave shouldBe false
        vm.estado.value.erro shouldBe ERRO_COFRE
        cofre.chave.shouldBeNull()
    }
}
