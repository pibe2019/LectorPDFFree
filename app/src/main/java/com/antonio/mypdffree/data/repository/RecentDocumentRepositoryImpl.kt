package com.antonio.mypdffree.data.repository

import com.antonio.mypdffree.data.datasource.local.LocalRecentDocumentDataSourceInterface
import com.antonio.mypdffree.data.mapper.toDomain
import com.antonio.mypdffree.data.mapper.toEntity
import com.antonio.mypdffree.data.util.resultOf
import com.antonio.mypdffree.domain.model.RecentDocument
import com.antonio.mypdffree.domain.model.result.AppResult
import com.antonio.mypdffree.domain.model.result.map
import com.antonio.mypdffree.domain.repository.RecentDocumentRepositoryInterface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Sin withContext(IO): las funciones suspend y los Flow de Room ya son main-safe (Room usa su propio
 * executor). Envolverlas otra vez solo añade un cambio de hilo inutil.
 */
class RecentDocumentRepositoryImpl @Inject constructor(
    private val localDataSource: LocalRecentDocumentDataSourceInterface,
) : RecentDocumentRepositoryInterface {

    override fun observeRecents(): Flow<List<RecentDocument>> =
        localDataSource.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getRecent(uri: String): AppResult<RecentDocument?> =
        resultOf { localDataSource.getByUri(uri)?.toDomain() }

    override suspend fun saveRecent(document: RecentDocument): AppResult<Unit> =
        resultOf { localDataSource.upsert(document.toEntity()) }

    override suspend fun updateLastPage(uri: String, pageIndex: Int): AppResult<Unit> =
        resultOf { localDataSource.updateLastPage(uri, pageIndex) }.map { } // filas afectadas: no le importan al dominio

    override suspend fun removeRecent(uri: String): AppResult<Unit> =
        resultOf { localDataSource.deleteByUri(uri) }.map { }

    override suspend fun removeAll(): AppResult<List<String>> =
        resultOf { localDataSource.deleteAllReturningUris() }

    override suspend fun trimTo(maxEntries: Int): AppResult<List<String>> =
        resultOf { localDataSource.trimTo(maxEntries) }
}
