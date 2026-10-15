package com.antonio.mypdffree.ui.home

sealed interface HomeEvent {
    data class OnRemoveRecent(val uri: String) : HomeEvent
    data object OnClearHistoryClick : HomeEvent
    data object OnClearHistoryConfirm : HomeEvent
    data object OnClearHistoryDismiss : HomeEvent
    /** El dispositivo no tiene selector de documentos (raro: algunos perfiles de empresa o ROMs recortadas). */
    data object OnPickerUnavailable : HomeEvent
    data class OnMessageShown(val id: Long) : HomeEvent
}
