package com.antonio.mypdffree.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.antonio.mypdffree.R
import com.antonio.mypdffree.ui.core.component.snackbar.AppMessageSnackbarHost
import com.antonio.mypdffree.ui.core.component.snackbar.UserMessagesEffect
import com.antonio.mypdffree.ui.reader.component.GoToPageDialog
import com.antonio.mypdffree.ui.reader.component.PageLoader
import com.antonio.mypdffree.ui.reader.component.PdfPagesList
import com.antonio.mypdffree.ui.reader.component.ReaderErrorState
import kotlinx.coroutines.flow.distinctUntilChanged

/** Hueco inferior para que el indicador de pagina no tape el final del documento. */
private val PageIndicatorClearance = 72.dp
private val IndicatorScrim = Color(0xD9202124)

@Composable
fun ReaderScreen(
    documentUri: String,
    onBack: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel<ReaderViewModel, ReaderViewModel.Factory>(
        creationCallback = { factory -> factory.create(documentUri) },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val pageLoader = remember(viewModel) { PageLoader { index, width -> viewModel.loadPage(index, width) } }

    UserMessagesEffect(uiState.userMessages, snackbarHostState) { id ->
        viewModel.onEvent(ReaderEvent.OnMessageShown(id))
    }
    LaunchedEffect(uiState.navigateBack) {
        if (uiState.navigateBack) {
            viewModel.onEvent(ReaderEvent.OnNavigatedBack)
            onBack()
        }
    }

    ReaderScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        pageLoader = pageLoader,
        onBack = onBack,
        onEvent = viewModel::onEvent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreenContent(
    uiState: ReaderUiState,
    snackbarHostState: SnackbarHostState,
    pageLoader: PageLoader,
    onBack: () -> Unit,
    onEvent: (ReaderEvent) -> Unit,
) {
    val content = uiState.content
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (content is ReaderContent.Ready) {
                        Text(content.info.displayName, maxLines = 1, overflow = TextOverflow.MiddleEllipsis)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (content is ReaderContent.Ready) {
                        ReaderOverflowMenu(onGoToPageClick = { onEvent(ReaderEvent.OnGoToPageClick) })
                    }
                },
            )
        },
        snackbarHost = { AppMessageSnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when (content) {
            ReaderContent.Loading -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is ReaderContent.Failed -> ReaderErrorState(
                error = content.error,
                canRemoveFromRecents = content.canRemoveFromRecents,
                onBackClick = onBack,
                onRemoveFromRecentsClick = { onEvent(ReaderEvent.OnRemoveFromRecentsClick) },
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )
            is ReaderContent.Ready -> ReaderDocument(
                ready = content,
                uiState = uiState,
                innerPadding = innerPadding,
                pageLoader = pageLoader,
                onEvent = onEvent,
            )
        }
    }

    if (uiState.isGoToPageDialogVisible) {
        GoToPageDialog(
            input = uiState.goToPageInput,
            pageCount = uiState.pageCount,
            error = uiState.goToPageError,
            onInputChange = { onEvent(ReaderEvent.OnGoToPageInputChange(it)) },
            onConfirm = { onEvent(ReaderEvent.OnGoToPageConfirm) },
            onDismiss = { onEvent(ReaderEvent.OnGoToPageDismiss) },
        )
    }
}

@Composable
private fun ReaderDocument(
    ready: ReaderContent.Ready,
    uiState: ReaderUiState,
    innerPadding: PaddingValues,
    pageLoader: PageLoader,
    onEvent: (ReaderEvent) -> Unit,
) {
    // rememberLazyListState es saveable: el indice inicial solo se usa la primera vez; tras rotar
    // se restaura la posicion real del scroll.
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = ready.initialPageIndex)
    val currentOnEvent by rememberUpdatedState(onEvent)

    LaunchedEffect(listState) {
        snapshotFlow { listState.centerItemIndex() }
            .distinctUntilChanged()
            .collect { currentOnEvent(ReaderEvent.OnPageChanged(it)) }
    }
    LaunchedEffect(uiState.pendingScrollToPage) {
        uiState.pendingScrollToPage?.let { page ->
            listState.scrollToItem(page)
            currentOnEvent(ReaderEvent.OnScrollHandled)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Fondo gris del tema: las hojas blancas se distinguen del "escritorio", tambien en tema oscuro.
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        PdfPagesList(
            pageCount = ready.info.pageCount,
            listState = listState,
            pageLoader = pageLoader,
            // Solo vertical como contentPadding (las paginas pasan por detras de las barras). Los insets
            // laterales (camara/landscape) van como padding real: el ancho de pagina = ancho util.
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + PageIndicatorClearance,
            ),
            modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        )
        PageIndicator(
            currentPage = uiState.currentPageIndex + 1,
            pageCount = ready.info.pageCount,
            onClick = { onEvent(ReaderEvent.OnGoToPageClick) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = innerPadding.calculateBottomPadding() + 16.dp),
        )
    }
}

/** Pagina "actual" = la que ocupa el centro de la pantalla (con la primera visible fallaria al ver dos a la vez). */
private fun LazyListState.centerItemIndex(): Int {
    val info = layoutInfo
    val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
    return info.visibleItemsInfo.firstOrNull { center in it.offset until it.offset + it.size }?.index
        ?: firstVisibleItemIndex
}

@Composable
private fun PageIndicator(currentPage: Int, pageCount: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.cd_page_indicator, currentPage, pageCount)
    Surface(
        onClick = onClick,
        modifier = modifier.semantics { contentDescription = description },
        shape = MaterialTheme.shapes.extraLarge,
        // Colores FIJOS y no del tema: el indicador flota sobre la hoja, que es blanca siempre. Con
        // inverseSurface, en tema oscuro quedaba casi blanco sobre blanco (visto en las capturas).
        color = IndicatorScrim,
        contentColor = Color.White,
        shadowElevation = 3.dp,
    ) {
        Text(
            text = stringResource(R.string.reader_page_indicator, currentPage, pageCount),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun ReaderOverflowMenu(onGoToPageClick: () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.cd_more_options))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.reader_go_to_page)) },
                onClick = {
                    expanded = false
                    onGoToPageClick()
                },
            )
        }
    }
}
