package com.googlespacecleaner.feature.history.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.googlespacecleaner.core.domain.model.CleanupAction
import com.googlespacecleaner.core.domain.repository.CleanupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val actions: List<CleanupAction> = emptyList(),
    val isLoading: Boolean = true,
    val undoError: String? = null
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val cleanupRepository: CleanupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val actions = cleanupRepository.getHistory()
            _uiState.value = _uiState.value.copy(actions = actions, isLoading = false)
        }
    }

    fun undo(actionId: String) {
        viewModelScope.launch {
            val result = cleanupRepository.undo(actionId)
            result.fold(
                onSuccess = { refresh() },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        undoError = error.message ?: "Impossible d'annuler cette action."
                    )
                }
            )
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(undoError = null)
    }
}
