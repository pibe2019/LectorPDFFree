package com.antonio.mypdffree.ui.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.antonio.mypdffree.ui.core.navigation.ex.back
import com.antonio.mypdffree.ui.core.navigation.ex.navigateTo
import com.antonio.mypdffree.ui.home.HomeScreen
import com.antonio.mypdffree.ui.reader.ReaderScreen

/**
 * @param initialDocumentUri PDF recibido de otra app ("Abrir con"). Se apila SOBRE Home para que
 * "atras" lleve al historial y no cierre la app de golpe. `rememberNavBackStack` solo usa los valores
 * iniciales la primera vez; tras rotar o tras muerte de proceso restaura el back stack guardado.
 */
@Composable
fun NavigationWrapper(
    initialDocumentUri: String?,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val initialKeys: Array<NavKey> = listOfNotNull(
        RoutesScreens.Home,
        initialDocumentUri?.let(RoutesScreens::Reader),
    ).toTypedArray()
    val backStack = rememberNavBackStack(*initialKeys)

    val goBack: () -> Unit = { if (!backStack.back()) onFinish() }

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = goBack,
        // SaveableStateHolder: cada entrada conserva su estado (scroll, etc.).
        // ViewModelStore: cada entrada tiene sus ViewModels y se destruyen al sacarla del back stack;
        // asi el ReaderViewModel cierra el PDF justo al salir del lector.
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<RoutesScreens.Home> {
                HomeScreen(onOpenDocument = { uri -> backStack.navigateTo(RoutesScreens.Reader(uri)) })
            }
            entry<RoutesScreens.Reader> { key ->
                ReaderScreen(documentUri = key.documentUri, onBack = goBack)
            }
        },
    )
}
