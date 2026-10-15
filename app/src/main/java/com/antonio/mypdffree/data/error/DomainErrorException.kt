package com.antonio.mypdffree.data.error

import com.antonio.mypdffree.domain.error.DomainError
import java.io.IOException

/**
 * "Sobre" con un DomainError ya decidido, para cuando el TIPO de excepcion de la plataforma no basta.
 * Caso real: `PdfRenderer(...)` lanza SecurityException si el PDF tiene contraseña, el mismo tipo que
 * `ContentResolver` usa para "sin permiso". Quien conoce el contexto lo traduce y lo lanza en este sobre;
 * `toDomainError()` solo lo abre. Nunca sale de data: `resultOf` lo convierte en valor.
 */
internal class DomainErrorException(val error: DomainError) : IOException()
