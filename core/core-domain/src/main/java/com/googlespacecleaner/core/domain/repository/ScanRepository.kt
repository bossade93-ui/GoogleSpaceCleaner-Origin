package com.googlespacecleaner.core.domain.repository

import com.googlespacecleaner.core.domain.model.DataSource
import com.googlespacecleaner.core.domain.model.ScanSession
import com.googlespacecleaner.core.domain.model.ScannedItem
import kotlinx.coroutines.flow.Flow

/**
 * Contrat commun pour toute source scannable (Drive, Photos, Gmail).
 * Chaque module feature fournit son implémentation concrète (ex: DriveRepositoryImpl
 * dans feature:drive-scan) en s'appuyant sur core-network et core-data.
 */
interface ScanRepository {

    /** Lance un scan et retourne un flux de progression + résultats. */
    fun scan(source: DataSource): Flow<ScanProgress>

    /** Retourne les éléments déjà en cache pour une source donnée. */
    suspend fun getCachedItems(source: DataSource): List<ScannedItem>

    /** Historique des sessions de scan. */
    suspend fun getScanSessions(source: DataSource): List<ScanSession>
}

sealed interface ScanProgress {
    data class InProgress(val itemsScanned: Int, val estimatedTotal: Int?) : ScanProgress
    data class Completed(val session: ScanSession, val items: List<ScannedItem>) : ScanProgress
    data class Failed(val reason: String) : ScanProgress
}
