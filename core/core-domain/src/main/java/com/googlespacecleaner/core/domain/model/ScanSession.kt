package com.googlespacecleaner.core.domain.model

data class ScanSession(
    val id: String,
    val source: DataSource,
    val startedAt: Long,
    val completedAt: Long?,
    val itemsFound: Int,
    val spaceRecoverableBytes: Long
)

data class CleanupAction(
    val id: String,
    /** Association ID -> source, conservée même après suppression du cache Room,
     * car l'annulation (undo) doit savoir quelle API appeler (Drive vs Gmail)
     * pour chaque item, sans pouvoir redemander cette info au cache. */
    val itemSources: Map<String, DataSource>,
    val actionType: CleanupActionType,
    val timestamp: Long,
    val status: CleanupStatus,
    val spaceFreedBytes: Long
) {
    val itemIds: List<String> get() = itemSources.keys.toList()
}

enum class CleanupActionType {
    MOVE_TO_TRASH, MANUAL_RECOMMENDATION_ONLY
}

enum class CleanupStatus {
    PENDING, COMPLETED, FAILED, UNDONE
}
