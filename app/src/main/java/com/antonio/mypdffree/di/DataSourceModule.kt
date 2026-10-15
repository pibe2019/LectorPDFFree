package com.antonio.mypdffree.di

import com.antonio.mypdffree.data.datasource.device.DocumentContentDataSource
import com.antonio.mypdffree.data.datasource.device.DocumentContentDataSourceInterface
import com.antonio.mypdffree.data.datasource.local.LocalRecentDocumentDataSource
import com.antonio.mypdffree.data.datasource.local.LocalRecentDocumentDataSourceInterface
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Data sources sin scope: son envoltorios baratos sobre recursos que ya son singleton (Room, ContentResolver). */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataSourceModule {

    @Binds
    abstract fun bindLocalRecentDocumentDataSource(impl: LocalRecentDocumentDataSource): LocalRecentDocumentDataSourceInterface

    @Binds
    abstract fun bindDocumentContentDataSource(impl: DocumentContentDataSource): DocumentContentDataSourceInterface
}
