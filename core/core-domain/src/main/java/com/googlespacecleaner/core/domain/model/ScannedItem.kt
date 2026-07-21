package com.googlespacecleaner.core.domain.model

/**
 * Représente un élément détecté lors d'un scan (fichier Drive, photo/vidéo, ou email
 * avec pièce jointe). Modèle indépendant de toute source de données (Room, API...).
 */
data class ScannedItem(
    val id: String,
    val source: DataSource,
    val name: String,
    val sizeBytes: Long,
    val mimeType: String,
    val createdAt: Long,
    val modifiedAt: Long,
    val contentHash: String?,
    val thumbnailUrl: String?,
    val flags: Set<ItemFlag> = emptySet()
)

enum class DataSource {
    DRIVE, PHOTOS_PICKER, PHOTOS_TAKEOUT, GMAIL
}

enum class ItemFlag {
    DUPLICATE, LARGE_FILE, OLD_FILE, SCREENSHOT, UNUSED
}
