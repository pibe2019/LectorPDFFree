package com.antonio.mypdffree.domain.usecase.reader

import com.antonio.mypdffree.domain.error.DomainError
import com.antonio.mypdffree.domain.model.OpenedPdf
import com.antonio.mypdffree.domain.model.result.DomainResult
import com.antonio.mypdffree.domain.repository.PdfDocumentRepositoryInterface
import com.antonio.mypdffree.domain.repository.RecentDocumentRepositoryInterface
import com.antonio.mypdffree.domain.usecase.recent.RecordRecentDocumentUseCase
import com.antonio.mypdffree.util.TEST_URI
import com.antonio.mypdffree.util.fakeHandle
import com.antonio.mypdffree.util.pdfInfo
import com.antonio.mypdffree.util.recentDocument
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenPdfDocumentUseCaseTest {

    private val pdfRepository: PdfDocumentRepositoryInterface = mockk()
    private val recentRepository: RecentDocumentRepositoryInterface = mockk()
    private val recordRecent: RecordRecentDocumentUseCase = mockk()
    private val open = OpenPdfDocumentUseCase(pdfRepository, recentRepository, recordRecent)

    @Test
    fun `documento nuevo abre en la primera pagina y se registra`() = runTest {
        val handle = fakeHandle()
        coEvery { pdfRepository.open(TEST_URI) } returns DomainResult.Success(handle)
        coEvery { recentRepository.getRecent(TEST_URI) } returns DomainResult.Success(null)
        coEvery { recordRecent(any(), any()) } returns DomainResult.Success(true)

        val info = handle.info // fuera del bloque verify: dentro contaria como una llamada mas al mock

        assertEquals(DomainResult.Success(OpenedPdf(handle, initialPageIndex = 0)), open(TEST_URI))
        coVerify(exactly = 1) { recordRecent(info, 0) }
    }

    @Test
    fun `documento ya leido retoma la ultima pagina`() = runTest {
        val handle = fakeHandle()
        coEvery { pdfRepository.open(TEST_URI) } returns DomainResult.Success(handle)
        coEvery { recentRepository.getRecent(TEST_URI) } returns DomainResult.Success(recentDocument(lastPageIndex = 7))
        coEvery { recordRecent(any(), any()) } returns DomainResult.Success(true)

        assertEquals(DomainResult.Success(OpenedPdf(handle, initialPageIndex = 7)), open(TEST_URI))
    }

    @Test
    fun `si el archivo ahora tiene menos paginas la pagina guardada se ajusta al rango`() = runTest {
        val handle = fakeHandle(pdfInfo(pageCount = 3))
        coEvery { pdfRepository.open(TEST_URI) } returns DomainResult.Success(handle)
        coEvery { recentRepository.getRecent(TEST_URI) } returns DomainResult.Success(recentDocument(lastPageIndex = 50))
        coEvery { recordRecent(any(), any()) } returns DomainResult.Success(true)

        assertEquals(DomainResult.Success(OpenedPdf(handle, initialPageIndex = 2)), open(TEST_URI))
    }

    @Test
    fun `si abrir falla devuelve el error y no toca el historial`() = runTest {
        coEvery { pdfRepository.open(TEST_URI) } returns DomainResult.Failure(DomainError.Pdf.PasswordProtected)

        assertEquals(DomainResult.Failure(DomainError.Pdf.PasswordProtected), open(TEST_URI))
        coVerify(exactly = 0) { recordRecent(any(), any()) }
    }

    @Test
    fun `un fallo del historial no impide leer el documento`() = runTest {
        val handle = fakeHandle()
        coEvery { pdfRepository.open(TEST_URI) } returns DomainResult.Success(handle)
        coEvery { recentRepository.getRecent(TEST_URI) } returns DomainResult.Failure(DomainError.Local.Storage)
        coEvery { recordRecent(any(), any()) } returns DomainResult.Failure(DomainError.Local.Storage)

        assertEquals(DomainResult.Success(OpenedPdf(handle, initialPageIndex = 0)), open(TEST_URI))
        verify(exactly = 0) { handle.close() }
    }

    @Test
    fun `si cancelan despues de abrir se cierra el handle y se relanza la cancelacion`() = runTest {
        val handle = fakeHandle()
        coEvery { pdfRepository.open(TEST_URI) } returns DomainResult.Success(handle)
        coEvery { recentRepository.getRecent(TEST_URI) } throws CancellationException("el usuario salio")

        val thrown = runCatching { open(TEST_URI) }.exceptionOrNull()

        assertTrue(thrown is CancellationException) // se relanza, no se convierte en Failure
        verify(exactly = 1) { handle.close() }
    }
}
