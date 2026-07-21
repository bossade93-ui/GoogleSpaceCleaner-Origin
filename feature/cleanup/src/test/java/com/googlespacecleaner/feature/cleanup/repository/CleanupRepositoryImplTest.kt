package com.googlespacecleaner.feature.cleanup.repository

import com.googlespacecleaner.core.data.local.db.CleanupActionDao
import com.googlespacecleaner.core.data.local.db.ScannedItemDao
import com.googlespacecleaner.core.data.mapper.toEntity
import com.googlespacecleaner.core.domain.model.CleanupAction
import com.googlespacecleaner.core.domain.model.CleanupActionType
import com.googlespacecleaner.core.domain.model.CleanupStatus
import com.googlespacecleaner.core.domain.model.DataSource
import com.googlespacecleaner.core.domain.model.ScannedItem
import com.googlespacecleaner.core.network.drive.DriveApiService
import com.googlespacecleaner.core.network.drive.DriveFileDto
import com.googlespacecleaner.core.network.drive.TrashFileRequest
import com.googlespacecleaner.core.network.gmail.GmailApiService
import com.googlespacecleaner.core.network.gmail.GmailMessageDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CleanupRepositoryImplTest {

    private lateinit var driveApi: DriveApiService
    private lateinit var gmailApi: GmailApiService
    private lateinit var scannedItemDao: ScannedItemDao
    private lateinit var cleanupActionDao: CleanupActionDao
    private lateinit var repository: CleanupRepositoryImpl

    @Before
    fun setUp() {
        driveApi = mockk(relaxed = true)
        gmailApi = mockk(relaxed = true)
        scannedItemDao = mockk(relaxed = true)
        cleanupActionDao = mockk(relaxed = true)
        repository = CleanupRepositoryImpl(driveApi, gmailApi, scannedItemDao, cleanupActionDao)
    }

    private fun driveItem(id: String, size: Long = 1_000_000) = ScannedItem(
        id = id, source = DataSource.DRIVE, name = "file", sizeBytes = size,
        mimeType = "application/pdf", createdAt = 0, modifiedAt = 0,
        contentHash = null, thumbnailUrl = null
    )

    private fun gmailItem(id: String, size: Long = 500_000) = ScannedItem(
        id = id, source = DataSource.GMAIL, name = "email", sizeBytes = size,
        mimeType = "message/rfc822", createdAt = 0, modifiedAt = 0,
        contentHash = null, thumbnailUrl = null
    )

    private fun photoItem(id: String, size: Long = 2_000_000) = ScannedItem(
        id = id, source = DataSource.PHOTOS_TAKEOUT, name = "photo.jpg", sizeBytes = size,
        mimeType = "image/jpeg", createdAt = 0, modifiedAt = 0,
        contentHash = null, thumbnailUrl = null
    )

    @Test
    fun `drive-only cleanup calls trashFile and marks action as completed`() = runTest {
        val items = listOf(driveItem("d1"), driveItem("d2"))

        val result = repository.executeCleanup(items)

        assertTrue(result.isSuccess)
        val action = result.getOrThrow()
        assertEquals(CleanupStatus.COMPLETED, action.status)
        assertEquals(CleanupActionType.MOVE_TO_TRASH, action.actionType)
        assertEquals(1_500_000L, action.spaceFreedBytes)
        coVerify(exactly = 1) { driveApi.trashFile("d1") }
        coVerify(exactly = 1) { driveApi.trashFile("d2") }
        coVerify { scannedItemDao.deleteByIds(match { it.toSet() == setOf("d1", "d2") }) }
    }

    @Test
    fun `mixed drive and gmail cleanup calls both apis and sums freed bytes`() = runTest {
        val items = listOf(driveItem("d1", size = 1_000), gmailItem("g1", size = 2_000))

        val result = repository.executeCleanup(items)

        assertTrue(result.isSuccess)
        assertEquals(3_000L, result.getOrThrow().spaceFreedBytes)
        coVerify { driveApi.trashFile("d1") }
        coVerify { gmailApi.trashMessage("g1") }
    }

    @Test
    fun `photos-only cleanup does not call any api and is marked manual-only`() = runTest {
        val items = listOf(photoItem("p1"))

        val result = repository.executeCleanup(items)

        assertTrue(result.isSuccess)
        val action = result.getOrThrow()
        assertEquals(CleanupActionType.MANUAL_RECOMMENDATION_ONLY, action.actionType)
        assertEquals(CleanupStatus.PENDING, action.status)
        assertEquals(0L, action.spaceFreedBytes) // aucune API appelée, donc aucun octet "libéré" côté app
        coVerify(exactly = 0) { driveApi.trashFile(any()) }
        coVerify(exactly = 0) { gmailApi.trashMessage(any()) }
    }

    @Test
    fun `drive item mixed with photo items is not manual-only since drive action succeeded`() = runTest {
        val items = listOf(driveItem("d1"), photoItem("p1"))

        val result = repository.executeCleanup(items)

        val action = result.getOrThrow()
        assertEquals(CleanupActionType.MOVE_TO_TRASH, action.actionType)
        assertEquals(CleanupStatus.COMPLETED, action.status)
        // Le photo item reste tout de même tracé dans l'historique avec sa source.
        assertEquals(DataSource.PHOTOS_TAKEOUT, action.itemSources["p1"])
    }

    @Test
    fun `api failure marks action as failed and does not throw`() = runTest {
        coEvery { driveApi.trashFile("bad-id") } throws java.io.IOException("network error")
        val items = listOf(driveItem("bad-id"))

        val result = repository.executeCleanup(items)

        assertTrue(result.isFailure)
        coVerify { cleanupActionDao.insert(match { it.status == CleanupStatus.FAILED.name }) }
        // Le cache ne doit PAS être purgé pour un item dont la suppression a échoué.
        coVerify(exactly = 0) { scannedItemDao.deleteByIds(any()) }
    }

    @Test
    fun `undo on drive action calls trashFile with trashed=false`() = runTest {
        val action = CleanupAction(
            id = "action-1",
            itemSources = mapOf("d1" to DataSource.DRIVE),
            actionType = CleanupActionType.MOVE_TO_TRASH,
            timestamp = 0L,
            status = CleanupStatus.COMPLETED,
            spaceFreedBytes = 1_000
        )
        coEvery { cleanupActionDao.getById("action-1") } returns action.toEntity()

        val result = repository.undo("action-1")

        assertTrue(result.isSuccess)
        coVerify { driveApi.trashFile("d1", TrashFileRequest(trashed = false)) }
        coVerify { cleanupActionDao.updateStatus("action-1", CleanupStatus.UNDONE.name) }
    }

    @Test
    fun `undo on gmail action calls untrashMessage`() = runTest {
        val action = CleanupAction(
            id = "action-2",
            itemSources = mapOf("g1" to DataSource.GMAIL),
            actionType = CleanupActionType.MOVE_TO_TRASH,
            timestamp = 0L,
            status = CleanupStatus.COMPLETED,
            spaceFreedBytes = 500
        )
        coEvery { cleanupActionDao.getById("action-2") } returns action.toEntity()

        val result = repository.undo("action-2")

        assertTrue(result.isSuccess)
        coVerify { gmailApi.untrashMessage("g1") }
    }

    @Test
    fun `undo on manual-only photos action fails with explanatory message`() = runTest {
        val action = CleanupAction(
            id = "action-3",
            itemSources = mapOf("p1" to DataSource.PHOTOS_TAKEOUT),
            actionType = CleanupActionType.MANUAL_RECOMMENDATION_ONLY,
            timestamp = 0L,
            status = CleanupStatus.PENDING,
            spaceFreedBytes = 0
        )
        coEvery { cleanupActionDao.getById("action-3") } returns action.toEntity()

        val result = repository.undo("action-3")

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { driveApi.trashFile(any(), any()) }
    }

    @Test
    fun `undo on unknown action id fails with NoSuchElementException`() = runTest {
        coEvery { cleanupActionDao.getById("missing") } returns null

        val result = repository.undo("missing")

        assertTrue(result.exceptionOrNull() is NoSuchElementException)
    }
}
