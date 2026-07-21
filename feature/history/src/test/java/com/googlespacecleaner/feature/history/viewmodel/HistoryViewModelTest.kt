package com.googlespacecleaner.feature.history.viewmodel

import app.cash.turbine.test
import com.googlespacecleaner.core.domain.model.CleanupAction
import com.googlespacecleaner.core.domain.model.CleanupActionType
import com.googlespacecleaner.core.domain.model.CleanupStatus
import com.googlespacecleaner.core.domain.model.DataSource
import com.googlespacecleaner.core.domain.repository.CleanupRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private lateinit var cleanupRepository: CleanupRepository

    private fun action(id: String, status: CleanupStatus = CleanupStatus.COMPLETED) = CleanupAction(
        id = id,
        itemSources = mapOf("item-$id" to DataSource.DRIVE),
        actionType = CleanupActionType.MOVE_TO_TRASH,
        timestamp = 0L,
        status = status,
        spaceFreedBytes = 1_000
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        cleanupRepository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads history on init`() = runTest {
        coEvery { cleanupRepository.getHistory() } returns listOf(action("1"), action("2"))

        val viewModel = HistoryViewModel(cleanupRepository)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertEquals(2, state.actions.size)
            assertEquals(false, state.isLoading)
        }
    }

    @Test
    fun `successful undo triggers a refresh of the history`() = runTest {
        coEvery { cleanupRepository.getHistory() } returnsMany listOf(
            listOf(action("1", CleanupStatus.COMPLETED)),
            listOf(action("1", CleanupStatus.UNDONE))
        )
        coEvery { cleanupRepository.undo("1") } returns Result.success(Unit)

        val viewModel = HistoryViewModel(cleanupRepository)
        viewModel.undo("1")

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertEquals(CleanupStatus.UNDONE, state.actions.single().status)
        }
        coVerify(exactly = 2) { cleanupRepository.getHistory() } // 1 au chargement initial, 1 après l'annulation
    }

    @Test
    fun `failed undo surfaces an error message without refreshing`() = runTest {
        coEvery { cleanupRepository.getHistory() } returns listOf(action("1"))
        coEvery { cleanupRepository.undo("1") } returns
            Result.failure(UnsupportedOperationException("Suppression manuelle uniquement."))

        val viewModel = HistoryViewModel(cleanupRepository)
        viewModel.undo("1")

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertEquals("Suppression manuelle uniquement.", state.undoError)
        }
        coVerify(exactly = 1) { cleanupRepository.getHistory() } // pas de second appel après un échec
    }

    @Test
    fun `dismissError clears the error message`() = runTest {
        coEvery { cleanupRepository.getHistory() } returns listOf(action("1"))
        coEvery { cleanupRepository.undo("1") } returns Result.failure(RuntimeException("erreur"))

        val viewModel = HistoryViewModel(cleanupRepository)
        viewModel.undo("1")
        viewModel.dismissError()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertNull(state.undoError)
        }
    }
}
