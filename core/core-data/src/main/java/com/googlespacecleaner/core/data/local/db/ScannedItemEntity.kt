package com.googlespacecleaner.core.data.local.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entité Room stockée dans une base chiffrée (voir core-security/EncryptedStorageProvider).
 * Le champ `flags` est sérialisé en CSV simple (ex: "DUPLICATE,LARGE_FILE") via un TypeConverter.
 * Index sur `source` : chaque écran (Drive/Photos/Gmail) et le Dashboard filtrent
 * systématiquement par cette colonne via getBySource() — sans index, ces requêtes
 * dégénèrent en scan complet de table à mesure que le cache grossit.
 */
@Entity(
    tableName = "scanned_items",
    indices = [Index(value = ["source"])]
)
data class ScannedItemEntity(
    @PrimaryKey val id: String,
    val source: String,
    val name: String,
    val sizeBytes: Long,
    val mimeType: String,
    val createdAt: Long,
    val modifiedAt: Long,
    val contentHash: String?,
    val thumbnailUrl: String?,
    val flagsCsv: String
)
