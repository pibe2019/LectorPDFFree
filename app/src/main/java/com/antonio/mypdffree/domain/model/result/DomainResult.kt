package com.antonio.mypdffree.domain.model.result

import com.antonio.mypdffree.domain.error.DomainError

sealed interface DomainResult<out T, out E : DomainError> {
    data class Success<out T>(val data: T) : DomainResult<T, Nothing>
    data class Failure<out E : DomainError>(val error: E) : DomainResult<Nothing, E>
}

/** Obliga a tratar exito y error. */
inline fun <T, E : DomainError, R> DomainResult<T, E>.fold(
    onSuccess: (T) -> R,
    onFailure: (E) -> R,
): R = when (this) {
    is DomainResult.Success -> onSuccess(data)
    is DomainResult.Failure -> onFailure(error)
}

inline fun <T, E : DomainError, R> DomainResult<T, E>.map(transform: (T) -> R): DomainResult<R, E> =
    when (this) {
        is DomainResult.Success -> DomainResult.Success(transform(data))
        is DomainResult.Failure -> this
    }

/** Desempaqueta o corta el flujo (`return` no local): encadena pasos sin piramide de callbacks. */
inline fun <T, E : DomainError> DomainResult<T, E>.getOrElse(onFailure: (E) -> T): T =
    when (this) {
        is DomainResult.Success -> data
        is DomainResult.Failure -> onFailure(error)
    }

fun <T, E : DomainError> DomainResult<T, E>.getOrNull(): T? = (this as? DomainResult.Success)?.data

inline fun <T, E : DomainError> DomainResult<T, E>.onSuccess(action: (T) -> Unit): DomainResult<T, E> {
    if (this is DomainResult.Success) action(data)
    return this
}

inline fun <T, E : DomainError> DomainResult<T, E>.onFailure(action: (E) -> Unit): DomainResult<T, E> {
    if (this is DomainResult.Failure) action(error)
    return this
}
// No existe getOrThrow a proposito: dentro de viewModelScope.launch una excepcion sin capturar tumba la app.
