package com.antonio.mypdffree.domain.usecase.reader

import com.antonio.mypdffree.domain.error.DomainError
import com.antonio.mypdffree.domain.model.PageBitmap
import com.antonio.mypdffree.domain.model.PdfDocumentHandle
import com.antonio.mypdffree.domain.model.result.AppResult
import com.antonio.mypdffree.domain.model.result.DomainResult
import javax.inject.Inject

/**
 * Pinta una pagina aplicando la politica de memoria de la app.
 *
 * Un PDF puede declarar paginas enormes (planos, carteles): pintarlas al ancho pedido sin limite
 * provoca OutOfMemoryError o el "Canvas: trying to draw too large bitmap". Los limites viven aqui,
 * en dominio, porque son una decision de producto (nitidez vs. estabilidad) y asi son testeables.
 */
class RenderPdfPageUseCase @Inject constructor() {

    suspend operator fun invoke(
        handle: PdfDocumentHandle,
        pageIndex: Int,
        requestedWidthPx: Int,
    ): AppResult<PageBitmap> {
        if (pageIndex !in 0 until handle.info.pageCount || requestedWidthPx <= 0) {
            return DomainResult.Failure(DomainError.Pdf.RenderFailed)
        }
        return handle.renderPage(
            pageIndex = pageIndex,
            targetWidthPx = requestedWidthPx.coerceAtMost(MAX_RENDER_WIDTH_PX),
            maxPixels = MAX_RENDER_PIXELS,
        )
    }

    companion object {
        /** Tamaño de textura que soporta cualquier GPU de gama baja. */
        const val MAX_RENDER_WIDTH_PX = 4096
        /** 8 MP en ARGB_8888 = 32 MB por pagina: nitido con zoom x2 y seguro en gama baja. */
        const val MAX_RENDER_PIXELS = 8_000_000
    }
}
