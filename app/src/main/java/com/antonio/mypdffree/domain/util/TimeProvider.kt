package com.antonio.mypdffree.domain.util

/**
 * Reloj inyectable. `System.currentTimeMillis()` directo en un use case lo haria no determinista
 * y no habria forma de testear el orden del historial.
 */
fun interface TimeProvider {
    fun nowMillis(): Long
}
