package com.antonio.mypdffree.domain.usecase.recent

import com.antonio.mypdffree.domain.model.result.AppResult
import com.antonio.mypdffree.domain.model.result.DomainResult
import com.antonio.mypdffree.domain.model.result.getOrElse
import com.antonio.mypdffree.domain.repository.DocumentAccessRepositoryInterface
import com.antonio.mypdffree.domain.repository.RecentDocumentRepositoryInterface
import javax.inject.Inject

/** Vacia el historial y libera todos los permisos retenidos (misma politica que [RemoveRecentDocumentUseCase]). */
class ClearRecentDocumentsUseCase @Inject constructor(
    private val recentRepository: RecentDocumentRepositoryInterface,
    private val accessRepository: DocumentAccessRepositoryInterface,
) {
    suspend operator fun invoke(): AppResult<Unit> {
        val removed = recentRepository.removeAll().getOrElse { return DomainResult.Failure(it) }
        removed.forEach { uri -> accessRepository.releaseAccess(uri) }
        return DomainResult.Success(Unit)
    }
}
