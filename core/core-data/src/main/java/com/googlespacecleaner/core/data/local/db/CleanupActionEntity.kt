package com.googlespacecleaner.core.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cleanup_actions")
data class CleanupActionEntity(
    @PrimaryKey val id: String,
    /** Format : "id1:DRIVE,id2:GMAIL" — voir CleanupAction.itemSources pour le pourquoi. */
    val itemSourcesCsv: String,
    val actionType: String,
    val timestamp: Long,
    val status: String,
    val spaceFreedBytes: Long
)
