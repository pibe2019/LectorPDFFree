package com.antonio.mypdffree.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = BrandIndigo,
    onPrimary = Color.White,
    primaryContainer = BrandIndigoContainer,
    onPrimaryContainer = OnBrandIndigoContainer,
    secondary = BrandTeal,
    onSecondary = Color.White,
    secondaryContainer = BrandTealContainer,
    onSecondaryContainer = OnBrandTealContainer,
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandIndigoLight,
    onPrimary = OnBrandIndigoDark,
    primaryContainer = BrandIndigoContainerDark,
    onPrimaryContainer = BrandIndigoContainer,
    secondary = BrandTealLight,
    onSecondary = OnBrandTealDark,
    secondaryContainer = BrandTealContainerDark,
    onSecondaryContainer = BrandTealContainer,
)

/**
 * Color dinamico activado por defecto (Material You, Android 12+): en una app de utilidad el usuario
 * espera que encaje con su sistema. La paleta de marca queda como respaldo para Android 8-11.
 */
@Composable
fun MyPdfFreeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
