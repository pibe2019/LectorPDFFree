package com.antonio.mypdffree.ui.reader

sealed interface ReaderEvent {
    /* LECTURA */
    data class OnPageChanged(val pageIndex: Int) : ReaderEvent
    data object OnScrollHandled : ReaderEvent
    /* IR A PAGINA */
    data object OnGoToPageClick : ReaderEvent
    data class OnGoToPageInputChange(val input: String) : ReaderEvent
    data object OnGoToPageConfirm : ReaderEvent
    data object OnGoToPageDismiss : ReaderEvent
    /* ERROR */
    data object OnRemoveFromRecentsClick : ReaderEvent
    /* ACUSES DE RECIBO */
    data class OnMessageShown(val id: Long) : ReaderEvent
    data object OnNavigatedBack : ReaderEvent
}
