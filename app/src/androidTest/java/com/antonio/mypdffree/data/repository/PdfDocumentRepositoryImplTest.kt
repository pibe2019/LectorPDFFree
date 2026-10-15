package com.antonio.mypdffree.data.repository

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.antonio.mypdffree.data.datasource.device.DocumentContentDataSource
import com.antonio.mypdffree.data.pdf.AndroidPageBitmap
import com.antonio.mypdffree.domain.error.DomainError
import com.antonio.mypdffree.domain.model.PdfDocumentHandle
import com.antonio.mypdffree.domain.model.result.DomainResult
import com.antonio.mypdffree.domain.model.result.getOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Integracion real con PdfRenderer: genera PDFs con la API del sistema (PdfDocument) y los abre por la
 * misma ruta que la app. Con file:// porque ContentResolver lo soporta y no hace falta un provider de test.
 */
@RunWith(AndroidJUnit4::class)
class PdfDocumentRepositoryImplTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val repository = PdfDocumentRepositoryImpl(
        contentDataSource = DocumentContentDataSource(context.contentResolver),
        ioDispatcher = Dispatchers.IO,
        renderDispatcher = Dispatchers.Default,
    )
    private val opened = mutableListOf<PdfDocumentHandle>()
    private lateinit var directory: File

    @Before
    fun setUp() {
        directory = File(context.cacheDir, "pdf-tests").apply { mkdirs() }
    }

    @After
    fun tearDown() {
        opened.forEach { it.close() }
        directory.deleteRecursively()
    }

    @Test
    fun abre_un_pdf_valido_y_reporta_sus_paginas() = runTest {
        val uri = createPdf("tres.pdf", pages = 3)

        val handle = open(uri)

        assertEquals(3, handle.info.pageCount)
        assertEquals(uri.toString(), handle.info.uri)
    }

    @Test
    fun pinta_la_pagina_al_ancho_pedido_y_con_la_proporcion_del_pdf() = runTest {
        val handle = open(createPdf("a4.pdf", pages = 1, widthPt = 595, heightPt = 842))

        val page = handle.renderPage(pageIndex = 0, targetWidthPx = 1_000, maxPixels = 10_000_000).getOrNull() as AndroidPageBitmap

        assertEquals(1_000, page.widthPx)
        assertEquals(1_415, page.heightPx) // 842 * (1000 / 595)
    }

    @Test
    fun respeta_el_presupuesto_de_pixeles_en_paginas_enormes() = runTest {
        val handle = open(createPdf("tira.pdf", pages = 1, widthPt = 200, heightPt = 4_000))

        val page = handle.renderPage(pageIndex = 0, targetWidthPx = 1_000, maxPixels = 1_000_000).getOrNull()!!

        assertTrue(page.widthPx.toLong() * page.heightPx <= 1_000_000)
        assertEquals(20f, page.heightPx / page.widthPx.toFloat(), 0.2f) // la proporcion se conserva
    }

    @Test
    fun un_archivo_que_no_es_pdf_es_Corrupted() = runTest {
        val notPdf = File(directory, "falso.pdf").apply { writeText("esto no es un pdf") }

        assertEquals(DomainResult.Failure(DomainError.Pdf.Corrupted), repository.open(Uri.fromFile(notPdf).toString()))
    }

    @Test
    fun un_archivo_inexistente_es_NotFound() = runTest {
        val missing = Uri.fromFile(File(directory, "no-existe.pdf")).toString()

        assertEquals(DomainResult.Failure(DomainError.Pdf.NotFound), repository.open(missing))
    }

    @Test
    fun tras_cerrar_pintar_falla_sin_lanzar_y_cerrar_dos_veces_es_seguro() = runTest {
        val handle = open(createPdf("cerrar.pdf", pages = 1))

        handle.close()
        handle.close()

        assertEquals(DomainResult.Failure(DomainError.Pdf.RenderFailed), handle.renderPage(0, 500, 10_000_000))
    }

    private suspend fun open(uri: Uri): PdfDocumentHandle {
        val result = repository.open(uri.toString())
        val handle = result.getOrNull() ?: error("No se pudo abrir: $result")
        opened += handle
        return handle
    }

    private fun createPdf(name: String, pages: Int, widthPt: Int = 595, heightPt: Int = 842): Uri {
        val file = File(directory, name)
        val document = PdfDocument()
        try {
            repeat(pages) { index ->
                val page = document.startPage(PdfDocument.PageInfo.Builder(widthPt, heightPt, index + 1).create())
                page.canvas.drawCircle(widthPt / 2f, heightPt / 2f, widthPt / 4f, android.graphics.Paint())
                document.finishPage(page)
            }
            file.outputStream().use(document::writeTo)
        } finally {
            document.close()
        }
        return Uri.fromFile(file)
    }
}
