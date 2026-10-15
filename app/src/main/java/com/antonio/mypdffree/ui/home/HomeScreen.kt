package com.antonio.mypdffree.ui.home

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.antonio.mypdffree.R
import com.antonio.mypdffree.domain.model.RecentDocument
import com.antonio.mypdffree.ui.core.component.snackbar.AppMessageSnackbarHost
import com.antonio.mypdffree.ui.core.component.snackbar.UserMessagesEffect
import com.antonio.mypdffree.ui.home.component.EmptyRecents
import com.antonio.mypdffree.ui.home.component.RecentDocumentItem
import com.antonio.mypdffree.ui.theme.MyPdfFreeTheme

private const val PDF_MIME_TYPE = "application/pdf"

/** Espacio libre al final de la lista para que el FAB no tape el ultimo elemento. */
private val FabClearance = 88.dp

/** Capa con estado: conecta ViewModel, launcher del sistema y navegacion. */
@Composable
fun HomeScreen(
    onOpenDocument: (uri: String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    UserMessagesEffect(uiState.userMessages, snackbarHostState) { id ->
        viewModel.onEvent(HomeEvent.OnMessageShown(id))
    }

    // Selector del sistema (Storage Access Framework): no requiere ningun permiso de almacenamiento.
    val openDocumentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { onOpenDocument(it.toString()) } // null = el usuario cancelo: no es un error
    }

    HomeContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onOpenPdfClick = {
            try {
                openDocumentLauncher.launch(arrayOf(PDF_MIME_TYPE))
            } catch (_: ActivityNotFoundException) {
                viewModel.onEvent(HomeEvent.OnPickerUnavailable)
            }
        },
        onRecentClick = { recent -> onOpenDocument(recent.uri) },
        onEvent = viewModel::onEvent,
    )
}

/** Capa sin estado: solo pinta y emite eventos. Es la que se previsualiza y se testea. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    uiState: HomeUiState,
    snackbarHostState: SnackbarHostState,
    onOpenPdfClick: () -> Unit,
    onRecentClick: (RecentDocument) -> Unit,
    onEvent: (HomeEvent) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    HomeOverflowMenu(
                        isClearEnabled = uiState.recents.isNotEmpty(),
                        onClearHistoryClick = { onEvent(HomeEvent.OnClearHistoryClick) },
                    )
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            // Variante con slot de contenido a proposito: la de `text =`/`icon =` no expone el texto en la
            // semantica (verificado con HomeContentTest) y TalkBack anunciaba solo "Boton".
            ExtendedFloatingActionButton(onClick = onOpenPdfClick) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.home_open_pdf))
            }
        },
        snackbarHost = { AppMessageSnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.isEmpty -> EmptyRecents(
                onOpenPdfClick = onOpenPdfClick,
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )
            else -> RecentsList(
                recents = uiState.recents,
                contentPadding = innerPadding,
                onRecentClick = onRecentClick,
                onRemoveClick = { onEvent(HomeEvent.OnRemoveRecent(it.uri)) },
            )
        }
    }

    if (uiState.isClearHistoryDialogVisible) {
        ClearHistoryDialog(
            onConfirm = { onEvent(HomeEvent.OnClearHistoryConfirm) },
            onDismiss = { onEvent(HomeEvent.OnClearHistoryDismiss) },
        )
    }
}

@Composable
private fun RecentsList(
    recents: List<RecentDocument>,
    contentPadding: PaddingValues,
    onRecentClick: (RecentDocument) -> Unit,
    onRemoveClick: (RecentDocument) -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // El padding del Scaffold va DENTRO de la lista (no como Modifier.padding) para que el
        // contenido se desplace por detras de la barra de navegacion (edge-to-edge).
        contentPadding = PaddingValues(
            start = contentPadding.calculateStartPadding(layoutDirection),
            top = contentPadding.calculateTopPadding(),
            end = contentPadding.calculateEndPadding(layoutDirection),
            bottom = contentPadding.calculateBottomPadding() + FabClearance,
        ),
    ) {
        // key = uri: Compose reconoce cada fila al reordenarse la lista (el documento abierto sube arriba).
        items(items = recents, key = { it.uri }) { recent ->
            RecentDocumentItem(
                document = recent,
                onClick = { onRecentClick(recent) },
                onRemoveClick = { onRemoveClick(recent) },
                modifier = Modifier.animateItem(),
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun HomeOverflowMenu(isClearEnabled: Boolean, onClearHistoryClick: () -> Unit) {
    // rememberSaveable: el menu sigue abierto tras rotar. Es estado de UI puro, no va al ViewModel.
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.cd_more_options))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.home_clear_history)) },
                enabled = isClearEnabled,
                onClick = {
                    expanded = false
                    onClearHistoryClick()
                },
            )
        }
    }
}

@Composable
private fun ClearHistoryDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.home_clear_history_title)) },
        text = { Text(stringResource(R.string.home_clear_history_message)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_clear)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Preview(showBackground = true)
@Composable
private fun HomeContentPreview() {
    MyPdfFreeTheme(dynamicColor = false) {
        HomeContent(
            uiState = HomeUiState(
                isLoading = false,
                recents = listOf(
                    RecentDocument("content://a", "Contrato de alquiler.pdf", 2_400_000, 12, 3, 0L),
                    RecentDocument("content://b", "Manual de usuario.pdf", null, 120, 0, 0L),
                ),
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onOpenPdfClick = {},
            onRecentClick = {},
            onEvent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeEmptyPreview() {
    MyPdfFreeTheme(dynamicColor = false) {
        HomeContent(HomeUiState(isLoading = false), remember { SnackbarHostState() }, {}, {}, {})
    }
}
