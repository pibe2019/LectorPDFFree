package com.antonio.mypdffree.ui.reader

import app.cash.turbine.test
import com.antonio.mypdffree.domain.error.DomainError
import com.antonio.mypdffree.domain.model.OpenedPdf
import com.antonio.mypdffree.domain.model.PageBitmap
import com.antonio.mypdffree.domain.model.result.DomainResult
import com.antonio.mypdffree.domain.usecase.reader.OpenPdfDocumentUseCase
import com.antonio.mypdffree.domain.usecase.reader.RenderPdfPageUseCase
import com.antonio.mypdffree.domain.usecase.reader.SaveReadingProgressUseCase
import com.antonio.mypdffree.domain.usecase.reader.ValidatePageNumberUseCase
import com.antonio.mypdffree.domain.usecase.recent.RemoveRecentDocumentUseCase
import com.antonio.mypdffree.util.MainDispatcherRule
import com.antonio.mypdffree.util.TEST_URI
import com.antonio.mypdffree.util.fakeHandle
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val openPdfDocument: OpenPdfDocumentUseCase = mockk()
    private val renderPdfPage: RenderPdfPageUseCase = mockk()
    private val saveReadingProgress: SaveReadingProgressUseCase = mockk()
    private val removeRecentDocument: RemoveRecentDocumentUseCase = mockk()
    private val handle = fakeHandle()

    private fun buildViewModel() = ReaderViewModel(
        documentUri = TEST_URI,
        openPdfDocument = openPdfDocument,
        renderPdfPage = renderPdfPage,
        saveReadingProgress = saveReadingProgress,
        validatePageNumber = ValidatePageNumberUseCase(), // real: es logica pura y es parte del comportamiento
        removeRecentDocument = removeRecentDocument,
    )

    private fun runVmTest(block: suspend TestScope.() -> Unit) = runTest(mainDispatcherRule.testDispatcher.scheduler) { block() }

    @Test
    fun `al observar abre el documento y retoma la pagina guardada`() = runVmTest {
        coEvery { openPdfDocument(TEST_URI) } returns DomainResult.Success(OpenedPdf(handle, initialPageIndex = 4))
        val vm = buildViewModel()

        vm.uiState.test {
            assertEquals(ReaderContent.Loading, awaitItem().content)
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertEquals(ReaderContent.Ready(handle.info, initialPageIndex = 4), state.content)
            assertEquals(4, state.currentPageIndex)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 1) { openPdfDocument(TEST_URI) }
    }

    @Test
    fun `si no se puede abrir muestra el error y ofrece quitarlo de recientes si ya no existe`() = runVmTest {
        coEvery { openPdfDocument(TEST_URI) } returns DomainResult.Failure(DomainError.Pdf.NotFound)
        coEvery { removeRecentDocument(TEST_URI) } returns DomainResult.Success(Unit)
        val vm = buildViewModel()

        vm.uiState.test {
            awaitItem()
            advanceUntilIdle()
            val failed = expectMostRecentItem().content as ReaderContent.Failed
            assertEquals(DomainError.Pdf.NotFound, failed.error)
            assertTrue(failed.canRemoveFromRecents)

            vm.onEvent(ReaderEvent.OnRemoveFromRecentsClick)
            advanceUntilIdle()
            assertTrue(expectMostRecentItem().navigateBack)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `un PDF dañado no ofrece quitarlo de recientes`() = runVmTest {
        coEvery { openPdfDocument(TEST_URI) } returns DomainResult.Failure(DomainError.Pdf.Corrupted)
        val vm = buildViewModel()

        vm.uiState.test {
            awaitItem()
            advanceUntilIdle()
            assertFalse((expectMostRecentItem().content as ReaderContent.Failed).canRemoveFromRecents)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ir a una pagina fuera de rango muestra el error en el campo y no desplaza`() = runVmTest {
        coEvery { openPdfDocument(TEST_URI) } returns DomainResult.Success(OpenedPdf(handle, initialPageIndex = 0))
        val vm = buildViewModel()

        vm.uiState.test {
            awaitItem()
            advanceUntilIdle()
            vm.onEvent(ReaderEvent.OnGoToPageClick)
            vm.onEvent(ReaderEvent.OnGoToPageInputChange("99"))
            vm.onEvent(ReaderEvent.OnGoToPageConfirm)
            advanceUntilIdle()

            val state = expectMostRecentItem()
            assertEquals(DomainError.Validation.PageNumberOutOfRange(pageCount = 10), state.goToPageError)
            assertTrue(state.isGoToPageDialogVisible)
            assertNull(state.pendingScrollToPage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ir a una pagina valida cierra el dialogo y pide el scroll al indice base cero`() = runVmTest {
        coEvery { openPdfDocument(TEST_URI) } returns DomainResult.Success(OpenedPdf(handle, initialPageIndex = 0))
        val vm = buildViewModel()

        vm.uiState.test {
            awaitItem()
            advanceUntilIdle()
            vm.onEvent(ReaderEvent.OnGoToPageClick)
            vm.onEvent(ReaderEvent.OnGoToPageInputChange("3a")) // la letra se filtra al escribir
            vm.onEvent(ReaderEvent.OnGoToPageConfirm)
            advanceUntilIdle()

            val state = expectMostRecentItem()
            assertFalse(state.isGoToPageDialogVisible)
            assertEquals(2, state.pendingScrollToPage)

            vm.onEvent(ReaderEvent.OnScrollHandled)
            advanceUntilIdle()
            assertNull(expectMostRecentItem().pendingScrollToPage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `cambiar de pagina guarda el progreso`() = runVmTest {
        coEvery { openPdfDocument(TEST_URI) } returns DomainResult.Success(OpenedPdf(handle, initialPageIndex = 0))
        coEvery { saveReadingProgress(any(), any()) } returns DomainResult.Success(Unit)
        val vm = buildViewModel()

        vm.uiState.test {
            awaitItem()
            advanceUntilIdle()
            vm.onEvent(ReaderEvent.OnPageChanged(5))
            advanceUntilIdle()
            assertEquals(5, expectMostRecentItem().currentPageIndex)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 1) { saveReadingProgress(TEST_URI, 5) }
    }

    @Test
    fun `loadPage delega en el use case con el documento abierto`() = runVmTest {
        val page: PageBitmap = mockk()
        coEvery { openPdfDocument(TEST_URI) } returns DomainResult.Success(OpenedPdf(handle, initialPageIndex = 0))
        coEvery { renderPdfPage(handle, 2, 1080) } returns DomainResult.Success(page)
        val vm = buildViewModel()

        vm.uiState.test {
            awaitItem()
            advanceUntilIdle()
            assertEquals(DomainResult.Success(page), vm.loadPage(pageIndex = 2, widthPx = 1080))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadPage antes de abrir el documento falla sin lanzar`() = runVmTest {
        coEvery { openPdfDocument(TEST_URI) } returns DomainResult.Failure(DomainError.Pdf.Corrupted)

        assertEquals(DomainResult.Failure(DomainError.Pdf.RenderFailed), buildViewModel().loadPage(0, 1080))
    }
}
