package com.antonio.mypdffree.domain.repository

import com.antonio.mypdffree.domain.model.RecentDocument
import com.antonio.mypdffree.domain.model.result.AppResult
import kotlinx.coroutines.flow.Flow

interface RecentDocumentRepositoryInterface {
    /** Room emite siempre un primer valor y una lectura local no falla: no se envuelve en AppResult. */
    fun observeRecents(): Flow<List<RecentDocument>>
    suspend fun getRecent(uri: String): AppResult<RecentDocument?>
    suspend fun saveRecent(document: RecentDocument): AppResult<Unit>
    suspend fun updateLastPage(uri: String, pageIndex: Int): AppResult<Unit>
    suspend fun removeRecent(uri: String): AppResult<Unit>
    /** Borra todo y devuelve los URIs borrados, para liberar su acceso. */
    suspend fun removeAll(): AppResult<List<String>>
    /** Deja solo los [maxEntries] mas recientes y devuelve los URIs descartados. */
    suspend fun trimTo(maxEntries: Int): AppResult<List<String>>
}
