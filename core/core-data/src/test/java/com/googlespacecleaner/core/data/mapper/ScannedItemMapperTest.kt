package com.googlespacecleaner.core.data.mapper

import com.googlespacecleaner.core.domain.model.DataSource
import com.googlespacecleaner.core.domain.model.ItemFlag
import com.googlespacecleaner.core.domain.model.ScannedItem
import org.junit.Assert.assertEquals
import org.junit.Test

class ScannedItemMapperTest {

    @Test
    fun `round trip preserves all fields including multiple flags`() {
        val original = ScannedItem(
            id = "abc123",
            source = DataSource.DRIVE,
            name = "rapport.pdf",
            sizeBytes = 204_800,
            mimeType = "application/pdf",
            createdAt = 1_700_000_000_000,
            modifiedAt = 1_700_100_000_000,
            contentHash = "d41d8cd98f00b204e9800998ecf8427e",
            thumbnailUrl = "https://example.com/thumb.jpg",
            flags = setOf(ItemFlag.DUPLICATE, ItemFlag.OLD_FILE)
        )

        val roundTripped = original.toEntity().toDomain()

        assertEquals(original, roundTripped)
    }

    @Test
    fun `round trip handles empty flags and null optional fields`() {
        val original = ScannedItem(
            id = "xyz789",
            source = DataSource.PHOTOS_TAKEOUT,
            name = "photo.jpg",
            sizeBytes = 1024,
            mimeType = "image/jpeg",
            createdAt = 0L,
            modifiedAt = 0L,
            contentHash = null,
            thumbnailUrl = null,
            flags = emptySet()
        )

        val roundTripped = original.toEntity().toDomain()

        assertEquals(original, roundTripped)
    }

    @Test
    fun `entity serializes flags as comma separated names`() {
        val item = ScannedItem(
            id = "1",
            source = DataSource.GMAIL,
            name = "email",
            sizeBytes = 100,
            mimeType = "message/rfc822",
            createdAt = 0L,
            modifiedAt = 0L,
            contentHash = null,
            thumbnailUrl = null,
            flags = setOf(ItemFlag.LARGE_FILE, ItemFlag.SCREENSHOT)
        )

        val entity = item.toEntity()

        assertEquals(setOf("LARGE_FILE", "SCREENSHOT"), entity.flagsCsv.split(",").toSet())
    }
}
