package com.googlespacecleaner.core.domain.repository

import com.googlespacecleaner.core.domain.model.ScannedItem
import kotlinx.coroutines.flow.StateFlow

/**
 * Transporte la sélection de l'utilisateur entre l'écran de scan (Drive/Photos/Gmail)
 * et l'écran de prévisualisation/suppression (feature:cleanup), sans passer par les
 * arguments de navigation (trop volumineux/complexes pour des objets ScannedItem).
 * Implémentation en mémoire uniquement (pas de persistance : une sélection ne doit
 * pas survivre à un redémarrage de l'app).
 */
interface SelectionRepository {
    val selection: StateFlow<List<ScannedItem>>
    fun setSelection(items: List<ScannedItem>)
    fun clear()
}
