package com.antonio.mypdffree.ui.home

import com.antonio.mypdffree.domain.model.RecentDocument
import com.antonio.mypdffree.ui.modelui.UserMessage

data class HomeUiState(
    // datos (los pone el combine)
    val recents: List<RecentDocument> = emptyList(),
    /** true solo hasta la primera emision de Room; evita mostrar "vacio" un instante antes de los datos. */
    val isLoading: Boolean = true,
    // dialogos
    val isClearHistoryDialogVisible: Boolean = false,
    // avisos transitorios
    val userMessages: List<UserMessage> = emptyList(),
) {
    val isEmpty: Boolean get() = !isLoading && recents.isEmpty()
}
