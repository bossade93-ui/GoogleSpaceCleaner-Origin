package com.googlespacecleaner.core.domain.usecase

import com.googlespacecleaner.core.domain.model.DataSource
import com.googlespacecleaner.core.domain.model.ItemFlag
import com.googlespacecleaner.core.domain.model.ScannedItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DetectDuplicatesUseCaseTest {

    private lateinit var useCase: DetectDuplicatesUseCase

    @Before
    fun setUp() {
        useCase = DetectDuplicatesUseCase()
    }

    private fun item(id: String, hash: String?, createdAt: Long) = ScannedItem(
        id = id,
        source = DataSource.DRIVE,
        name = "file_$id",
        sizeBytes = 1024,
        mimeType = "application/pdf",
        createdAt = createdAt,
        modifiedAt = createdAt,
        contentHash = hash,
        thumbnailUrl = null
    )

    @Test
    fun `marks all items except the oldest as duplicates when hashes match`() {
        val items = listOf(
            item("1", hash = "abc", createdAt = 100),
            item("2", hash = "abc", createdAt = 200),
            item("3", hash = "abc", createdAt = 50) // le plus ancien -> conservé
        )

        val result = useCase(items)

        val original = result.first { it.id == "3" }
        val duplicates = result.filter { it.id != "3" }

        assertTrue(original.flags.isEmpty())
        assertTrue(duplicates.all { ItemFlag.DUPLICATE in it.flags })
    }

    @Test
    fun `does not flag items with unique hashes`() {
        val items = listOf(
            item("1", hash = "abc", createdAt = 100),
            item("2", hash = "def", createdAt = 200)
        )

        val result = useCase(items)

        assertTrue(result.all { it.flags.isEmpty() })
    }

    @Test
    fun `ignores items without a hash`() {
        val items = listOf(
            item("1", hash = null, createdAt = 100),
            item("2", hash = null, createdAt = 200)
        )

        val result = useCase(items)

        assertEquals(0, result.count { ItemFlag.DUPLICATE in it.flags })
    }
}
