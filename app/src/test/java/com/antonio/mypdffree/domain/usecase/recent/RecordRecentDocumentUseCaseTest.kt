package com.antonio.mypdffree.domain.usecase.recent

import com.antonio.mypdffree.domain.error.DomainError
import com.antonio.mypdffree.domain.model.RecentDocument
import com.antonio.mypdffree.domain.model.result.DomainResult
import com.antonio.mypdffree.domain.repository.DocumentAccessRepositoryInterface
import com.antonio.mypdffree.domain.repository.RecentDocumentRepositoryInterface
import com.antonio.mypdffree.util.TEST_URI
import com.antonio.mypdffree.util.pdfInfo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RecordRecentDocumentUseCaseTest {

    private val accessRepository: DocumentAccessRepositoryInterface = mockk(relaxed = true)
    private val recentRepository: RecentDocumentRepositoryInterface = mockk()
    private val record = RecordRecentDocumentUseCase(accessRepository, recentRepository) { NOW }

    @Test
    fun `si se retiene el acceso guarda la entrada con la hora actual`() = runTest {
        coEvery { accessRepository.retainAccess(TEST_URI) } returns DomainResult.Success(true)
        coEvery { recentRepository.saveRecent(any()) } returns DomainResult.Success(Unit)
        coEvery { recentRepository.trimTo(any()) } returns DomainResult.Success(emptyList())

        val result = record(pdfInfo(pageCount = 10), lastPageIndex = 3)

        assertEquals(DomainResult.Success(true), result)
        coVerify(exactly = 1) {
            recentRepository.saveRecent(
                RecentDocument(TEST_URI, "Contrato.pdf", 1_024L, pageCount = 10, lastPageIndex = 3, lastOpenedAtMillis = NOW)
            )
        }
        coVerify { recentRepository.trimTo(RecordRecentDocumentUseCase.MAX_RECENT_DOCUMENTS) }
    }

    @Test
    fun `si el origen no permite retener el acceso no se registra`() = runTest {
        coEvery { accessRepository.retainAccess(TEST_URI) } returns DomainResult.Success(false)

        assertEquals(DomainResult.Success(false), record(pdfInfo(), lastPageIndex = 0))

        coVerify(exactly = 0) { recentRepository.saveRecent(any()) }
    }

    @Test
    fun `libera el permiso de las entradas descartadas al recortar`() = runTest {
        coEvery { accessRepository.retainAccess(TEST_URI) } returns DomainResult.Success(true)
        coEvery { recentRepository.saveRecent(any()) } returns DomainResult.Success(Unit)
        coEvery { recentRepository.trimTo(any()) } returns DomainResult.Success(listOf("content://viejo1", "content://viejo2"))

        record(pdfInfo(), lastPageIndex = 0)

        coVerify(exactly = 1) { accessRepository.releaseAccess("content://viejo1") }
        coVerify(exactly = 1) { accessRepository.releaseAccess("content://viejo2") }
    }

    @Test
    fun `si Room falla al guardar devuelve el error y no recorta`() = runTest {
        coEvery { accessRepository.retainAccess(TEST_URI) } returns DomainResult.Success(true)
        coEvery { recentRepository.saveRecent(any()) } returns DomainResult.Failure(DomainError.Local.Storage)

        assertEquals(DomainResult.Failure(DomainError.Local.Storage), record(pdfInfo(), lastPageIndex = 0))

        coVerify(exactly = 0) { recentRepository.trimTo(any()) }
    }

    private companion object {
        const val NOW = 1_700_000_000_000L
    }
}
