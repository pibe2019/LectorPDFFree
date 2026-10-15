package com.antonio.mypdffree

import android.content.ContentResolver
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.antonio.mypdffree.ui.core.navigation.NavigationWrapper
import com.antonio.mypdffree.ui.theme.MyPdfFreeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash del sistema (API SplashScreen, retrocompatible a API 23). Antes de super.onCreate.
        // No hace falta una pantalla Splash en Compose: no hay sesion ni datos que esperar.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Solo en la primera creacion: tras rotar o tras muerte de proceso, Navigation 3 restaura su
        // back stack (que ya incluye el lector) y releer el intent apilaria el PDF por duplicado.
        val initialDocumentUri = if (savedInstanceState == null) intent.pdfUriOrNull() else null

        setContent {
            MyPdfFreeTheme {
                NavigationWrapper(initialDocumentUri = initialDocumentUri, onFinish = ::finish)
            }
        }
    }

    /**
     * "Abrir con MyPDFFree" desde otra app (gestor de archivos, Gmail, WhatsApp...).
     * Solo `content://`: un `file://` exigiria permiso de almacenamiento y desde Android 7 otras apps
     * ya no pueden compartirlos (FileUriExposedException).
     */
    private fun Intent.pdfUriOrNull(): String? =
        data?.takeIf { action == Intent.ACTION_VIEW && it.scheme == ContentResolver.SCHEME_CONTENT }?.toString()
}
