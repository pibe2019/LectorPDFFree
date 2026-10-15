package com.antonio.mypdffree.data.datasource.local

import com.antonio.mypdffree.data.local.room.entities.RecentDocumentEntity
import kotlinx.coroutines.flow.Flow

interface LocalRecentDocumentDataSourceInterface {
    fun observeAll(): Flow<List<RecentDocumentEntity>>
    suspend fun getByUri(uri: String): RecentDocumentEntity?
    suspend fun upsert(entity: RecentDocumentEntity)
    suspend fun updateLastPage(uri: String, pageIndex: Int): Int
    suspend fun deleteByUri(uri: String): Int
    suspend fun deleteAllReturningUris(): List<String>
    suspend fun trimTo(keep: Int): List<String>
}
