package com.googlespacecleaner.feature.photosscan.takeout

import android.content.ContentResolver
import android.net.Uri
import com.googlespacecleaner.core.domain.model.DataSource
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class TakeoutImportRepositoryTest {

    private lateinit var contentResolver: ContentResolver
    private lateinit var repository: TakeoutImportRepository
    private val fakeUri: Uri = mockk()

    @Before
    fun setUp() {
        contentResolver = mockk()
        repository = TakeoutImportRepository(contentResolver)
    }

    /** Construit une archive ZIP en mémoire avec les entrées données (nom -> contenu). */
    private fun buildZip(entries: Map<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content)
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    @Test
    fun `import extracts only recognized media files, ignoring sidecar metadata`() = runTest {
        val zipBytes = buildZip(
            mapOf(
                "Takeout/Google Photos/Album/IMG_0001.jpg" to "photo-bytes".toByteArray(),
                "Takeout/Google Photos/Album/IMG_0001.jpg.supplemental-metadata.json" to "{}".toByteArray(),
                "Takeout/Google Photos/Album/clip.mp4" to "video-bytes".toByteArray()
            )
        )
        every { contentResolver.openInputStream(fakeUri) } returns ByteArrayInputStream(zipBytes)

        val completed = repository.importArchive(fakeUri).toList()
            .filterIsInstance<TakeoutImportProgress.Completed>().single()

        assertEquals(2, completed.items.size)
        assertTrue(completed.items.all { it.source == DataSource.PHOTOS_TAKEOUT })
        assertTrue(completed.items.none { it.name.endsWith(".json") })
    }

    @Test
    fun `identical file content produces identical hash for duplicate detection`() = runTest {
        val sameContent = "identical-photo-bytes".toByteArray()
        val zipBytes = buildZip(
            mapOf(
                "Takeout/Google Photos/A/photo1.jpg" to sameContent,
                "Takeout/Google Photos/B/photo2.jpg" to sameContent
            )
        )
        every { contentResolver.openInputStream(fakeUri) } returns ByteArrayInputStream(zipBytes)

        val completed = repository.importArchive(fakeUri).toList()
            .filterIsInstance<TakeoutImportProgress.Completed>().single()

        val hashes = completed.items.map { it.contentHash }.toSet()
        assertEquals(1, hashes.size)
    }

    @Test
    fun `item ids are prefixed with takeout_ so cleanup can distinguish sources`() = runTest {
        val zipBytes = buildZip(mapOf("Takeout/Google Photos/A/photo1.jpg" to "x".toByteArray()))
        every { contentResolver.openInputStream(fakeUri) } returns ByteArrayInputStream(zipBytes)

        val completed = repository.importArchive(fakeUri).toList()
            .filterIsInstance<TakeoutImportProgress.Completed>().single()

        assertTrue(completed.items.single().id.startsWith("takeout_"))
    }

    @Test
    fun `emits Failed when the file cannot be opened`() = runTest {
        every { contentResolver.openInputStream(fakeUri) } returns null

        val result = repository.importArchive(fakeUri).toList()

        assertTrue(result.last() is TakeoutImportProgress.Failed)
    }

    @Test
    fun `guesses correct mime types for supported extensions`() = runTest {
        val zipBytes = buildZip(
            mapOf(
                "a.png" to "x".toByteArray(),
                "b.heic" to "x".toByteArray(),
                "c.mov" to "x".toByteArray()
            )
        )
        every { contentResolver.openInputStream(fakeUri) } returns ByteArrayInputStream(zipBytes)

        val completed = repository.importArchive(fakeUri).toList()
            .filterIsInstance<TakeoutImportProgress.Completed>().single()

        val mimeByName = completed.items.associate { it.name to it.mimeType }
        assertEquals("image/png", mimeByName["a.png"])
        assertEquals("image/heic", mimeByName["b.heic"])
        assertEquals("video/quicktime", mimeByName["c.mov"])
    }
}
