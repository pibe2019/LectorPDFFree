package com.antonio.mypdffree

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.doubleClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Recorrido visual para revisar la UI a ojo (no verifica nada). Excluido de Gradle (notClass en
 * build.gradle.kts); se lanza con `./gradlew installDebug installDebugAndroidTest` y luego
 * `adb shell am instrument -w [-e suffix -oscuro] -e class com.antonio.mypdffree.ScreenshotTourTest
 *  com.antonio.mypdffree.test/androidx.test.runner.AndroidJUnitRunner`
 * Las PNG quedan en /sdcard/Android/data/com.antonio.mypdffree/files/screenshots/.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 29)
class ScreenshotTourTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val outputDir by lazy { File(context.getExternalFilesDir(null), "screenshots").apply { mkdirs() } }
    private var pdfUri: Uri? = null

    @After
    fun tearDown() {
        pdfUri?.let { context.contentResolver.delete(it, null, null) }
    }

    @Test
    fun recorrido() {
        val suffix = InstrumentationRegistry.getArguments().getString("suffix").orEmpty()

        ActivityScenario.launch(MainActivity::class.java).use {
            composeRule.waitUntilAtLeastOneExists(hasText("MyPDFFree"), 10_000)
            capture("01-home$suffix")
        }

        val uri = createPdf().also { pdfUri = it }
        val intent = Intent(Intent.ACTION_VIEW, uri, context, MainActivity::class.java)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        ActivityScenario.launch<MainActivity>(intent).use {
            composeRule.waitUntilAtLeastOneExists(hasText("1 / 6"), 10_000)
            composeRule.waitForIdle()
            Thread.sleep(1_500) // deja terminar el pintado de las paginas
            capture("02-lector$suffix")

            composeRule.onRoot().performTouchInput { doubleClick(center) }
            composeRule.waitForIdle()
            Thread.sleep(1_500)
            capture("03-lector-zoom$suffix")
            composeRule.onRoot().performTouchInput { doubleClick(center) }

            composeRule.waitForIdle()
            composeRule.onNodeWithText("1 / 6").performClick()
            composeRule.waitForIdle()
            // El dialogo es otra ventana: captureToImage de la raiz no lo incluye.
            capture("04-ir-a-pagina$suffix", composeRule.onNode(isDialog()))
            composeRule.onNodeWithText("Cancelar").performClick()

            // Abierto via "Abrir con" sin permiso persistible: por regla de negocio NO entra en recientes.
            composeRule.onNodeWithContentDescription("Volver").performClick()
            composeRule.waitUntilAtLeastOneExists(hasText("Aún no has abierto ningún PDF"), 10_000)
        }
    }

    private fun capture(name: String, node: SemanticsNodeInteraction = composeRule.onRoot()) {
        val bitmap = node.captureToImage().asAndroidBitmap()
        File(outputDir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun createPdf(): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, "Manual de usuario.pdf")
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
        }
        val uri = requireNotNull(context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
        val document = PdfDocument()
        try {
            repeat(6) { index ->
                val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, index + 1).create())
                val canvas = page.canvas
                canvas.drawText("Capitulo ${index + 1}", 60f, 90f, Paint().apply { textSize = 36f; isFakeBoldText = true })
                val body = Paint().apply { textSize = 13f }
                repeat(40) { line -> canvas.drawText("Linea ${line + 1}: texto de ejemplo para comprobar la nitidez del zoom.", 60f, 140f + line * 17, body) }
                canvas.drawRect(60f, 830f - 120f, 535f, 830f - 20f, Paint().apply { color = 0xFF3755C3.toInt() })
                document.finishPage(page)
            }
            requireNotNull(context.contentResolver.openOutputStream(uri)).use(document::writeTo)
        } finally {
            document.close()
        }
        return uri
    }
}
