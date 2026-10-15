package com.antonio.mypdffree.data.mapper

import com.antonio.mypdffree.data.error.DomainErrorException
import com.antonio.mypdffree.data.util.resultOf
import com.antonio.mypdffree.domain.error.DomainError
import com.antonio.mypdffree.domain.model.result.DomainResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.FileNotFoundException
import kotlin.coroutines.cancellation.CancellationException

class DomainErrorMapperTest {

    @Test
    fun `cada excepcion de plataforma se traduce a su error de dominio`() {
        assertEquals(DomainError.Pdf.NotFound, FileNotFoundException().toDomainError())
        assertEquals(DomainError.Pdf.AccessDenied, SecurityException().toDomainError())
        assertEquals(DomainError.Pdf.RenderFailed, OutOfMemoryError().toDomainError())
    }

    @Test
    fun `el sobre DomainErrorException entrega el error ya decidido`() {
        assertEquals(DomainError.Pdf.PasswordProtected, DomainErrorException(DomainError.Pdf.PasswordProtected).toDomainError())
    }

    @Test
    fun `una excepcion desconocida usa el fallback o cae en Unknown`() {
        val error = IllegalStateException("boom")
        assertEquals(DomainError.Local.Storage, error.toDomainError(fallback = DomainError.Local.Storage))
        assertEquals(DomainError.Unknown("boom", error), error.toDomainError())
    }

    @Test
    fun `resultOf convierte excepciones en Failure pero relanza la cancelacion`() {
        assertEquals(DomainResult.Failure(DomainError.Pdf.NotFound), resultOf { throw FileNotFoundException() })

        val thrown = runCatching { resultOf { throw CancellationException("cancelado") } }.exceptionOrNull()
        assertTrue(thrown is CancellationException)
    }
}
