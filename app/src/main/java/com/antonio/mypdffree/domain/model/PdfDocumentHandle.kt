package com.antonio.mypdffree.domain.model

import com.antonio.mypdffree.domain.model.result.AppResult

/**
 * Documento ABIERTO: mantiene un descriptor de archivo y el renderer nativo mientras se lee.
 *
 * Es un recurso, no un dato; por eso es [AutoCloseable] y su dueño (el ViewModel del lector) lo cierra
 * al destruirse (`addCloseable`). Se modela como objeto devuelto por el repositorio, y no como estado
 * escondido en un repositorio con scope, para que el ciclo de vida sea explicito y no haga falta
 * ningun scope de Hilt.
 */
interface PdfDocumentHandle : AutoCloseable {
    val info: PdfDocumentInfo

    /**
     * Pinta la pagina [pageIndex] (base 0) a [targetWidthPx] de ancho conservando la proporcion,
     * y reduce la escala si la imagen superase [maxPixels] (presupuesto de memoria).
     */
    suspend fun renderPage(pageIndex: Int, targetWidthPx: Int, maxPixels: Int): AppResult<PageBitmap>
}
