package com.antonio.mypdffree.data.repository

import androidx.core.net.toUri
import com.antonio.mypdffree.data.datasource.device.DocumentContentDataSourceInterface
import com.antonio.mypdffree.data.util.resultOf
import com.antonio.mypdffree.di.IoDispatcher
import com.antonio.mypdffree.domain.model.result.AppResult
import com.antonio.mypdffree.domain.repository.DocumentAccessRepositoryInterface
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Las llamadas a ContentResolver son IPC sincronas con otro proceso: siempre fuera del hilo principal. */
class DocumentAccessRepositoryImpl @Inject constructor(
    private val contentDataSource: DocumentContentDataSourceInterface,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : DocumentAccessRepositoryInterface {

    override suspend fun retainAccess(uri: String): AppResult<Boolean> = withContext(ioDispatcher) {
        resultOf { contentDataSource.takePersistableReadPermission(uri.toUri()) }
    }

    override suspend fun releaseAccess(uri: String): AppResult<Unit> = withContext(ioDispatcher) {
        resultOf { contentDataSource.releasePersistableReadPermission(uri.toUri()) }
    }
}
