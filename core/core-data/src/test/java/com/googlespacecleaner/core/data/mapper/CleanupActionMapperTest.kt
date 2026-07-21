package com.googlespacecleaner.core.data.mapper

import com.googlespacecleaner.core.domain.model.CleanupAction
import com.googlespacecleaner.core.domain.model.CleanupActionType
import com.googlespacecleaner.core.domain.model.CleanupStatus
import com.googlespacecleaner.core.domain.model.DataSource
import org.junit.Assert.assertEquals
import org.junit.Test

class CleanupActionMapperTest {

    @Test
    fun `round trip preserves item sources across mixed data sources`() {
        val original = CleanupAction(
            id = "action-1",
            itemSources = mapOf(
                "drive-file-1" to DataSource.DRIVE,
                "gmail-msg-1" to DataSource.GMAIL,
                "photo-1" to DataSource.PHOTOS_TAKEOUT
            ),
            actionType = CleanupActionType.MOVE_TO_TRASH,
            timestamp = 1_700_000_000_000,
            status = CleanupStatus.COMPLETED,
            spaceFreedBytes = 5_000_000
        )

        val roundTripped = original.toEntity().toDomain()

        assertEquals(original, roundTripped)
    }

    @Test
    fun `round trip handles a single item`() {
        val original = CleanupAction(
            id = "action-2",
            itemSources = mapOf("only-item" to DataSource.DRIVE),
            actionType = CleanupActionType.MOVE_TO_TRASH,
            timestamp = 0L,
            status = CleanupStatus.PENDING,
            spaceFreedBytes = 0L
        )

        val roundTripped = original.toEntity().toDomain()

        assertEquals(original, roundTripped)
    }

    @Test
    fun `itemIds derived property matches the keys of itemSources`() {
        val action = CleanupAction(
            id = "action-3",
            itemSources = mapOf("a" to DataSource.DRIVE, "b" to DataSource.GMAIL),
            actionType = CleanupActionType.MOVE_TO_TRASH,
            timestamp = 0L,
            status = CleanupStatus.COMPLETED,
            spaceFreedBytes = 0L
        )

        assertEquals(setOf("a", "b"), action.itemIds.toSet())
    }
}
