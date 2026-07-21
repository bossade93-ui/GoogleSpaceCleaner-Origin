package com.googlespacecleaner.core.domain.repository

import com.googlespacecleaner.core.domain.model.CleanupAction
import com.googlespacecleaner.core.domain.model.ScannedItem

interface CleanupRepository {

    /**
     * Exécute une action de nettoyage sur les items fournis. Reçoit les
     * ScannedItem complets (et non de simples IDs) car leur source détermine
     * l'API à appeler (Drive = suppression réelle, Photos = recommandation
     * manuelle uniquement — voir Étape 3). Le cache Room ne contient que les
     * items Drive, donc la source ne peut pas être redérivée depuis le cache seul.
     */
    suspend fun executeCleanup(items: List<ScannedItem>): Result<CleanupAction>

    suspend fun getHistory(): List<CleanupAction>

    suspend fun undo(actionId: String): Result<Unit>
}
