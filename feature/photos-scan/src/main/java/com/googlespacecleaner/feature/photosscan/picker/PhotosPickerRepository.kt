package com.googlespacecleaner.feature.photosscan.picker

import com.googlespacecleaner.core.domain.model.DataSource
import com.googlespacecleaner.core.domain.model.ScannedItem
import com.googlespacecleaner.core.network.photos.PhotosPickerApiService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

sealed interface PickerSessionState {
    data class AwaitingUserSelection(val pickerUri: String) : PickerSessionState
    data class Completed(val items: List<ScannedItem>) : PickerSessionState
    data class TimedOut(val message: String) : PickerSessionState
}

/**
 * Orchestration du flux Photos Picker :
 * 1. Crée une session et expose le pickerUri (à ouvrir dans un Custom Tab par l'UI)
 * 2. Poll la session jusqu'à ce que l'utilisateur ait terminé sa sélection
 * 3. Récupère et mappe les médias sélectionnés
 *
 * Note : la taille exacte des fichiers n'est pas fournie par l'API Picker
 * (contrairement à Drive) ; elle est estimée à 0 ici et pourra être affinée
 * par une requête HEAD sur mediaFile.baseUrl si nécessaire.
 */
class PhotosPickerRepository @Inject constructor(
    private val pickerApi: PhotosPickerApiService
) {
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)

    fun startPickerFlow(): Flow<PickerSessionState> = flow {
        val session = pickerApi.createSession()
        emit(PickerSessionState.AwaitingUserSelection(session.pickerUri))

        val maxAttempts = 120 // ~10 min avec un poll toutes les 5s
        var attempts = 0
        var currentSession = session

        while (!currentSession.mediaItemsSet && attempts < maxAttempts) {
            delay(5_000)
            currentSession = pickerApi.getSession(session.id)
            attempts++
        }

        if (!currentSession.mediaItemsSet) {
            emit(PickerSessionState.TimedOut("La sélection a expiré, veuillez réessayer."))
            return@flow
        }

        val items = mutableListOf<ScannedItem>()
        var pageToken: String? = null
        do {
            val page = pickerApi.listMediaItems(session.id, pageToken = pageToken)
            items += (page.mediaItems ?: emptyList()).mapNotNull { it.toDomainItemOrNull() }
            pageToken = page.nextPageToken
        } while (pageToken != null)

        emit(PickerSessionState.Completed(items))
    }

    private fun com.googlespacecleaner.core.network.photos.PickedMediaItemDto.toDomainItemOrNull(): ScannedItem? {
        val file = mediaFile ?: return null
        val createdMillis = runCatching { isoFormat.parse(createTime)?.time }.getOrNull()
            ?: System.currentTimeMillis()

        return ScannedItem(
            id = id,
            source = DataSource.PHOTOS_PICKER,
            name = file.filename,
            sizeBytes = 0L, // non fourni par l'API Picker
            mimeType = file.mimeType,
            createdAt = createdMillis,
            modifiedAt = createdMillis,
            contentHash = null, // pas de hash disponible sans télécharger le fichier
            thumbnailUrl = "${file.baseUrl}=w200-h200"
        )
    }
}
