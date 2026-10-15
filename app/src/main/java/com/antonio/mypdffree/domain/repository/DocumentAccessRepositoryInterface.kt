package com.antonio.mypdffree.domain.repository

import com.antonio.mypdffree.domain.model.result.AppResult

/**
 * Conservar el acceso a un documento entre reinicios de la app.
 * Interfaz separada de [PdfDocumentRepositoryInterface] (ISP): abrir y retener acceso cambian por
 * motivos distintos y los usan casos de uso distintos.
 */
interface DocumentAccessRepositoryInterface {
    /** `true` si el acceso quedo retenido; `false` si el origen no lo permite (p. ej. un adjunto de otra app). */
    suspend fun retainAccess(uri: String): AppResult<Boolean>
    suspend fun releaseAccess(uri: String): AppResult<Unit>
}
