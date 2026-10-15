package com.antonio.mypdffree.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antonio.mypdffree.R
import com.antonio.mypdffree.domain.model.result.fold
import com.antonio.mypdffree.domain.usecase.recent.ClearRecentDocumentsUseCase
import com.antonio.mypdffree.domain.usecase.recent.ObserveRecentDocumentsUseCase
import com.antonio.mypdffree.domain.usecase.recent.RemoveRecentDocumentUseCase
import com.antonio.mypdffree.ui.core.util.UiText
import com.antonio.mypdffree.ui.modelui.AppMessage
import com.antonio.mypdffree.ui.modelui.UserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeRecentDocuments: ObserveRecentDocumentsUseCase,
    private val removeRecentDocument: RemoveRecentDocumentUseCase,
    private val clearRecentDocuments: ClearRecentDocumentsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())

    /**
     * Estado de interaccion + datos reactivos en UN solo UiState. Sin init{}: el flujo de Room se
     * suscribe solo cuando la UI lo observa y se suelta 5 s despues de dejar de hacerlo
     * (sobrevive a una rotacion sin re-consultar).
     */
    val uiState: StateFlow<HomeUiState> = combine(_uiState, observeRecentDocuments()) { state, recents ->
        state.copy(recents = recents, isLoading = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.OnRemoveRecent -> removeRecent(event.uri)
            HomeEvent.OnClearHistoryClick -> _uiState.update { it.copy(isClearHistoryDialogVisible = true) }
            HomeEvent.OnClearHistoryDismiss -> _uiState.update { it.copy(isClearHistoryDialogVisible = false) }
            HomeEvent.OnClearHistoryConfirm -> clearHistory()
            HomeEvent.OnPickerUnavailable -> showMessage(AppMessage.Error(UiText.StringResource(R.string.error_picker_unavailable)))
            is HomeEvent.OnMessageShown -> _uiState.update { state ->
                state.copy(userMessages = state.userMessages.filterNot { it.id == event.id })
            }
        }
    }

    private fun removeRecent(uri: String) {
        viewModelScope.launch {
            removeRecentDocument(uri).fold(
                onSuccess = { showMessage(AppMessage.Info(UiText.StringResource(R.string.home_recent_removed))) },
                onFailure = { error -> showMessage(AppMessage.Error(UiText.FromError(error))) },
            )
        }
    }

    private fun clearHistory() {
        _uiState.update { it.copy(isClearHistoryDialogVisible = false) }
        viewModelScope.launch {
            clearRecentDocuments().fold(
                onSuccess = { showMessage(AppMessage.Info(UiText.StringResource(R.string.home_history_cleared))) },
                onFailure = { error -> showMessage(AppMessage.Error(UiText.FromError(error))) },
            )
        }
    }

    private fun showMessage(message: AppMessage) {
        _uiState.update { it.copy(userMessages = it.userMessages + UserMessage.of(message)) }
    }
}
