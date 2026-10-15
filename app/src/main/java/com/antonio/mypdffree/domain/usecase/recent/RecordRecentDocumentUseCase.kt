package com.antonio.mypdffree.domain.usecase.recent

import com.antonio.mypdffree.domain.model.PdfDocumentInfo
import com.antonio.mypdffree.domain.model.RecentDocument
import com.antonio.mypdffree.domain.model.result.AppResult
import com.antonio.mypdffree.domain.model.result.DomainResult
import com.antonio.mypdffree.domain.model.result.getOrElse
import com.antonio.mypdffree.domain.repository.DocumentAccessRepositoryInterface
import com.antonio.mypdffree.domain.repository.RecentDocumentRepositoryInterface
import com.antonio.mypdffree.domain.util.TimeProvider
import javax.inject.Inject

/**
 * Registra un documento en el historial.
 *
 * Regla de negocio: solo entra al historial lo que se puede VOLVER a abrir. Si el origen no permite
 * retener el acceso (p. ej. un adjunto abierto desde otra app), una entrada en recientes fallaria al
 * tocarla; por eso se descarta y se devuelve `false`.
 */
class RecordRecentDocumentUseCase @Inject constructor(
    private val accessRepository: DocumentAccessRepositoryInterface,
    private val recentRepository: RecentDocumentRepositoryInterface,
    private val timeProvider: TimeProvider,
) {
    suspend operator fun invoke(info: PdfDocumentInfo, lastPageIndex: Int): AppResult<Boolean> {
        val retained = accessRepository.retainAccess(info.uri).getOrElse { return DomainResult.Failure(it) }
        if (!retained) return DomainResult.Success(false)

        recentRepository.saveRecent(
            RecentDocument(
                uri = info.uri,
                displayName = info.displayName,
                sizeBytes = info.sizeBytes,
                pageCount = info.pageCount,
                lastPageIndex = lastPageIndex,
                lastOpenedAtMillis = timeProvider.nowMillis(),
            )
        ).getOrElse { return DomainResult.Failure(it) }

        // Android limita los permisos persistidos por app (128 antes de Android 11, 512 despues):
        // recortar el historial y liberar lo descartado evita llegar a ese techo.
        val discarded = recentRepository.trimTo(MAX_RECENT_DOCUMENTS).getOrElse { return DomainResult.Failure(it) }
        discarded.forEach { uri -> accessRepository.releaseAccess(uri) } // best effort: no invalida el registro
        return DomainResult.Success(true)
    }

    companion object {
        const val MAX_RECENT_DOCUMENTS = 50
    }
}
