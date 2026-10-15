package com.antonio.mypdffree.domain.usecase.recent

import com.antonio.mypdffree.domain.model.result.AppResult
import com.antonio.mypdffree.domain.model.result.DomainResult
import com.antonio.mypdffree.domain.model.result.getOrElse
import com.antonio.mypdffree.domain.repository.DocumentAccessRepositoryInterface
import com.antonio.mypdffree.domain.repository.RecentDocumentRepositoryInterface
import javax.inject.Inject

/**
 * Quita un documento del historial y libera su permiso. Primero se borra la fila (lo que ve el
 * usuario); liberar el permiso es limpieza: si falla, la entrada ya no esta y no se reporta error.
 */
class RemoveRecentDocumentUseCase @Inject constructor(
    private val recentRepository: RecentDocumentRepositoryInterface,
    private val accessRepository: DocumentAccessRepositoryInterface,
) {
    suspend operator fun invoke(uri: String): AppResult<Unit> {
        recentRepository.removeRecent(uri).getOrElse { return DomainResult.Failure(it) }
        accessRepository.releaseAccess(uri)
        return DomainResult.Success(Unit)
    }
}
