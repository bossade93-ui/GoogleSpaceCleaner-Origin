package com.googlespacecleaner.core.data.mapper

import com.googlespacecleaner.core.data.local.db.CleanupActionEntity
import com.googlespacecleaner.core.domain.model.CleanupAction
import com.googlespacecleaner.core.domain.model.CleanupActionType
import com.googlespacecleaner.core.domain.model.CleanupStatus
import com.googlespacecleaner.core.domain.model.DataSource

fun CleanupAction.toEntity(): CleanupActionEntity = CleanupActionEntity(
    id = id,
    itemSourcesCsv = itemSources.entries.joinToString(",") { (itemId, source) -> "$itemId:${source.name}" },
    actionType = actionType.name,
    timestamp = timestamp,
    status = status.name,
    spaceFreedBytes = spaceFreedBytes
)

fun CleanupActionEntity.toDomain(): CleanupAction = CleanupAction(
    id = id,
    itemSources = itemSourcesCsv.split(",")
        .filter { it.isNotBlank() }
        .associate { pair ->
            val (itemId, source) = pair.split(":", limit = 2)
            itemId to DataSource.valueOf(source)
        },
    actionType = CleanupActionType.valueOf(actionType),
    timestamp = timestamp,
    status = CleanupStatus.valueOf(status),
    spaceFreedBytes = spaceFreedBytes
)
