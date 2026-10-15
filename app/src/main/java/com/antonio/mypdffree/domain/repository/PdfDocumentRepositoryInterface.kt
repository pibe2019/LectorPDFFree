package com.antonio.mypdffree.domain.repository

import com.antonio.mypdffree.domain.model.PdfDocumentHandle
import com.antonio.mypdffree.domain.model.result.AppResult

interface PdfDocumentRepositoryInterface {
    /** Abre el PDF. El llamador es DUEÑO del handle devuelto y debe cerrarlo. */
    suspend fun open(uri: String): AppResult<PdfDocumentHandle>
}
