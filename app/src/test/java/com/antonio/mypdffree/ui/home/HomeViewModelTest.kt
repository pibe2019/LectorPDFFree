package com.antonio.mypdffree.ui.home

import app.cash.turbine.test
import com.antonio.mypdffree.R
import com.antonio.mypdffree.domain.error.DomainError
import com.antonio.mypdffree.domain.model.RecentDocument
import com.antonio.mypdffree.domain.model.result.DomainResult
import com.antonio.mypdffree.domain.usecase.recent.ClearRecentDocumentsUseCase
import com.antonio.mypdffree.domain.usecase.recent.ObserveRecentDocumentsUseCase
import com.antonio.mypdffree.domain.usecase.recent.RemoveRecentDocumentUseCase
import com.antonio.mypdffree.ui.core.util.UiText
import com.antonio.mypdffree.ui.modelui.AppMessage
import com.antonio.mypdffree.util.MainDispatcherRule
import com.antonio.mypdffree.util.TEST_URI
import com.antonio.mypdffree.util.recentDocument
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val recents = MutableStateFlow<List<RecentDocument>>(emptyList())
    private val observeRecentDocuments: ObserveRecentDocumentsUseCase = mockk { every { this@mockk() } returns recents }
    private val removeRecentDocument: RemoveRecentDocumentUseCase = mockk()
    private val clearRecentDocuments: ClearRecentDocumentsUseCase = mockk()

    private fun buildViewModel() = HomeViewModel(observeRecentDocuments, removeRecentDocument, clearRecentDocuments)

    @Test
    fun `empieza cargando y despues refleja el historial de Room`() = runTest(mainDispatcherRule.testDispatcher.scheduler) {
        val vm = buildViewModel()

        vm.uiState.test {
            assertTrue(awaitItem().isLoading)
            advanceUntilIdle()
            val empty = expectMostRecentItem()
            assertFalse(empty.isLoading)
            assertTrue(empty.isEmpty)

            recents.value = listOf(recentDocument())
            advanceUntilIdle()
            assertEquals(listOf(recentDocument()), expectMostRecentItem().recents)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `quitar un reciente avisa con un mensaje informativo`() = runTest(mainDispatcherRule.testDispatcher.scheduler) {
        coEvery { removeRecentDocument(TEST_URI) } returns DomainResult.Success(Unit)
        val vm = buildViewModel()

        vm.uiState.test {
            awaitItem()
            vm.onEvent(HomeEvent.OnRemoveRecent(TEST_URI))
            advanceUntilIdle()
            val message = expectMostRecentItem().userMessages.single()
            assertEquals(AppMessage.Info(UiText.StringResource(R.string.home_recent_removed)), message.message)

            vm.onEvent(HomeEvent.OnMessageShown(message.id))
            advanceUntilIdle()
            assertTrue(expectMostRecentItem().userMessages.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `si quitar falla muestra el error de dominio`() = runTest(mainDispatcherRule.testDispatcher.scheduler) {
        coEvery { removeRecentDocument(TEST_URI) } returns DomainResult.Failure(DomainError.Local.Storage)
        val vm = buildViewModel()

        vm.uiState.test {
            awaitItem()
            vm.onEvent(HomeEvent.OnRemoveRecent(TEST_URI))
            advanceUntilIdle()
            assertEquals(
                AppMessage.Error(UiText.FromError(DomainError.Local.Storage)),
                expectMostRecentItem().userMessages.single().message,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `borrar historial pide confirmacion y solo borra al confirmar`() = runTest(mainDispatcherRule.testDispatcher.scheduler) {
        coEvery { clearRecentDocuments() } returns DomainResult.Success(Unit)
        val vm = buildViewModel()

        vm.uiState.test {
            awaitItem()
            vm.onEvent(HomeEvent.OnClearHistoryClick)
            advanceUntilIdle()
            assertTrue(expectMostRecentItem().isClearHistoryDialogVisible)
            coVerify(exactly = 0) { clearRecentDocuments() }

            vm.onEvent(HomeEvent.OnClearHistoryConfirm)
            advanceUntilIdle()
            assertFalse(expectMostRecentItem().isClearHistoryDialogVisible)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 1) { clearRecentDocuments() }
    }
}
