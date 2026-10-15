package com.antonio.mypdffree.di

import com.antonio.mypdffree.data.repository.DocumentAccessRepositoryImpl
import com.antonio.mypdffree.data.repository.PdfDocumentRepositoryImpl
import com.antonio.mypdffree.data.repository.RecentDocumentRepositoryImpl
import com.antonio.mypdffree.data.time.SystemTimeProvider
import com.antonio.mypdffree.domain.repository.DocumentAccessRepositoryInterface
import com.antonio.mypdffree.domain.repository.PdfDocumentRepositoryInterface
import com.antonio.mypdffree.domain.repository.RecentDocumentRepositoryInterface
import com.antonio.mypdffree.domain.util.TimeProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Todos SIN scope (lo recomendado por la doc de Hilt): ninguno guarda estado compartido. El unico
 * recurso con estado, el documento abierto, es un `PdfDocumentHandle` cuyo dueño es el ViewModel.
 * Si algun repositorio llega a mantener estado (shareIn, cache, callback), su @Binds pasa a @Singleton.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds abstract fun bindPdfDocumentRepository(impl: PdfDocumentRepositoryImpl): PdfDocumentRepositoryInterface

    @Binds abstract fun bindDocumentAccessRepository(impl: DocumentAccessRepositoryImpl): DocumentAccessRepositoryInterface

    @Binds abstract fun bindRecentDocumentRepository(impl: RecentDocumentRepositoryImpl): RecentDocumentRepositoryInterface

    @Binds abstract fun bindTimeProvider(impl: SystemTimeProvider): TimeProvider
}
