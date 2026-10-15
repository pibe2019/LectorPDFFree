package com.antonio.mypdffree.domain.usecase.recent

import com.antonio.mypdffree.domain.model.RecentDocument
import com.antonio.mypdffree.domain.repository.RecentDocumentRepositoryInterface
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Historial ordenado del mas reciente al mas antiguo; se re-emite solo al cambiar la tabla. */
class ObserveRecentDocumentsUseCase @Inject constructor(
    private val recentRepository: RecentDocumentRepositoryInterface,
) {
    operator fun invoke(): Flow<List<RecentDocument>> = recentRepository.observeRecents()
}
