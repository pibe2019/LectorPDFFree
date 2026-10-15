package com.antonio.mypdffree.ui.modelui

import com.antonio.mypdffree.ui.core.util.UiText

/** Aviso efimero (snackbar). La severidad fija color y duracion en `AppMessageSnackbarHost`. */
sealed interface AppMessage {
    val text: UiText
    data class Error(override val text: UiText) : AppMessage
    data class Info(override val text: UiText) : AppMessage
}
