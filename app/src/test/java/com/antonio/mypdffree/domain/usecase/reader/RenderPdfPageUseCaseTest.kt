package com.antonio.mypdffree.domain.usecase.reader

import com.antonio.mypdffree.domain.error.DomainError
import com.antonio.mypdffree.domain.model.PageBitmap
import com.antonio.mypdffree.domain.model.result.DomainResult
import com.antonio.mypdffree.util.fakeHandle
import com.antonio.mypdffree.util.pdfInfo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RenderPdfPageUseCaseTest {

    private val render = RenderPdfPageUseCase()
    private val handle = fakeHandle(pdfInfo(pageCount = 3))
    private val page: PageBitmap = mockk()

    @Test
    fun `pide la pagina con el ancho solicitado y el presupuesto de pixeles`() = runTest {
        coEvery { handle.renderPage(1, 1080, any()) } returns DomainResult.Success(page)

        assertEquals(DomainResult.Success(page), render(handle, pageIndex = 1, requestedWidthPx = 1080))

        coVerify(exactly = 1) { handle.renderPage(1, 1080, RenderPdfPageUseCase.MAX_RENDER_PIXELS) }
    }

    @Test
    fun `un ancho excesivo se recorta al maximo permitido`() = runTest {
        coEvery { handle.renderPage(any(), any(), any()) } returns DomainResult.Success(page)

        render(handle, pageIndex = 0, requestedWidthPx = 20_000)

        coVerify { handle.renderPage(0, RenderPdfPageUseCase.MAX_RENDER_WIDTH_PX, any()) }
    }

    @Test
    fun `indice fuera de rango o ancho invalido falla sin tocar el renderer`() = runTest {
        val expected = DomainResult.Failure(DomainError.Pdf.RenderFailed)
        assertEquals(expected, render(handle, pageIndex = 3, requestedWidthPx = 1080))
        assertEquals(expected, render(handle, pageIndex = -1, requestedWidthPx = 1080))
        assertEquals(expected, render(handle, pageIndex = 0, requestedWidthPx = 0))

        coVerify(exactly = 0) { handle.renderPage(any(), any(), any()) }
    }
}
