package com.googlespacecleaner.feature.photosscan.picker

import com.googlespacecleaner.core.network.photos.MediaFileDto
import com.googlespacecleaner.core.network.photos.PickedMediaItemDto
import com.googlespacecleaner.core.network.photos.PickedMediaItemsResponse
import com.googlespacecleaner.core.network.photos.PickerSessionDto
import com.googlespacecleaner.core.network.photos.PhotosPickerApiService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PhotosPickerRepositoryTest {

    private lateinit var pickerApi: PhotosPickerApiService
    private lateinit var repository: PhotosPickerRepository

    @Before
    fun setUp() {
        pickerApi = mockk()
        repository = PhotosPickerRepository(pickerApi)
    }

    @Test
    fun `emits AwaitingUserSelection immediately with the pickerUri`() = runTest {
        coEvery { pickerApi.createSession() } returns PickerSessionDto(
            id = "session-1", pickerUri = "https://photos.google.com/picker/session-1", mediaItemsSet = false
        )
        coEvery { pickerApi.getSession("session-1") } returns PickerSessionDto(
            id = "session-1", pickerUri = "https://photos.google.com/picker/session-1", mediaItemsSet = true
        )
        coEvery { pickerApi.listMediaItems("session-1", pageToken = null) } returns
            PickedMediaItemsResponse(mediaItems = emptyList(), nextPageToken = null)

        val firstState = repository.startPickerFlow().toList().first()

        assertTrue(firstState is PickerSessionState.AwaitingUserSelection)
        assertEquals(
            "https://photos.google.com/picker/session-1",
            (firstState as PickerSessionState.AwaitingUserSelection).pickerUri
        )
    }

    @Test
    fun `polls until mediaItemsSet becomes true, using virtual time`() = runTest {
        coEvery { pickerApi.createSession() } returns PickerSessionDto(
            id = "s1", pickerUri = "uri", mediaItemsSet = false
        )
        // Les deux premiers polls ne sont pas terminés, le troisième l'est.
        coEvery { pickerApi.getSession("s1") } returnsMany listOf(
            PickerSessionDto(id = "s1", pickerUri = "uri", mediaItemsSet = false),
            PickerSessionDto(id = "s1", pickerUri = "uri", mediaItemsSet = false),
            PickerSessionDto(id = "s1", pickerUri = "uri", mediaItemsSet = true)
        )
        coEvery { pickerApi.listMediaItems("s1", pageToken = null) } returns
            PickedMediaItemsResponse(mediaItems = emptyList(), nextPageToken = null)

        val states = repository.startPickerFlow().toList()

        assertTrue(states.last() is PickerSessionState.Completed)
    }

    @Test
    fun `emits TimedOut after the maximum number of polling attempts`() = runTest {
        coEvery { pickerApi.createSession() } returns PickerSessionDto(
            id = "s1", pickerUri = "uri", mediaItemsSet = false
        )
        // Ne devient jamais "true" : le polling doit s'arrêter après 120 tentatives
        // (temps virtuel : ce test s'exécute instantanément malgré les delay(5000) réels).
        coEvery { pickerApi.getSession("s1") } returns
            PickerSessionDto(id = "s1", pickerUri = "uri", mediaItemsSet = false)

        val states = repository.startPickerFlow().toList()

        assertTrue(states.last() is PickerSessionState.TimedOut)
    }

    @Test
    fun `maps selected media items, appending size hints to the thumbnail url`() = runTest {
        coEvery { pickerApi.createSession() } returns PickerSessionDto(
            id = "s1", pickerUri = "uri", mediaItemsSet = true
        )
        coEvery { pickerApi.listMediaItems("s1", pageToken = null) } returns PickedMediaItemsResponse(
            mediaItems = listOf(
                PickedMediaItemDto(
                    id = "media-1",
                    createTime = "2024-01-15T10:30:00Z",
                    type = "PHOTO",
                    mediaFile = MediaFileDto(
                        baseUrl = "https://photoslibrary.googleapis.com/abc",
                        mimeType = "image/jpeg",
                        filename = "IMG_0001.jpg",
                        mediaFileMetadata = null
                    )
                )
            ),
            nextPageToken = null
        )

        val completed = repository.startPickerFlow().toList()
            .filterIsInstance<PickerSessionState.Completed>().single()

        val item = completed.items.single()
        assertEquals("media-1", item.id)
        assertEquals("IMG_0001.jpg", item.name)
        assertEquals(0L, item.sizeBytes) // non fourni par l'API Picker, documenté
        assertTrue(item.thumbnailUrl!!.endsWith("=w200-h200"))
    }

    @Test
    fun `skips media items without a mediaFile instead of crashing`() = runTest {
        coEvery { pickerApi.createSession() } returns PickerSessionDto(
            id = "s1", pickerUri = "uri", mediaItemsSet = true
        )
        coEvery { pickerApi.listMediaItems("s1", pageToken = null) } returns PickedMediaItemsResponse(
            mediaItems = listOf(
                PickedMediaItemDto(id = "no-file", createTime = "2024-01-15T10:30:00Z", type = "PHOTO", mediaFile = null)
            ),
            nextPageToken = null
        )

        val completed = repository.startPickerFlow().toList()
            .filterIsInstance<PickerSessionState.Completed>().single()

        assertTrue(completed.items.isEmpty())
    }
}
