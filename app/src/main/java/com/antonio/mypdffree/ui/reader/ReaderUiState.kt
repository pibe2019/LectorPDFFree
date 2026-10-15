package com.antonio.mypdffree.ui.reader

import com.antonio.mypdffree.domain.error.DomainError
import com.antonio.mypdffree.domain.model.PdfDocumentInfo
import com.antonio.mypdffree.ui.modelui.UserMessage

/** Ciclo de vida de la apertura: sealed para que la UI cubra los 3 casos (el compilador lo exige). */
sealed interface ReaderContent {
    data object Loading : ReaderContent
    data class Ready(val info: PdfDocumentInfo, val initialPageIndex: Int) : ReaderContent
    data class Failed(val error: DomainError) : ReaderContent {
        /** Solo tiene sentido ofrecer "quitar de recientes" si el archivo ya no es accesible. */
        val canRemoveFromRecents: Boolean
            get() = error == DomainError.Pdf.NotFound || error == DomainError.Pdf.AccessDenied
    }
}

data class ReaderUiState(
    val content: ReaderContent = ReaderContent.Loading,
    val currentPageIndex: Int = 0,
    // dialogo "ir a pagina"
    val isGoToPageDialogVisible: Boolean = false,
    val goToPageInput: String = "",
    val goToPageError: DomainError.Validation? = null,
    /** Pagina a la que la UI debe desplazarse; la UI confirma con OnScrollHandled (acuse de recibo). */
    val pendingScrollToPage: Int? = null,
    val userMessages: List<UserMessage> = emptyList(),
    val navigateBack: Boolean = false,
) {
    val pageCount: Int get() = (content as? ReaderContent.Ready)?.info?.pageCount ?: 0
}
