package com.antonio.mypdffree

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pila completa y real: MainActivity + Hilt + Navigation 3 + PdfRenderer + Room, entrando por el
 * mismo camino que "Abrir con MyPDFFree" desde otra app (ACTION_VIEW con un content://).
 * El PDF se crea en MediaStore.Downloads (API 29+) para obtener un content:// genuino.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 29)
class ReaderEndToEndTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private var pdfUri: Uri? = null
    private var scenario: ActivityScenario<MainActivity>? = null

    @After
    fun tearDown() {
        scenario?.close()
        pdfUri?.let { context.contentResolver.delete(it, null, null) }
    }

    @Test
    fun abrir_con_muestra_el_documento_y_permite_ir_a_una_pagina() {
        val uri = createPdfInDownloads(pages = 5).also { pdfUri = it }
        val intent = Intent(Intent.ACTION_VIEW, uri, context, MainActivity::class.java)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        scenario = ActivityScenario.launch(intent)

        // El lector se abre en la pagina 1 de 5 y el titulo es el nombre del archivo.
        composeRule.waitUntilAtLeastOneExists(hasText("1 / 5"), timeoutMillis = 10_000)
        composeRule.onNodeWithText(FILE_NAME).assertIsDisplayed()

        // "Ir a pagina" desde el indicador: valida y desplaza.
        composeRule.onNodeWithText("1 / 5").performClick()
        composeRule.onNode(hasSetTextAction()).performTextReplacement("4")
        composeRule.onNodeWithText("Ir").performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("4 / 5"), timeoutMillis = 10_000)

        // Volver lleva al Home (no cierra la app): el lector se apilo sobre el historial.
        composeRule.onNodeWithContentDescription("Volver").performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("MyPDFFree"), timeoutMillis = 5_000)
    }

    private fun createPdfInDownloads(pages: Int): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, FILE_NAME)
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
        }
        val resolver = context.contentResolver
        val uri = requireNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
        val document = PdfDocument()
        try {
            repeat(pages) { index ->
                val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, index + 1).create())
                page.canvas.drawText("Pagina ${index + 1}", 100f, 100f, Paint().apply { textSize = 40f })
                document.finishPage(page)
            }
            requireNotNull(resolver.openOutputStream(uri)).use(document::writeTo)
        } finally {
            document.close()
        }
        return uri
    }

    private companion object {
        const val FILE_NAME = "prueba-e2e.pdf"
    }
}
