package com.googlespacecleaner.feature.cleanup.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.googlespacecleaner.core.domain.model.CleanupActionType
import com.googlespacecleaner.core.domain.model.ScannedItem
import com.googlespacecleaner.core.domain.repository.CleanupRepository
import com.googlespacecleaner.core.domain.repository.SelectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface CleanupResult {
    data object Idle : CleanupResult
    data object InProgress : CleanupResult
    data class Success(val spaceFreedBytes: Long, val requiresManualAction: Boolean) : CleanupResult
    data class Failure(val message: String) : CleanupResult
}

data class CleanupUiState(
    val items: List<ScannedItem> = emptyList(),
    val checkedIds: Set<String> = emptySet(),
    val result: CleanupResult = CleanupResult.Idle
) {
    val selectedItems: List<ScannedItem>
        get() = items.filter { it.id in checkedIds }

    val totalSizeBytes: Long
        get() = selectedItems.sumOf { it.sizeBytes }
}

@HiltViewModel
class CleanupViewModel @Inject constructor(
    private val selectionRepository: SelectionRepository,
    private val cleanupRepository: CleanupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CleanupUiState())
    val uiState: StateFlow<CleanupUiState> = _uiState.asStateFlow()

    init {
        // Charge la sélection transmise par l'écran Drive/Photos ; tout est
        // coché par défaut, l'utilisateur peut décocher avant confirmation.
        viewModelScope.launch {
            selectionRepository.selection.collect { items ->
                _uiState.value = _uiState.value.copy(
                    items = items,
                    checkedIds = items.map { it.id }.toSet()
                )
            }
        }
    }

    fun toggleItem(itemId: String) {
        val current = _uiState.value.checkedIds
        _uiState.value = _uiState.value.copy(
            checkedIds = if (itemId in current) current - itemId else current + itemId
        )
    }

    /** Lance réellement la suppression (après double confirmation côté UI). */
    fun confirmDeletion() {
        val selected = _uiState.value.selectedItems
        if (selected.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(result = CleanupResult.InProgress)

            val outcome = cleanupRepository.executeCleanup(selected)

            _uiState.value = _uiState.value.copy(
                result = outcome.fold(
                    onSuccess = { action ->
                        selectionRepository.clear()
                        CleanupResult.Success(
                            spaceFreedBytes = action.spaceFreedBytes,
                            requiresManualAction = action.actionType == CleanupActionType.MANUAL_RECOMMENDATION_ONLY
                        )
                    },
                    onFailure = { error ->
                        CleanupResult.Failure(error.message ?: "Erreur lors de la suppression.")
                    }
                )
            )
        }
    }
}
