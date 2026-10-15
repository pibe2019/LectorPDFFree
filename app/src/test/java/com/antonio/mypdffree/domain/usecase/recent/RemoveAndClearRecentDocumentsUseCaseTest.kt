package com.antonio.mypdffree.domain.usecase.recent

import com.antonio.mypdffree.domain.error.DomainError
import com.antonio.mypdffree.domain.model.result.DomainResult
import com.antonio.mypdffree.domain.repository.DocumentAccessRepositoryInterface
import com.antonio.mypdffree.domain.repository.RecentDocumentRepositoryInterface
import com.antonio.mypdffree.util.TEST_URI
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RemoveAndClearRecentDocumentsUseCaseTest {

    private val recentRepository: RecentDocumentRepositoryInterface = mockk()
    private val accessRepository: DocumentAccessRepositoryInterface = mockk(relaxed = true)

    @Test
    fun `quitar borra la fila y despues libera el permiso`() = runTest {
        coEvery { recentRepository.removeRecent(TEST_URI) } returns DomainResult.Success(Unit)

        assertEquals(DomainResult.Success(Unit), RemoveRecentDocumentUseCase(recentRepository, accessRepository)(TEST_URI))

        coVerifyOrder {
            recentRepository.removeRecent(TEST_URI)
            accessRepository.releaseAccess(TEST_URI)
        }
    }

    @Test
    fun `si no se pudo borrar la fila el permiso se conserva`() = runTest {
        coEvery { recentRepository.removeRecent(TEST_URI) } returns DomainResult.Failure(DomainError.Local.Storage)

        val result = RemoveRecentDocumentUseCase(recentRepository, accessRepository)(TEST_URI)

        assertEquals(DomainResult.Failure(DomainError.Local.Storage), result)
        coVerify(exactly = 0) { accessRepository.releaseAccess(any()) }
    }

    @Test
    fun `un fallo al liberar el permiso no convierte el borrado en error`() = runTest {
        coEvery { recentRepository.removeRecent(TEST_URI) } returns DomainResult.Success(Unit)
        coEvery { accessRepository.releaseAccess(TEST_URI) } returns DomainResult.Failure(DomainError.Pdf.AccessDenied)

        assertEquals(DomainResult.Success(Unit), RemoveRecentDocumentUseCase(recentRepository, accessRepository)(TEST_URI))
    }

    @Test
    fun `vaciar el historial libera el permiso de cada documento borrado`() = runTest {
        coEvery { recentRepository.removeAll() } returns DomainResult.Success(listOf("content://a", "content://b"))

        assertEquals(DomainResult.Success(Unit), ClearRecentDocumentsUseCase(recentRepository, accessRepository)())

        coVerify(exactly = 1) { accessRepository.releaseAccess("content://a") }
        coVerify(exactly = 1) { accessRepository.releaseAccess("content://b") }
    }
}
