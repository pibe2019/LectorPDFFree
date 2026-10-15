package com.antonio.mypdffree.domain.model

/** Entrada del historial de documentos recientes. */
data class RecentDocument(
    val uri: String,
    val displayName: String,
    val sizeBytes: Long?,
    val pageCount: Int,
    val lastPageIndex: Int,
    val lastOpenedAtMillis: Long,
)
