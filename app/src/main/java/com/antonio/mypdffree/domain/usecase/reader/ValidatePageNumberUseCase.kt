package com.antonio.mypdffree.domain.usecase.reader

import com.antonio.mypdffree.domain.error.DomainError
import com.antonio.mypdffree.domain.model.result.DomainResult
import javax.inject.Inject

/**
 * Valida el numero de pagina que escribe el usuario (base 1) y devuelve el INDICE (base 0).
 * El tipo de error es `DomainError.Validation` y no el general: la UI lo muestra inline en el campo.
 */
class ValidatePageNumberUseCase @Inject constructor() {

    operator fun invoke(input: String, pageCount: Int): DomainResult<Int, DomainError.Validation> {
        val text = input.trim()
        if (text.isEmpty()) return DomainResult.Failure(DomainError.Validation.PageNumberEmpty)
        val pageNumber = text.toIntOrNull()
            ?: return DomainResult.Failure(DomainError.Validation.PageNumberNotANumber)
        if (pageNumber !in 1..pageCount) {
            return DomainResult.Failure(DomainError.Validation.PageNumberOutOfRange(pageCount))
        }
        return DomainResult.Success(pageNumber - 1)
    }
}
