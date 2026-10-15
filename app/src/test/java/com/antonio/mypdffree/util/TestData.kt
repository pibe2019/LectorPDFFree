package com.antonio.mypdffree.util

import com.antonio.mypdffree.domain.model.PdfDocumentHandle
import com.antonio.mypdffree.domain.model.PdfDocumentInfo
import com.antonio.mypdffree.domain.model.RecentDocument
import io.mockk.every
import io.mockk.mockk

const val TEST_URI = "content://com.android.providers.downloads.documents/document/42"

fun pdfInfo(pageCount: Int = 10, uri: String = TEST_URI) = PdfDocumentInfo(
    uri = uri,
    displayName = "Contrato.pdf",
    sizeBytes = 1_024L,
    pageCount = pageCount,
)

fun recentDocument(lastPageIndex: Int = 0, uri: String = TEST_URI) = RecentDocument(
    uri = uri,
    displayName = "Contrato.pdf",
    sizeBytes = 1_024L,
    pageCount = 10,
    lastPageIndex = lastPageIndex,
    lastOpenedAtMillis = 1_000L,
)

/** relaxed: close() y renderPage() no necesitan respuesta salvo que el test la defina. */
fun fakeHandle(info: PdfDocumentInfo = pdfInfo()): PdfDocumentHandle = mockk(relaxed = true) {
    every { this@mockk.info } returns info
}
