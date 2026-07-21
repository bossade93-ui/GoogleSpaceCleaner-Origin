package com.googlespacecleaner.core.data.mapper

import com.googlespacecleaner.core.data.local.db.ScannedItemEntity
import com.googlespacecleaner.core.domain.model.DataSource
import com.googlespacecleaner.core.domain.model.ItemFlag
import com.googlespacecleaner.core.domain.model.ScannedItem

fun ScannedItem.toEntity(): ScannedItemEntity = ScannedItemEntity(
    id = id,
    source = source.name,
    name = name,
    sizeBytes = sizeBytes,
    mimeType = mimeType,
    createdAt = createdAt,
    modifiedAt = modifiedAt,
    contentHash = contentHash,
    thumbnailUrl = thumbnailUrl,
    flagsCsv = flags.joinToString(",") { it.name }
)

fun ScannedItemEntity.toDomain(): ScannedItem = ScannedItem(
    id = id,
    source = DataSource.valueOf(source),
    name = name,
    sizeBytes = sizeBytes,
    mimeType = mimeType,
    createdAt = createdAt,
    modifiedAt = modifiedAt,
    contentHash = contentHash,
    thumbnailUrl = thumbnailUrl,
    flags = flagsCsv.split(",")
        .filter { it.isNotBlank() }
        .map { ItemFlag.valueOf(it) }
        .toSet()
)
