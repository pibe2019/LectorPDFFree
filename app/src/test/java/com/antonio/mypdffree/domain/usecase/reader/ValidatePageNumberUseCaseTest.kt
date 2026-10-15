package com.antonio.mypdffree.domain.usecase.reader

import com.antonio.mypdffree.domain.error.DomainError
import com.antonio.mypdffree.domain.model.result.DomainResult
import org.junit.Assert.assertEquals
import org.junit.Test

class ValidatePageNumberUseCaseTest {

    private val validate = ValidatePageNumberUseCase()

    @Test
    fun `numero valido devuelve el indice base cero`() {
        assertEquals(DomainResult.Success(4), validate("5", pageCount = 10))
    }

    @Test
    fun `admite espacios alrededor y los limites del rango`() {
        assertEquals(DomainResult.Success(0), validate(" 1 ", pageCount = 10))
        assertEquals(DomainResult.Success(9), validate("10", pageCount = 10))
    }

    @Test
    fun `texto vacio es PageNumberEmpty`() {
        assertEquals(DomainResult.Failure(DomainError.Validation.PageNumberEmpty), validate("   ", pageCount = 10))
    }

    @Test
    fun `texto no numerico es PageNumberNotANumber`() {
        assertEquals(DomainResult.Failure(DomainError.Validation.PageNumberNotANumber), validate("3a", pageCount = 10))
    }

    @Test
    fun `cero y paginas mayores al total estan fuera de rango`() {
        val expected = DomainResult.Failure(DomainError.Validation.PageNumberOutOfRange(pageCount = 10))
        assertEquals(expected, validate("0", pageCount = 10))
        assertEquals(expected, validate("11", pageCount = 10))
    }

    @Test
    fun `un numero que desborda Int no lanza excepcion`() {
        assertEquals(
            DomainResult.Failure(DomainError.Validation.PageNumberNotANumber),
            validate("99999999999", pageCount = 10),
        )
    }
}
