package com.antonio.mypdffree.domain.usecase.reader

import com.antonio.mypdffree.domain.model.result.AppResult
import com.antonio.mypdffree.domain.repository.RecentDocumentRepositoryInterface
import javax.inject.Inject

/** Guarda la pagina en la que va el usuario para retomarla al reabrir el documento. */
class SaveReadingProgressUseCase @Inject constructor(
    private val recentRepository: RecentDocumentRepositoryInterface,
) {
    suspend operator fun invoke(uri: String, pageIndex: Int): AppResult<Unit> =
        recentRepository.updateLastPage(uri, pageIndex.coerceAtLeast(0))
}
