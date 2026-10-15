package com.antonio.mypdffree.ui.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Unica definicion de rutas. @Serializable: Navigation 3 guarda el back stack en el estado de la
 * Activity, asi que tras una muerte de proceso el usuario vuelve al mismo PDF.
 */
@Serializable
sealed interface RoutesScreens : NavKey {
    @Serializable
    data object Home : RoutesScreens

    /** [documentUri] viaja como String: un `Uri` no es serializable por kotlinx.serialization. */
    @Serializable
    data class Reader(val documentUri: String) : RoutesScreens
}
