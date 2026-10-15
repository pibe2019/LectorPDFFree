package com.antonio.mypdffree.domain.model

/**
 * Metadatos de un PDF abierto.
 * [uri] es un String y no `android.net.Uri`: el dominio no conoce Android; data lo convierte en su frontera.
 */
data class PdfDocumentInfo(
    val uri: String,
    val displayName: String,
    val sizeBytes: Long?,
    val pageCount: Int,
)
