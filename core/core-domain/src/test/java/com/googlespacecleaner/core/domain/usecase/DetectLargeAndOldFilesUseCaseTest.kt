package com.googlespacecleaner.core.domain.usecase

import com.googlespacecleaner.core.domain.model.DataSource
import com.googlespacecleaner.core.domain.model.ItemFlag
import com.googlespacecleaner.core.domain.model.ScannedItem
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class DetectLargeAndOldFilesUseCaseTest {

    private lateinit var useCase: DetectLargeAndOldFilesUseCase
    private val now = 1_700_000_000_000L // référence fixe pour des tests déterministes

    @Before
    fun setUp() {
        useCase = DetectLargeAndOldFilesUseCase()
    }

    private fun item(size: Long, modifiedAt: Long) = ScannedItem(
        id = "1",
        source = DataSource.DRIVE,
        name = "file",
        sizeBytes = size,
        mimeType = "application/pdf",
        createdAt = modifiedAt,
        modifiedAt = modifiedAt,
        contentHash = null,
        thumbnailUrl = null
    )

    @Test
    fun `flags files above the large threshold`() {
        val items = listOf(item(size = 200L * 1024 * 1024, modifiedAt = now))

        val result = useCase(items, nowMillis = now)

        assertTrue(ItemFlag.LARGE_FILE in result.first().flags)
    }

    @Test
    fun `does not flag files below the large threshold`() {
        val items = listOf(item(size = 10L * 1024 * 1024, modifiedAt = now))

        val result = useCase(items, nowMillis = now)

        assertFalse(ItemFlag.LARGE_FILE in result.first().flags)
    }

    @Test
    fun `flags files older than the threshold in months`() {
        val fourteenMonthsAgo = now - TimeUnit.DAYS.toMillis(14 * 30L)
        val items = listOf(item(size = 1024, modifiedAt = fourteenMonthsAgo))

        val result = useCase(items, oldThresholdMonths = 12, nowMillis = now)

        assertTrue(ItemFlag.OLD_FILE in result.first().flags)
    }

    @Test
    fun `does not flag recently modified files as old`() {
        val oneMonthAgo = now - TimeUnit.DAYS.toMillis(30L)
        val items = listOf(item(size = 1024, modifiedAt = oneMonthAgo))

        val result = useCase(items, oldThresholdMonths = 12, nowMillis = now)

        assertFalse(ItemFlag.OLD_FILE in result.first().flags)
    }
}
