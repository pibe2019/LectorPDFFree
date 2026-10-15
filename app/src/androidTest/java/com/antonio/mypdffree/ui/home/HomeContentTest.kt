package com.antonio.mypdffree.ui.home

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.antonio.mypdffree.domain.model.RecentDocument
import com.antonio.mypdffree.ui.theme.MyPdfFreeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Se prueba la capa SIN estado (HomeContent): no necesita Hilt ni ViewModel, solo estado y callbacks. */
@RunWith(AndroidJUnit4::class)
class HomeContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun historial_vacio_muestra_el_estado_vacio_y_su_boton_abre_el_selector() {
        var openClicks = 0
        setContent(HomeUiState(isLoading = false), onOpenPdfClick = { openClicks++ })

        composeRule.onNodeWithText("Aún no has abierto ningún PDF").assertIsDisplayed()
        // Hay dos "Abrir PDF": el FAB y el boton del estado vacio. Ambos deben abrir el selector.
        val buttons = composeRule.onAllNodesWithText("Abrir PDF")
        buttons[0].performClick()
        buttons[1].performClick()
        assertEquals(2, openClicks)
    }

    @Test
    fun tocar_un_reciente_lo_abre() {
        val recent = RecentDocument("content://a", "Contrato.pdf", null, 12, 2, 0L)
        var opened: RecentDocument? = null
        setContent(HomeUiState(isLoading = false, recents = listOf(recent)), onRecentClick = { opened = it })

        composeRule.onNodeWithText("Página 3 de 12").assertIsDisplayed()
        composeRule.onNodeWithText("Contrato.pdf").performClick()
        assertEquals(recent, opened)
    }

    private fun setContent(
        state: HomeUiState,
        onOpenPdfClick: () -> Unit = {},
        onRecentClick: (RecentDocument) -> Unit = {},
    ) {
        composeRule.setContent {
            MyPdfFreeTheme(dynamicColor = false) {
                HomeContent(state, SnackbarHostState(), onOpenPdfClick, onRecentClick, onEvent = {})
            }
        }
    }
}
