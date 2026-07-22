package com.googlespacecleaner.feature.dashboard.viewmodel

import app.cash.turbine.test
import com.googlespacecleaner.core.data.local.db.ScannedItemDao
import com.googlespacecleaner.core.data.local.db.ScannedItemEntity
import com.googlespacecleaner.core.domain.model.DataSource
import com.googlespacecleaner.core.domain.repository.AuthRepository
import com.googlespacecleaner.core.domain.repository.AuthState
import com.googlespacecleaner.core.domain.repository.CleanupRepository
import com.googlespacecleaner.core.domain.repository.GoogleAccountInfo
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    // Partagé entre setMain() et runTest() : le ViewModel lance ses coroutines
    // (viewModelScope) sur Dispatchers.Main, donc runTest doit piloter le même
    // scheduler pour pouvoir les faire progresser via advanceUntilIdle(),
    // sinon elles restent en file d'attente et uiState ne reflète jamais que
    // sa valeur initiale pendant tout le test.
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var scannedItemDao: ScannedItemDao
    private lateinit var cleanupRepository: CleanupRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var authStateFlow: MutableStateFlow<AuthState>

    private fun entity(source: DataSource, size: Long, flags: String = "") = ScannedItemEntity(
        id = "id-${source.name}-$size", source = source.name, name = "item", sizeBytes = size,
        mimeType = "application/octet-stream", createdAt = 0, modifiedAt = 0,
        contentHash = null, thumbnailUrl = null, flagsCsv = flags
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        scannedItemDao = mockk()
        cleanupRepository = mockk()
        authStateFlow = MutableStateFlow(AuthState.SignedOut)
        authRepository = mockk {
            every { authState } returns authStateFlow
        }
        coEvery { scannedItemDao.getBySource(any()) } returns emptyList()
        coEvery { cleanupRepository.getHistory() } returns emptyList()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `aggregates bytes across drive, both photos sources, and gmail`() = runTest(testDispatcher) {
        coEvery { scannedItemDao.getBySource(DataSource.DRIVE.name) } returns
            listOf(entity(DataSource.DRIVE, 1_000))
        coEvery { scannedItemDao.getBySource(DataSource.PHOTOS_PICKER.name) } returns
            listOf(entity(DataSource.PHOTOS_PICKER, 2_000))
        coEvery { scannedItemDao.getBySource(DataSource.PHOTOS_TAKEOUT.name) } returns
            listOf(entity(DataSource.PHOTOS_TAKEOUT, 3_000))
        coEvery { scannedItemDao.getBySource(DataSource.GMAIL.name) } returns
            listOf(entity(DataSource.GMAIL, 4_000))

        val viewModel = DashboardViewModel(scannedItemDao, cleanupRepository, authRepository)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertEquals(1_000L, state.perSourceBytes[DataSource.DRIVE])
            assertEquals(5_000L, state.perSourceBytes[DataSource.PHOTOS_PICKER]) // picker + takeout combinés
            assertEquals(4_000L, state.perSourceBytes[DataSource.GMAIL])
            assertEquals(10_000L, state.totalUsedBytes)
        }
    }

    @Test
    fun `recoverable bytes only counts items flagged as duplicate`() = runTest(testDispatcher) {
        coEvery { scannedItemDao.getBySource(DataSource.DRIVE.name) } returns listOf(
            entity(DataSource.DRIVE, 1_000, flags = "DUPLICATE"),
            entity(DataSource.DRIVE, 500, flags = "")
        )

        val viewModel = DashboardViewModel(scannedItemDao, cleanupRepository, authRepository)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertEquals(1_000L, state.recoverableBytes)
        }
    }

    @Test
    fun `account email is populated once the user is signed in`() = runTest(testDispatcher) {
        val viewModel = DashboardViewModel(scannedItemDao, cleanupRepository, authRepository)
        authStateFlow.value = AuthState.SignedIn(GoogleAccountInfo("a@b.com", "A B", null))
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertEquals("a@b.com", state.accountEmail)
        }
    }

    @Test
    fun `recentCleanupsCount reflects the size of the cleanup history`() = runTest(testDispatcher) {
        coEvery { cleanupRepository.getHistory() } returns listOf(mockk(), mockk(), mockk())

        val viewModel = DashboardViewModel(scannedItemDao, cleanupRepository, authRepository)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertEquals(3, state.recentCleanupsCount)
        }
    }

    @Test
    fun `isLoading becomes false once refresh completes`() = runTest(testDispatcher) {
        val viewModel = DashboardViewModel(scannedItemDao, cleanupRepository, authRepository)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertEquals(false, state.isLoading)
        }
    }
}
