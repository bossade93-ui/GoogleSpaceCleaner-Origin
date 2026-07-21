package com.googlespacecleaner.feature.cleanup.repository

import com.googlespacecleaner.core.data.local.db.CleanupActionDao
import com.googlespacecleaner.core.data.local.db.ScannedItemDao
import com.googlespacecleaner.core.data.mapper.toDomain
import com.googlespacecleaner.core.data.mapper.toEntity
import com.googlespacecleaner.core.domain.model.CleanupAction
import com.googlespacecleaner.core.domain.model.CleanupActionType
import com.googlespacecleaner.core.domain.model.CleanupStatus
import com.googlespacecleaner.core.domain.model.DataSource
import com.googlespacecleaner.core.domain.model.ScannedItem
import com.googlespacecleaner.core.domain.repository.CleanupRepository
import com.googlespacecleaner.core.network.drive.DriveApiService
import com.googlespacecleaner.core.network.drive.TrashFileRequest
import com.googlespacecleaner.core.network.gmail.GmailApiService
import java.util.UUID
import javax.inject.Inject

/**
 * Exécute une suppression en fonction de la source de chaque item :
 * - DRIVE : déplacement réel vers la corbeille via l'API Drive
 * - GMAIL : déplacement réel vers la corbeille via l'API Gmail
 * - PHOTOS_PICKER / PHOTOS_TAKEOUT : impossible via API (voir Étape 3), l'action
 *   est marquée MANUAL_RECOMMENDATION_ONLY et l'utilisateur doit supprimer
 *   lui-même dans Google Photos
 */
class CleanupRepositoryImpl @Inject constructor(
    private val driveApi: DriveApiService,
    private val gmailApi: GmailApiService,
    private val scannedItemDao: ScannedItemDao,
    private val cleanupActionDao: CleanupActionDao
) : CleanupRepository {

    override suspend fun executeCleanup(items: List<ScannedItem>): Result<CleanupAction> {
        val driveItems = items.filter { it.source == DataSource.DRIVE }
        val photoItems = items.filter {
            it.source == DataSource.PHOTOS_PICKER || it.source == DataSource.PHOTOS_TAKEOUT
        }
        val gmailItems = items.filter { it.source == DataSource.GMAIL }

        var freedBytes = 0L
        val successful = mutableMapOf<String, DataSource>()

        try {
            driveItems.forEach { item ->
                driveApi.trashFile(item.id)
                freedBytes += item.sizeBytes
                successful[item.id] = DataSource.DRIVE
            }
            gmailItems.forEach { item ->
                gmailApi.trashMessage(item.id)
                freedBytes += item.sizeBytes
                successful[item.id] = DataSource.GMAIL
            }
        } catch (e: Exception) {
            val partialAction = buildAction(
                itemSources = successful,
                status = CleanupStatus.FAILED,
                type = CleanupActionType.MOVE_TO_TRASH,
                freedBytes = freedBytes
            )
            cleanupActionDao.insert(partialAction.toEntity())
            return Result.failure(e)
        }

        // Les items supprimés avec succès (Drive + Gmail) sont retirés du cache local.
        if (successful.isNotEmpty()) {
            scannedItemDao.deleteByIds(successful.keys.toList())
        }

        val isManualOnly = photoItems.isNotEmpty() && driveItems.isEmpty() && gmailItems.isEmpty()
        // Les items Photos sont tout de même enregistrés dans l'historique (avec
        // leur source), même si aucune action API réelle n'a été effectuée dessus :
        // l'utilisateur doit les voir listés comme "à supprimer manuellement".
        val allSources = successful + photoItems.associate { it.id to it.source }

        val action = buildAction(
            itemSources = allSources,
            status = if (isManualOnly) CleanupStatus.PENDING else CleanupStatus.COMPLETED,
            type = if (isManualOnly) CleanupActionType.MANUAL_RECOMMENDATION_ONLY
                else CleanupActionType.MOVE_TO_TRASH,
            freedBytes = freedBytes
        )

        cleanupActionDao.insert(action.toEntity())
        return Result.success(action)
    }

    override suspend fun getHistory(): List<CleanupAction> =
        cleanupActionDao.getAll().map { it.toDomain() }

    override suspend fun undo(actionId: String): Result<Unit> {
        val entity = cleanupActionDao.getById(actionId)
            ?: return Result.failure(NoSuchElementException("Action introuvable : $actionId"))
        val action = entity.toDomain()

        if (action.actionType == CleanupActionType.MANUAL_RECOMMENDATION_ONLY) {
            return Result.failure(
                UnsupportedOperationException(
                    "Cette action concernait des éléments Photos supprimés manuellement " +
                        "par vous-même dans Google Photos : l'app ne peut pas les restaurer."
                )
            )
        }

        return try {
            action.itemSources.forEach { (itemId, source) ->
                when (source) {
                    DataSource.DRIVE -> driveApi.trashFile(itemId, TrashFileRequest(trashed = false))
                    DataSource.GMAIL -> gmailApi.untrashMessage(itemId)
                    DataSource.PHOTOS_PICKER, DataSource.PHOTOS_TAKEOUT -> Unit // jamais atteint ici
                }
            }
            cleanupActionDao.updateStatus(actionId, CleanupStatus.UNDONE.name)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildAction(
        itemSources: Map<String, DataSource>,
        status: CleanupStatus,
        type: CleanupActionType,
        freedBytes: Long
    ) = CleanupAction(
        id = UUID.randomUUID().toString(),
        itemSources = itemSources,
        actionType = type,
        timestamp = System.currentTimeMillis(),
        status = status,
        spaceFreedBytes = freedBytes
    )
}
