package com.googlespacecleaner.feature.gmailscan.repository

import com.googlespacecleaner.core.data.local.db.ScannedItemDao
import com.googlespacecleaner.core.domain.model.DataSource
import com.googlespacecleaner.core.domain.model.ItemFlag
import com.googlespacecleaner.core.domain.repository.ScanProgress
import com.googlespacecleaner.core.domain.usecase.DetectLargeAndOldFilesUseCase
import com.googlespacecleaner.core.network.gmail.GmailApiService
import com.googlespacecleaner.core.network.gmail.GmailHeaderDto
import com.googlespacecleaner.core.network.gmail.GmailMessageDto
import com.googlespacecleaner.core.network.gmail.GmailMessageListResponse
import com.googlespacecleaner.core.network.gmail.GmailMessageRefDto
import com.googlespacecleaner.core.network.gmail.GmailPayloadDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GmailRepositoryImplTest {

    private lateinit var gmailApi: GmailApiService
    private lateinit var dao: ScannedItemDao
    private lateinit var repository: GmailRepositoryImpl

    @Before
    fun setUp() {
        gmailApi = mockk()
        dao = mockk(relaxed = true)
        repository = GmailRepositoryImpl(gmailApi, dao, DetectLargeAndOldFilesUseCase())
    }

    @Test
    fun `scan fetches message details for each id returned by search`() = runTest {
        coEvery { gmailApi.listMessages(pageToken = null) } returns GmailMessageListResponse(
            messages = listOf(GmailMessageRefDto("msg1", "thread1")),
            nextPageToken = null,
            resultSizeEstimate = 1
        )
        coEvery { gmailApi.getMessage("msg1") } returns GmailMessageDto(
            id = "msg1",
            internalDate = "1700000000000",
            sizeEstimate = 15_000_000,
            payload = GmailPayloadDto(
                headers = listOf(
                    GmailHeaderDto("Subject", "Facture"),
                    GmailHeaderDto("From", "billing@example.com")
                )
            )
        )

        val completed = repository.scan(DataSource.GMAIL).toList()
            .filterIsInstance<ScanProgress.Completed>().single()

        val item = completed.items.single()
        assertEquals("msg1", item.id)
        assertTrue(item.name.contains("Facture"))
        assertEquals(15_000_000L, item.sizeBytes)
        assertNull(item.contentHash) // Gmail ne fournit aucun hash de contenu
    }

    @Test
    fun `scan flags messages above the lower gmail threshold as large`() = runTest {
        coEvery { gmailApi.listMessages(pageToken = null) } returns GmailMessageListResponse(
            messages = listOf(GmailMessageRefDto("msg1", "thread1")),
            nextPageToken = null,
            resultSizeEstimate = 1
        )
        coEvery { gmailApi.getMessage("msg1") } returns GmailMessageDto(
            id = "msg1", internalDate = "0", sizeEstimate = 12_000_000, payload = null
        )

        val completed = repository.scan(DataSource.GMAIL).toList()
            .filterIsInstance<ScanProgress.Completed>().single()

        assertTrue(ItemFlag.LARGE_FILE in completed.items.single().flags)
    }

    @Test
    fun `scan skips a message if fetching its details fails, without crashing`() = runTest {
        coEvery { gmailApi.listMessages(pageToken = null) } returns GmailMessageListResponse(
            messages = listOf(GmailMessageRefDto("bad-msg", "t"), GmailMessageRefDto("good-msg", "t")),
            nextPageToken = null,
            resultSizeEstimate = 2
        )
        coEvery { gmailApi.getMessage("bad-msg") } throws java.io.IOException("timeout")
        coEvery { gmailApi.getMessage("good-msg") } returns GmailMessageDto(
            id = "good-msg", internalDate = "0", sizeEstimate = 1_000, payload = null
        )

        val completed = repository.scan(DataSource.GMAIL).toList()
            .filterIsInstance<ScanProgress.Completed>().single()

        assertEquals(1, completed.items.size)
        assertEquals("good-msg", completed.items.single().id)
    }

    @Test
    fun `scan clears and repopulates the cache for GMAIL source`() = runTest {
        coEvery { gmailApi.listMessages(pageToken = null) } returns GmailMessageListResponse(
            messages = emptyList(), nextPageToken = null, resultSizeEstimate = 0
        )

        repository.scan(DataSource.GMAIL).toList()

        coVerify { dao.clearSource(DataSource.GMAIL.name) }
    }

    @Test
    fun `scan rejects a source other than GMAIL`() = runTest {
        try {
            repository.scan(DataSource.DRIVE).toList()
            org.junit.Assert.fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // attendu
        }
    }
}
