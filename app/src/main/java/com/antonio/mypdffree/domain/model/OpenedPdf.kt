package com.antonio.mypdffree.domain.model

/** Resultado de abrir un PDF: el documento y la pagina donde el usuario lo dejo la ultima vez. */
data class OpenedPdf(
    val handle: PdfDocumentHandle,
    val initialPageIndex: Int,
)
