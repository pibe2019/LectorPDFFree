package com.antonio.mypdffree.data.pdf

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.core.graphics.createBitmap
import com.antonio.mypdffree.data.util.resultOf
import com.antonio.mypdffree.domain.error.DomainError
import com.antonio.mypdffree.domain.model.PageBitmap
import com.antonio.mypdffree.domain.model.PdfDocumentHandle
import com.antonio.mypdffree.domain.model.PdfDocumentInfo
import com.antonio.mypdffree.domain.model.result.AppResult
import com.antonio.mypdffree.domain.model.result.DomainResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Envoltorio sobre [PdfRenderer] (API del sistema, sin licencias ni dependencias).
 *
 * Restricciones de PdfRenderer que dictan este diseño:
 * 1. NO es thread-safe y solo admite UNA pagina abierta a la vez -> todo acceso pasa por [mutex].
 * 2. Cerrarlo con una pagina abierta lanza IllegalStateException -> [close] nunca cierra mientras
 *    se pinta: marca [closeRequested] y quien suelte el mutex libera los recursos (ver [releaseIfClosed]).
 */
internal class AndroidPdfDocumentHandle(
    override val info: PdfDocumentInfo,
    private val descriptor: ParcelFileDescriptor,
    private val renderer: PdfRenderer,
    private val renderDispatcher: CoroutineDispatcher,
    private val cache: PageBitmapCache = PageBitmapCache(),
) : PdfDocumentHandle {

    private val mutex = Mutex()
    private val closeRequested = AtomicBoolean(false)
    private val released = AtomicBoolean(false)

    override suspend fun renderPage(pageIndex: Int, targetWidthPx: Int, maxPixels: Int): AppResult<PageBitmap> =
        try {
            withContext(renderDispatcher) {
                mutex.withLock {
                    if (closeRequested.get()) {
                        DomainResult.Failure(DomainError.Pdf.RenderFailed)
                    } else {
                        resultOf {
                            cache.get(pageIndex, targetWidthPx)
                                ?: renderLocked(pageIndex, targetWidthPx, maxPixels).also { cache.put(targetWidthPx, it) }
                        }
                    }
                }
            }
        } finally {
            // En finally: si la coroutine se cancela (el usuario hizo scroll rapido), igual hay que
            // comprobar si un close() llego mientras teniamos el mutex.
            releaseIfClosed()
        }

    private fun renderLocked(pageIndex: Int, targetWidthPx: Int, maxPixels: Int): AndroidPageBitmap =
        renderer.openPage(pageIndex).use { page ->
            // page.width/height vienen en puntos (1/72"); se escala para que el ancho sea el pedido.
            var width = targetWidthPx
            var height = (page.height * (targetWidthPx / page.width.toFloat())).roundToInt()
            val pixels = width.toLong() * height
            if (pixels > maxPixels) {
                // Reduccion uniforme: mantiene la proporcion y deja el area justo en el presupuesto.
                val factor = sqrt(maxPixels / pixels.toDouble())
                width = (width * factor).toInt()
                height = (height * factor).toInt()
            }
            val bitmap = createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
            // Muchos PDFs no pintan fondo (es transparente): sin esto se verian negros en tema oscuro.
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            AndroidPageBitmap(pageIndex, bitmap)
        }

    override fun close() {
        closeRequested.set(true)
        releaseIfClosed()
    }

    /**
     * Libera solo si nadie esta pintando. Si [close] encuentra el mutex ocupado, el render en curso
     * lo vera al terminar (su finally llama aqui) y liberara el. [released] evita la doble liberacion.
     */
    private fun releaseIfClosed() {
        if (!closeRequested.get() || !mutex.tryLock()) return
        try {
            if (released.compareAndSet(false, true)) {
                cache.clear()
                renderer.close()
                descriptor.close() // idempotente; PdfRenderer ya lo cierra, pero no lo garantiza en todas las versiones
            }
        } finally {
            mutex.unlock()
        }
    }
}
