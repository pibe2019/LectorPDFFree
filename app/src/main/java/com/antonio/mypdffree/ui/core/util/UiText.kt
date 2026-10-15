package com.antonio.mypdffree.ui.core.util

import androidx.annotation.StringRes
import com.antonio.mypdffree.domain.error.DomainError

/**
 * Texto que la UI resuelve en composicion. El ViewModel nunca toca `Context` ni strings ya
 * traducidos: guarda la INTENCION (recurso + argumentos o un error) y Compose la resuelve.
 */
sealed class UiText {
    sealed class Resolved : UiText()
    data class DynamicString(val value: String) : Resolved()
    data class StringResource(@StringRes val resId: Int, val args: List<Any> = emptyList()) : Resolved()
    data class FromError(val error: DomainError) : UiText()
}
