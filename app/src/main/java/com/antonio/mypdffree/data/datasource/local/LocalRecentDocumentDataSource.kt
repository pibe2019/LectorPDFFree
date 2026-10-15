package com.antonio.mypdffree.data.datasource.local

import com.antonio.mypdffree.data.local.room.dao.RecentDocumentDao
import com.antonio.mypdffree.data.local.room.entities.RecentDocumentEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LocalRecentDocumentDataSource @Inject constructor(
    private val dao: RecentDocumentDao,
) : LocalRecentDocumentDataSourceInterface {

    override fun observeAll(): Flow<List<RecentDocumentEntity>> = dao.observeAll()

    override suspend fun getByUri(uri: String): RecentDocumentEntity? = dao.getByUri(uri)

    override suspend fun upsert(entity: RecentDocumentEntity) = dao.upsert(entity)

    override suspend fun updateLastPage(uri: String, pageIndex: Int): Int = dao.updateLastPage(uri, pageIndex)

    override suspend fun deleteByUri(uri: String): Int = dao.deleteByUri(uri)

    override suspend fun deleteAllReturningUris(): List<String> = dao.deleteAllReturningUris()

    override suspend fun trimTo(keep: Int): List<String> = dao.trimTo(keep)
}
