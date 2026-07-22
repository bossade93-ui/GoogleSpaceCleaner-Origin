package com.googlespacecleaner.feature.cleanup.viewmodel

import app.cash.turbine.test
import com.googlespacecleaner.core.domain.model.CleanupAction
import com.googlespacecleaner.core.domain.model.CleanupActionType
import com.googlespacecleaner.core.domain.model.CleanupStatus
import com.googlespacecleaner.core.domain.model.DataSource
import com.googlespacecleaner.core.domain.model.ScannedItem
import com.googlespacecleaner.core.domain.repository.CleanupRepository
import com.googlespacecleaner.core.domain.repository.SelectionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CleanupViewModelTest {

    // Partagé entre setMain() et runTest() : le ViewModel lance ses coroutines
    // (viewModelScope) sur Dispatchers.Main, donc runTest doit piloter le même
    // scheduler pour pouvoir les faire progresser via advanceUntilIdle(),
    // sinon elles restent en file d'attente et uiState ne reflète jamais que
    // sa valeur initiale pendant tout le test.
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var selectionRepository: SelectionRepository
    private lateinit var cleanupRepository: CleanupRepository
    private lateinit var selectionFlow: MutableStateFlow<List<ScannedItem>>

    private fun item(id: String, size: Long = 1_000) = ScannedItem(
        id = id, source = DataSource.DRIVE, name = "file $id", sizeBytes = size,
        mimeType = "application/pdf", createdAt = 0, modifiedAt = 0,
        contentHash = null, thumbnailUrl = null
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        selectionFlow = MutableStateFlow(emptyList())
        selectionRepository = mockk(relaxed = true) {
            every { selection } returns selectionFlow
        }
        cleanupRepository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `items from selection are all checked by default`() = kotlinx.coroutines.test.runTest(testDispatcher) {
        val viewModel = CleanupViewModel(selectionRepository, cleanupRepository)
        selectionFlow.value = listOf(item("a"), item("b"))
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertEquals(setOf("a", "b"), state.checkedIds)
        }
    }

    @Test
    fun `toggling an item removes it from the checked set`() = kotlinx.coroutines.test.runTest(testDispatcher) {
        val viewModel = CleanupViewModel(selectionRepository, cleanupRepository)
        selectionFlow.value = listOf(item("a"), item("b"))
        advanceUntilIdle()

        viewModel.toggleItem("a")

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertEquals(setOf("b"), state.checkedIds)
        }
    }

    @Test
    fun `confirmDeletion with no selection does nothing`() = kotlinx.coroutines.test.runTest(testDispatcher) {
        val viewModel = CleanupViewModel(selectionRepository, cleanupRepository)
        selectionFlow.value = listOf(item("a"))
        advanceUntilIdle()
        viewModel.toggleItem("a") // décoche tout

        viewModel.confirmDeletion()

        coVerify(exactly = 0) { cleanupRepository.executeCleanup(any()) }
    }

    @Test
    fun `successful confirmDeletion clears the selection and reports success`() = kotlinx.coroutines.test.runTest(testDispatcher) {
        val successAction = CleanupAction(
            id = "action-1",
            itemSources = mapOf("a" to DataSource.DRIVE),
            actionType = CleanupActionType.MOVE_TO_TRASH,
            timestamp = 0L,
            status = CleanupStatus.COMPLETED,
            spaceFreedBytes = 1_000
        )
        coEvery { cleanupRepository.executeCleanup(any()) } returns Result.success(successAction)

        val viewModel = CleanupViewModel(selectionRepository, cleanupRepository)
        selectionFlow.value = listOf(item("a"))
        advanceUntilIdle()

        viewModel.confirmDeletion()
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state.result is CleanupResult.Success)
        }
        coVerify { selectionRepository.clear() }
    }

    @Test
    fun `failed confirmDeletion reports failure without clearing selection`() = kotlinx.coroutines.test.runTest(testDispatcher) {
        coEvery { cleanupRepository.executeCleanup(any()) } returns
            Result.failure(java.io.IOException("network error"))

        val viewModel = CleanupViewModel(selectionRepository, cleanupRepository)
        selectionFlow.value = listOf(item("a"))
        advanceUntilIdle()

        viewModel.confirmDeletion()
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state.result is CleanupResult.Failure)
        }
        coVerify(exactly = 0) { selectionRepository.clear() }
    }
}
