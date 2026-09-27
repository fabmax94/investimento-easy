package com.investimentoeasy.core.importacao

import com.investimentoeasy.core.domain.snapshot.PrepararRevisao
import com.investimentoeasy.core.domain.snapshot.Revisao
import com.investimentoeasy.parser.xperformance.ResultadoParse
import com.investimentoeasy.parser.xperformance.XPerformanceParser

/** Arquivo escolhido pelo usuário. */
public class ArquivoRecebido(
    public val nome: String,
    public val tipoMime: String?,
    public val conteudo: ByteArray,
)

/** Extrai o texto de cada página de um PDF, na ordem, com o texto ordenado pela posição na página. */
public fun interface ExtratorDeTextoPdf {
    /**
     * @throws PdfProtegidoException se o PDF exigir senha.
     * @throws ArquivoIlegivelException se o arquivo não for um PDF válido.
     */
    public fun paginas(conteudo: ByteArray): List<String>
}

public class PdfProtegidoException(
    causa: Throwable? = null,
) : Exception("PDF protegido por senha", causa)

public class ArquivoIlegivelException(
    causa: Throwable? = null,
) : Exception("Arquivo ilegível", causa)

public enum class MotivoFalha {
    /** Tipo de arquivo que o app ainda não lê (ex.: .xlsx antes da T1.10). */
    FORMATO_NAO_SUPORTADO,

    /** PDF legível, mas não é um relatório XPerformance (extração por IA é a T1.20). */
    RELATORIO_NAO_RECONHECIDO,
    PDF_PROTEGIDO,
    ARQUIVO_ILEGIVEL,
    ARQUIVO_GRANDE_DEMAIS,
    ARQUIVO_VAZIO,
}

public sealed interface ResultadoImportacao {
    public data class Lido(
        val nomeArquivo: String,
        val tamanhoBytes: Int,
        val paginas: Int,
        val revisao: Revisao,
    ) : ResultadoImportacao

    public data class Falha(
        val motivo: MotivoFalha,
    ) : ResultadoImportacao
}

/**
 * Do arquivo escolhido até a revisão (tela "Revisar extração"): valida tamanho e tipo,
 * extrai o texto, reconhece o formato com o parser determinístico (R2) e prepara o rascunho
 * com classificação e validação (R1, R3, R5, R6).
 */
public class ImportarRelatorio(
    private val extrator: ExtratorDeTextoPdf,
    private val preparar: PrepararRevisao = PrepararRevisao(),
) {
    /** Detalhe interno: novos formatos (xlsx, IA) entram aqui sem mudar quem importa. */
    private val parser = XPerformanceParser()

    public fun importar(arquivo: ArquivoRecebido): ResultadoImportacao {
        val tamanho = arquivo.conteudo.size
        val falha =
            when {
                tamanho == 0 -> MotivoFalha.ARQUIVO_VAZIO
                tamanho > TAMANHO_MAXIMO_BYTES -> MotivoFalha.ARQUIVO_GRANDE_DEMAIS
                !ehPdf(arquivo) -> MotivoFalha.FORMATO_NAO_SUPORTADO
                else -> null
            }
        if (falha != null) return ResultadoImportacao.Falha(falha)

        return extrair(arquivo.conteudo).fold(
            onSuccess = { paginas ->
                when (val parse = parser.parse(paginas)) {
                    ResultadoParse.FormatoNaoReconhecido -> ResultadoImportacao.Falha(MotivoFalha.RELATORIO_NAO_RECONHECIDO)
                    is ResultadoParse.Sucesso -> ResultadoImportacao.Lido(arquivo.nome, tamanho, paginas.size, preparar(parse.extracao))
                }
            },
            onFailure = { erro ->
                val motivo = if (erro is PdfProtegidoException) MotivoFalha.PDF_PROTEGIDO else MotivoFalha.ARQUIVO_ILEGIVEL
                ResultadoImportacao.Falha(motivo)
            },
        )
    }

    /** Só as falhas esperadas do extrator viram resultado; qualquer outro erro é bug e propaga. */
    private fun extrair(conteudo: ByteArray): Result<List<String>> =
        try {
            Result.success(extrator.paginas(conteudo))
        } catch (e: PdfProtegidoException) {
            Result.failure(e)
        } catch (e: ArquivoIlegivelException) {
            Result.failure(e)
        }

    /** Pelo conteúdo (assinatura `%PDF`), não só pela extensão ou pelo MIME informado. */
    private fun ehPdf(arquivo: ArquivoRecebido): Boolean {
        val assinatura = arquivo.conteudo.take(ASSINATURA_PDF.size).toByteArray()
        return assinatura.contentEquals(ASSINATURA_PDF)
    }

    public companion object {
        /** Limite do protótipo: "até 20 MB". */
        public const val TAMANHO_MAXIMO_BYTES: Int = 20 * 1024 * 1024
        private val ASSINATURA_PDF = "%PDF".toByteArray(Charsets.US_ASCII)
    }
}
