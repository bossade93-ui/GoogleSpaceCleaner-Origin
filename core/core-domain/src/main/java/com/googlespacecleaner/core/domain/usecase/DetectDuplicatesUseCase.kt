package com.googlespacecleaner.core.domain.usecase

import com.googlespacecleaner.core.domain.model.ItemFlag
import com.googlespacecleaner.core.domain.model.ScannedItem
import javax.inject.Inject

/**
 * Regroupe les éléments par empreinte de contenu (hash) et marque comme doublons
 * tous les éléments d'un groupe sauf le plus ancien (conservé comme "original").
 */
class DetectDuplicatesUseCase @Inject constructor() {

    operator fun invoke(items: List<ScannedItem>): List<ScannedItem> {
        val byHash = items.filter { it.contentHash != null }.groupBy { it.contentHash }

        val duplicateIds = byHash.values
            .filter { group -> group.size > 1 }
            .flatMap { group ->
                group.sortedBy { it.createdAt }.drop(1) // garde le plus ancien comme original
            }
            .map { it.id }
            .toSet()

        return items.map { item ->
            if (item.id in duplicateIds) {
                item.copy(flags = item.flags + ItemFlag.DUPLICATE)
            } else item
        }
    }
}
