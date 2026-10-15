package com.antonio.mypdffree.domain.error

/**
 * Los errores de dominio son VALORES, no excepciones: viajan dentro de `AppResult` hasta la UI.
 * Cada variante nueva obliga (por el `when` exhaustivo de `toUiText()`) a darle un texto al usuario.
 */
sealed interface DomainError {

    /** Fallos al abrir o pintar un PDF. */
    sealed interface Pdf : DomainError {
        /** El archivo ya no existe (borrado, movido, tarjeta SD retirada). */
        data object NotFound : Pdf
        /** El permiso de lectura sobre el URI se perdio o nunca se concedio. */
        data object AccessDenied : Pdf
        /** PdfRenderer no abre PDFs cifrados con contraseña. */
        data object PasswordProtected : Pdf
        /** El archivo no es un PDF valido o esta dañado. */
        data object Corrupted : Pdf
        /** Una pagina concreta no se pudo pintar (memoria insuficiente, documento ya cerrado...). */
        data object RenderFailed : Pdf
    }

    sealed interface Local : DomainError {
        /** Room: lectura o escritura del historial fallida. */
        data object Storage : Local
    }

    /** Validacion de entradas del usuario; se muestran inline, junto al campo. */
    sealed interface Validation : DomainError {
        data object PageNumberEmpty : Validation
        data object PageNumberNotANumber : Validation
        data class PageNumberOutOfRange(val pageCount: Int) : Validation
    }

    data class Unknown(val message: String?, val cause: Throwable? = null) : DomainError
}
