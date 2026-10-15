package com.antonio.mypdffree.data.util

import com.antonio.mypdffree.data.mapper.toDomainError
import com.antonio.mypdffree.domain.error.DomainError
import com.antonio.mypdffree.domain.model.result.AppResult
import com.antonio.mypdffree.domain.model.result.DomainResult
import kotlin.coroutines.cancellation.CancellationException

/**
 * Unica frontera excepcion -> DomainError. Todo lo que pueda lanzar en data se envuelve aqui.
 * [fallback] solo cuando el tipo de excepcion no identifica la causa por si mismo.
 */
inline fun <T> resultOf(fallback: DomainError? = null, block: () -> T): AppResult<T> =
    try {
        DomainResult.Success(block())
    } catch (e: CancellationException) {
        throw e // la cancelacion es control de flujo de coroutines, nunca un error de negocio
    } catch (e: Throwable) {
        DomainResult.Failure(e.toDomainError(fallback))
    }
