package com.antonio.mypdffree.domain.usecase.reader

import com.antonio.mypdffree.domain.model.OpenedPdf
import com.antonio.mypdffree.domain.model.result.AppResult
import com.antonio.mypdffree.domain.model.result.DomainResult
import com.antonio.mypdffree.domain.model.result.getOrElse
import com.antonio.mypdffree.domain.model.result.getOrNull
import com.antonio.mypdffree.domain.repository.PdfDocumentRepositoryInterface
import com.antonio.mypdffree.domain.repository.RecentDocumentRepositoryInterface
import com.antonio.mypdffree.domain.usecase.recent.RecordRecentDocumentUseCase
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Abre un PDF, calcula donde retomar la lectura y lo registra en el historial.
 *
 * El historial es secundario: si Room falla, el usuario igual lee su documento. Por eso los fallos
 * de `getRecent` y de [RecordRecentDocumentUseCase] no convierten la apertura en error.
 */
class OpenPdfDocumentUseCase @Inject constructor(
    private val pdfRepository: PdfDocumentRepositoryInterface,
    private val recentRepository: RecentDocumentRepositoryInterface,
    private val recordRecentDocument: RecordRecentDocumentUseCase,
) {
    suspend operator fun invoke(uri: String): AppResult<OpenedPdf> {
        val handle = pdfRepository.open(uri).getOrElse { return DomainResult.Failure(it) }
        return try {
            val lastPage = recentRepository.getRecent(uri).getOrNull()?.lastPageIndex ?: 0
            // El archivo pudo cambiar desde la ultima lectura (menos paginas): nunca restaurar fuera de rango.
            val initialPage = lastPage.coerceIn(0, (handle.info.pageCount - 1).coerceAtLeast(0))
            recordRecentDocument(handle.info, initialPage)
            DomainResult.Success(OpenedPdf(handle, initialPage))
        } catch (e: CancellationException) {
            // Desde aqui el handle es nuestro: si nos cancelan (el usuario salio) nadie mas lo cerraria
            // y quedaria un descriptor de archivo abierto. No se convierte en error: se relanza.
            handle.close()
            throw e
        }
    }
}
