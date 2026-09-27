package com.investimentoeasy.core.domain.snapshot

import com.investimentoeasy.core.domain.validacao.Problema
import com.investimentoeasy.core.model.SnapshotId
import com.investimentoeasy.core.model.StatusSnapshot
import com.investimentoeasy.core.testing.FakeSnapshotRepository
import com.investimentoeasy.core.testing.INSTANTE_FIXO
import com.investimentoeasy.core.testing.extracao
import com.investimentoeasy.core.testing.geradorSequencial
import com.investimentoeasy.core.testing.posicaoExtraida
import com.investimentoeasy.core.testing.relogioFixo
import com.investimentoeasy.core.testing.snapshotConfirmado
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ConfirmarSnapshotTest {
    private val setembro = LocalDate.of(2026, 9, 3)
    private val preparar = PrepararRevisao(geradorId = geradorSequencial("novo"))

    private fun revisao(
        data: LocalDate? = setembro,
        patrimonio: String? = "1000.00",
    ) = preparar(extracao(listOf(posicaoExtraida(saldo = "1000.00")), patrimonio = patrimonio, dataReferencia = data))

    private fun confirmar(repo: FakeSnapshotRepository) = ConfirmarSnapshot(repo, relogioFixo())

    @Test
    fun `R8 - primeiro upload confirmado vira base, imutavel e datado`() =
        runTest {
            val repo = FakeSnapshotRepository()
            val resultado = confirmar(repo)(revisao()).shouldBeInstanceOf<ResultadoConfirmacao.Confirmado>()
            resultado.virouBase shouldBe true
            resultado.snapshot.status shouldBe StatusSnapshot.CONFIRMADO
            resultado.snapshot.confirmadoEm shouldBe INSTANTE_FIXO
            resultado.snapshot.versao shouldBe 1
            resultado.snapshot.divergenciaAceita shouldBe false
            repo.base() shouldBe resultado.snapshot
        }

    @Test
    fun `R1 - sem data de referencia a confirmacao e bloqueada`() =
        runTest {
            val repo = FakeSnapshotRepository()
            val resultado = confirmar(repo)(revisao(data = null)).shouldBeInstanceOf<ResultadoConfirmacao.Bloqueado>()
            resultado.motivos shouldBe listOf(Problema.DataReferenciaAusente)
            repo.confirmados() shouldHaveSize 0
        }

    @Test
    fun `R3 - divergencia bloqueia ate o usuario aceitar`() =
        runTest {
            val repo = FakeSnapshotRepository()
            val divergente = revisao(patrimonio = "1100.00")
            confirmar(repo)(divergente).shouldBeInstanceOf<ResultadoConfirmacao.Bloqueado>()
                .motivos.single().shouldBeInstanceOf<Problema.DivergenciaSoma>()
            repo.confirmados() shouldHaveSize 0

            val aceito = confirmar(repo)(divergente, aceitouDivergencia = true).shouldBeInstanceOf<ResultadoConfirmacao.Confirmado>()
            aceito.snapshot.divergenciaAceita shouldBe true
        }

    @Test
    fun `R15 - upload mais novo substitui a base e a anterior fica no historico`() =
        runTest {
            val agosto = snapshotConfirmado("ago", LocalDate.of(2026, 8, 1))
            val repo = FakeSnapshotRepository(listOf(agosto), base = agosto.id)
            confirmar(repo)(revisao()).shouldBeInstanceOf<ResultadoConfirmacao.Confirmado>().virouBase shouldBe true
            repo.base()!!.dataReferencia shouldBe setembro
            repo.confirmados() shouldHaveSize 2
        }

    @Test
    fun `R15 - upload mais antigo vai para o historico sem mudar a base`() =
        runTest {
            val outubro = snapshotConfirmado("out", LocalDate.of(2026, 10, 1))
            val repo = FakeSnapshotRepository(listOf(outubro), base = outubro.id)
            confirmar(repo)(revisao()).shouldBeInstanceOf<ResultadoConfirmacao.Confirmado>().virouBase shouldBe false
            repo.base() shouldBe outubro
            repo.confirmados() shouldHaveSize 2
        }

    @Test
    fun `R15 - mesma data pergunta antes de substituir`() =
        runTest {
            val atual = snapshotConfirmado("set", setembro)
            val repo = FakeSnapshotRepository(listOf(atual), base = atual.id)
            confirmar(repo)(revisao()) shouldBe ResultadoConfirmacao.PrecisaDecidirMesmaData(atual)
            repo.confirmados() shouldHaveSize 1
        }

    @Test
    fun `R15 - mesma data mantendo a atual nao grava nada`() =
        runTest {
            val atual = snapshotConfirmado("set", setembro)
            val repo = FakeSnapshotRepository(listOf(atual), base = atual.id)
            confirmar(repo)(revisao(), decisaoMesmaData = DecisaoMesmaData.MANTER_ATUAL) shouldBe ResultadoConfirmacao.MantidoAtual
            repo.confirmados() shouldHaveSize 1
            repo.base() shouldBe atual
        }

    @Test
    fun `R16 - substituir na mesma data cria nova versao e preserva a anterior`() =
        runTest {
            val atual = snapshotConfirmado("set", setembro)
            val repo = FakeSnapshotRepository(listOf(atual), base = atual.id)
            val resultado =
                confirmar(repo)(revisao(), decisaoMesmaData = DecisaoMesmaData.SUBSTITUIR)
                    .shouldBeInstanceOf<ResultadoConfirmacao.Confirmado>()
            resultado.snapshot.versao shouldBe 2
            resultado.virouBase shouldBe true
            repo.base()!!.id shouldBe SnapshotId("novo1")
            repo.confirmados() shouldBe listOf(atual, resultado.snapshot)
        }

    @Test
    fun `R16 - snapshot ja confirmado nao pode ser confirmado de novo`() =
        runTest {
            val repo = FakeSnapshotRepository()
            val r = revisao()
            val confirmado = confirmar(repo)(r).shouldBeInstanceOf<ResultadoConfirmacao.Confirmado>().snapshot
            shouldThrow<IllegalArgumentException> { confirmar(repo)(r.copy(rascunho = confirmado)) }
            shouldThrow<IllegalStateException> { repo.inserirConfirmado(confirmado) }
        }
}
