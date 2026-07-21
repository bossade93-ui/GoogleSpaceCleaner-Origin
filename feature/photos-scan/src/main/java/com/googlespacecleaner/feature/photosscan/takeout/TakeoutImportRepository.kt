package com.googlespacecleaner.feature.photosscan.takeout

import android.content.ContentResolver
import android.net.Uri
import com.googlespacecleaner.core.domain.model.DataSource
import com.googlespacecleaner.core.domain.model.ScannedItem
import com.googlespacecleaner.core.security.HashUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import javax.inject.Inject

sealed interface TakeoutImportProgress {
    data class InProgress(val entriesProcessed: Int) : TakeoutImportProgress
    data class Completed(val items: List<ScannedItem>) : TakeoutImportProgress
    data class Failed(val reason: String) : TakeoutImportProgress
}

/**
 * Analyse une archive ZIP exportée via Google Takeout (dossier "Google Photos").
 * Structure Takeout typique :
 *   Takeout/Google Photos/<Album>/IMG_0001.jpg
 *   Takeout/Google Photos/<Album>/IMG_0001.jpg.supplemental-metadata.json
 *
 * Le hash SHA-256 est calculé directement à partir des octets du fichier
 * pendant la lecture du flux ZIP (pas de fichier temporaire), ce qui permet
 * une détection de doublons fiable, contrairement au Picker qui n'expose
 * pas les octets du fichier.
 *
 * L'utilisateur sélectionne le(s) fichier(s) ZIP via le Storage Access
 * Framework (ACTION_OPEN_DOCUMENT côté UI) ; seul l'Uri est transmis ici.
 */
class TakeoutImportRepository @Inject constructor(
    private val contentResolver: ContentResolver
) {

    fun importArchive(zipUri: Uri): Flow<TakeoutImportProgress> = flow {
        val items = mutableListOf<ScannedItem>()
        var processed = 0

        try {
            contentResolver.openInputStream(zipUri)?.use { input ->
                ZipInputStream(input).use { zip ->
                    var entry: ZipEntry? = zip.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory && isMediaFile(entry.name)) {
                            val bytes = zip.readBytes()
                            val hash = HashUtils.sha256(bytes)

                            items += ScannedItem(
                                id = "takeout_${entry.name.hashCode()}",
                                source = DataSource.PHOTOS_TAKEOUT,
                                name = entry.name.substringAfterLast('/'),
                                sizeBytes = bytes.size.toLong(),
                                mimeType = guessMimeType(entry.name),
                                createdAt = entry.time,
                                modifiedAt = entry.time,
                                contentHash = hash,
                                thumbnailUrl = null // pas de miniature distante pour un import local
                            )
                            processed++
                            emit(TakeoutImportProgress.InProgress(processed))
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            } ?: run {
                emit(TakeoutImportProgress.Failed("Impossible d'ouvrir le fichier sélectionné."))
                return@flow
            }

            emit(TakeoutImportProgress.Completed(items))
        } catch (e: Exception) {
            emit(TakeoutImportProgress.Failed(e.message ?: "Erreur lors de la lecture de l'archive."))
        }
    }

    private fun isMediaFile(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
            lower.endsWith(".png") || lower.endsWith(".heic") ||
            lower.endsWith(".mp4") || lower.endsWith(".mov")
    }

    private fun guessMimeType(name: String): String = when {
        name.endsWith(".jpg", true) || name.endsWith(".jpeg", true) -> "image/jpeg"
        name.endsWith(".png", true) -> "image/png"
        name.endsWith(".heic", true) -> "image/heic"
        name.endsWith(".mp4", true) -> "video/mp4"
        name.endsWith(".mov", true) -> "video/quicktime"
        else -> "application/octet-stream"
    }
}
