package com.googlespacecleaner.feature.drivescan.repository

import com.googlespacecleaner.core.data.local.db.ScannedItemDao
import com.googlespacecleaner.core.data.local.db.ScannedItemEntity
import com.googlespacecleaner.core.domain.model.DataSource
import com.googlespacecleaner.core.domain.model.ItemFlag
import com.googlespacecleaner.core.domain.repository.ScanProgress
import com.googlespacecleaner.core.domain.usecase.DetectDuplicatesUseCase
import com.googlespacecleaner.core.domain.usecase.DetectLargeAndOldFilesUseCase
import com.googlespacecleaner.core.network.drive.DriveApiService
import com.googlespacecleaner.core.network.drive.DriveFileDto
import com.googlespacecleaner.core.network.drive.DriveFileListResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DriveRepositoryImplTest {

    private lateinit var driveApi: DriveApiService
    private lateinit var dao: ScannedItemDao
    private lateinit var repository: DriveRepositoryImpl

    private fun fileDto(
        id: String,
        size: Long? = 1_000_000,
        md5: String? = "hash1",
        createdTime: String = "2024-01-15T10:30:00.000Z"
    ) = DriveFileDto(
        id = id,
        name = "file_$id.pdf",
        size = size,
        mimeType = "application/pdf",
        createdTime = createdTime,
        modifiedTime = createdTime,
        md5Checksum = md5,
        thumbnailLink = null
    )

    @Before
    fun setUp() {
        driveApi = mockk()
        dao = mockk(relaxed = true)
        repository = DriveRepositoryImpl(
            driveApi, dao, DetectDuplicatesUseCase(), DetectLargeAndOldFilesUseCase()
        )
    }

    @Test
    fun `scan paginates until nextPageToken is null`() = runTest {
        coEvery { driveApi.listFiles(pageToken = null) } returns
            DriveFileListResponse(nextPageToken = "page2", files = listOf(fileDto("1")))
        coEvery { driveApi.listFiles(pageToken = "page2") } returns
            DriveFileListResponse(nextPageToken = null, files = listOf(fileDto("2")))

        val progresses = repository.scan(DataSource.DRIVE).toList()

        val completed = progresses.filterIsInstance<ScanProgress.Completed>().single()
        assertEquals(2, completed.items.size)
        coEvery { driveApi.listFiles(pageToken = "page2") }
    }

    @Test
    fun `scan maps DriveFileDto fields correctly into ScannedItem`() = runTest {
        coEvery { driveApi.listFiles(pageToken = null) } returns
            DriveFileListResponse(nextPageToken = null, files = listOf(fileDto("1", size = 500)))

        val completed = repository.scan(DataSource.DRIVE).toList()
            .filterIsInstance<ScanProgress.Completed>().single()

        val item = completed.items.single()
        assertEquals("1", item.id)
        assertEquals(DataSource.DRIVE, item.source)
        assertEquals(500L, item.sizeBytes)
        assertEquals("hash1", item.contentHash)
    }

    @Test
    fun `scan falls back to a safe timestamp when date parsing fails`() = runTest {
        coEvery { driveApi.listFiles(pageToken = null) } returns
            DriveFileListResponse(nextPageToken = null, files = listOf(fileDto("1", createdTime = "not-a-date")))

        val completed = repository.scan(DataSource.DRIVE).toList()
            .filterIsInstance<ScanProgress.Completed>().single()

        assertTrue(completed.items.single().createdAt > 0L)
    }

    @Test
    fun `scan detects duplicates by md5 hash across files`() = runTest {
        coEvery { driveApi.listFiles(pageToken = null) } returns
            DriveFileListResponse(
                nextPageToken = null,
                files = listOf(
                    fileDto("1", md5 = "sameHash"),
                    fileDto("2", md5 = "sameHash")
                )
            )

        val completed = repository.scan(DataSource.DRIVE).toList()
            .filterIsInstance<ScanProgress.Completed>().single()

        assertTrue(completed.items.any { ItemFlag.DUPLICATE in it.flags })
    }

    @Test
    fun `scan clears and repopulates the cache for DRIVE source only`() = runTest {
        coEvery { driveApi.listFiles(pageToken = null) } returns
            DriveFileListResponse(nextPageToken = null, files = listOf(fileDto("1")))

        repository.scan(DataSource.DRIVE).toList()

        coVerify { dao.clearSource(DataSource.DRIVE.name) }
        coVerify { dao.insertAll(match { it.size == 1 }) }
    }

    @Test
    fun `scan rejects a source other than DRIVE`() = runTest {
        try {
            repository.scan(DataSource.GMAIL).toList()
            org.junit.Assert.fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // attendu
        }
    }

    @Test
    fun `getCachedItems maps entities from the DAO`() = runTest {
        coEvery { dao.getBySource(DataSource.DRIVE.name) } returns listOf(
            ScannedItemEntity(
                id = "1", source = "DRIVE", name = "f", sizeBytes = 10,
                mimeType = "application/pdf", createdAt = 0, modifiedAt = 0,
                contentHash = null, thumbnailUrl = null, flagsCsv = ""
            )
        )

        val items = repository.getCachedItems(DataSource.DRIVE)

        assertEquals(1, items.size)
        assertEquals("1", items.first().id)
    }
}
