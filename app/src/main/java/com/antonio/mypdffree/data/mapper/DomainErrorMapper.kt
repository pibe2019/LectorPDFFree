package com.antonio.mypdffree.data.mapper

import android.database.SQLException
import com.antonio.mypdffree.data.error.DomainErrorException
import com.antonio.mypdffree.domain.error.DomainError
import java.io.FileNotFoundException

/** Solo se invoca desde `resultOf { }`. */
fun Throwable.toDomainError(fallback: DomainError? = null): DomainError = when (this) {
    is DomainErrorException   -> error // el sobre ya viene decidido
    is FileNotFoundException  -> DomainError.Pdf.NotFound
    is SecurityException      -> DomainError.Pdf.AccessDenied // permiso del URI revocado o inexistente
    is SQLException           -> DomainError.Local.Storage
    is OutOfMemoryError       -> DomainError.Pdf.RenderFailed // pagina demasiado grande para la memoria libre
    else                      -> fallback ?: DomainError.Unknown(message = message, cause = this)
}
