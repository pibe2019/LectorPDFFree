package com.antonio.mypdffree.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antonio.mypdffree.domain.error.DomainError
import com.antonio.mypdffree.domain.model.PageBitmap
import com.antonio.mypdffree.domain.model.PdfDocumentHandle
import com.antonio.mypdffree.domain.model.result.AppResult
import com.antonio.mypdffree.domain.model.result.DomainResult
import com.antonio.mypdffree.domain.model.result.fold
import com.antonio.mypdffree.domain.usecase.reader.OpenPdfDocumentUseCase
import com.antonio.mypdffree.domain.usecase.reader.RenderPdfPageUseCase
import com.antonio.mypdffree.domain.usecase.reader.SaveReadingProgressUseCase
import com.antonio.mypdffree.domain.usecase.reader.ValidatePageNumberUseCase
import com.antonio.mypdffree.domain.usecase.recent.RemoveRecentDocumentUseCase
import com.antonio.mypdffree.ui.core.util.UiText
import com.antonio.mypdffree.ui.modelui.AppMessage
import com.antonio.mypdffree.ui.modelui.UserMessage
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Assisted injection: el URI llega desde la ruta de Navigation 3 (`RoutesScreens.Reader`) como
 * parametro del constructor. Es la forma recomendada con Nav3; SavedStateHandle no recibe los
 * argumentos de una NavKey.
 */
@HiltViewModel(assistedFactory = ReaderViewModel.Factory::class)
class ReaderViewModel @AssistedInject constructor(
    @Assisted private val documentUri: String,
    private val openPdfDocument: OpenPdfDocumentUseCase,
    private val renderPdfPage: RenderPdfPageUseCase,
    private val saveReadingProgress: SaveReadingProgressUseCase,
    private val validatePageNumber: ValidatePageNumberUseCase,
    private val removeRecentDocument: RemoveRecentDocumentUseCase,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(documentUri: String): ReaderViewModel
    }

    private val _uiState = MutableStateFlow(ReaderUiState())
    private var document: PdfDocumentHandle? = null
    private var saveProgressJob: Job? = null

    /**
     * SharingStarted.Lazily (y no WhileSubscribed): el PDF se abre UNA vez, con el primer suscriptor,
     * y se mantiene abierto hasta que el ViewModel muere. Con WhileSubscribed se cerraria y reabriria
     * cada vez que la app pasa 5 s en segundo plano. Sustituye a un init{}.
     */
    private val content: StateFlow<ReaderContent> = flow { emit(openDocument()) }
        .stateIn(viewModelScope, SharingStarted.Lazily, ReaderContent.Loading)

    val uiState: StateFlow<ReaderUiState> = combine(_uiState, content) { state, content ->
        state.copy(content = content)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReaderUiState(),
    )

    private suspend fun openDocument(): ReaderContent =
        openPdfDocument(documentUri).fold(
            onSuccess = { opened ->
                // addCloseable: el handle se cierra en onCleared sin sobrescribirlo; si el ViewModel ya
                // estuviera destruido, lo cierra en el acto. Nunca queda un descriptor huerfano.
                addCloseable(opened.handle)
                document = opened.handle
                _uiState.update { it.copy(currentPageIndex = opened.initialPageIndex) }
                ReaderContent.Ready(opened.handle.info, opened.initialPageIndex)
            },
            onFailure = { error -> ReaderContent.Failed(error) },
        )

    /**
     * Carga bajo demanda de UNA pagina. Excepcion deliberada a "la UI solo lee estado": los bitmaps no
     * son estado (pesan decenas de MB y la lista los pide y suelta al hacer scroll), igual que una
     * imagen de red con Coil. La UI sigue sin conocer use cases: solo recibe esta funcion.
     */
    suspend fun loadPage(pageIndex: Int, widthPx: Int): AppResult<PageBitmap> {
        val handle = document ?: return DomainResult.Failure(DomainError.Pdf.RenderFailed)
        return renderPdfPage(handle, pageIndex, widthPx)
    }

    fun onEvent(event: ReaderEvent) {
        when (event) {
            is ReaderEvent.OnPageChanged -> onPageChanged(event.pageIndex)
            ReaderEvent.OnScrollHandled -> _uiState.update { it.copy(pendingScrollToPage = null) }
            ReaderEvent.OnGoToPageClick -> _uiState.update {
                it.copy(
                    isGoToPageDialogVisible = true,
                    goToPageInput = (it.currentPageIndex + 1).toString(),
                    goToPageError = null,
                )
            }
            is ReaderEvent.OnGoToPageInputChange -> _uiState.update {
                // Solo digitos y como mucho 6: ningun PDF real tiene un millon de paginas.
                it.copy(goToPageInput = event.input.filter(Char::isDigit).take(MAX_PAGE_DIGITS), goToPageError = null)
            }
            ReaderEvent.OnGoToPageConfirm -> confirmGoToPage()
            ReaderEvent.OnGoToPageDismiss -> _uiState.update { it.copy(isGoToPageDialogVisible = false) }
            ReaderEvent.OnRemoveFromRecentsClick -> removeFromRecents()
            is ReaderEvent.OnMessageShown -> _uiState.update { state ->
                state.copy(userMessages = state.userMessages.filterNot { it.id == event.id })
            }
            ReaderEvent.OnNavigatedBack -> _uiState.update { it.copy(navigateBack = false) }
        }
    }

    /**
     * Se guarda en cada cambio de pagina, cancelando la escritura anterior si aun no termino: un fling
     * largo genera muchas, pero solo importa la ultima. Es un UPDATE de una fila; no hace falta debounce
     * y asi no se pierde la ultima pagina si el usuario sale justo despues.
     */
    private fun onPageChanged(pageIndex: Int) {
        if (pageIndex == _uiState.value.currentPageIndex) return
        _uiState.update { it.copy(currentPageIndex = pageIndex) }
        saveProgressJob?.cancel()
        // Un fallo al guardar el progreso no merece interrumpir la lectura: se ignora a proposito.
        saveProgressJob = viewModelScope.launch { saveReadingProgress(documentUri, pageIndex) }
    }

    private fun confirmGoToPage() {
        // El total sale de `content` (fuente de verdad del documento), no de _uiState: ahi `content`
        // nunca se rellena, solo se combina en uiState.
        val pageCount = (content.value as? ReaderContent.Ready)?.info?.pageCount ?: return
        validatePageNumber(_uiState.value.goToPageInput, pageCount).fold(
            onSuccess = { pageIndex ->
                _uiState.update { it.copy(isGoToPageDialogVisible = false, pendingScrollToPage = pageIndex) }
            },
            onFailure = { error -> _uiState.update { it.copy(goToPageError = error) } },
        )
    }

    private fun removeFromRecents() {
        viewModelScope.launch {
            removeRecentDocument(documentUri).fold(
                onSuccess = { _uiState.update { it.copy(navigateBack = true) } },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(userMessages = it.userMessages + UserMessage.of(AppMessage.Error(UiText.FromError(error))))
                    }
                },
            )
        }
    }

    private companion object {
        const val MAX_PAGE_DIGITS = 6
    }
}
