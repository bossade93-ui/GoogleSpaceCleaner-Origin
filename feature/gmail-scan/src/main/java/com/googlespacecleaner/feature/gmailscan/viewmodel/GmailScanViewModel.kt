package com.googlespacecleaner.feature.gmailscan.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.googlespacecleaner.core.domain.model.DataSource
import com.googlespacecleaner.core.domain.model.ScannedItem
import com.googlespacecleaner.core.domain.repository.AuthRepository
import com.googlespacecleaner.core.domain.repository.GoogleScopes
import com.googlespacecleaner.core.domain.repository.ScanProgress
import com.googlespacecleaner.core.domain.repository.SelectionRepository
import com.googlespacecleaner.feature.gmailscan.repository.GmailRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GmailScanUiState(
    val isScanning: Boolean = false,
    val itemsScanned: Int = 0,
    val items: List<ScannedItem> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val error: String? = null
) {
    val recoverableSpaceBytes: Long get() = items.filter { it.id in selectedIds }.sumOf { it.sizeBytes }
}

@HiltViewModel
class GmailScanViewModel @Inject constructor(
    private val gmailRepository: GmailRepositoryImpl,
    private val authRepository: AuthRepository,
    private val selectionRepository: SelectionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GmailScanUiState())
    val uiState: StateFlow<GmailScanUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val cached = gmailRepository.getCachedItems(DataSource.GMAIL)
            _uiState.value = _uiState.value.copy(items = cached)
        }
    }

    fun startScan() {
        viewModelScope.launch {
            if (!authRepository.hasScope(GoogleScopes.GMAIL_READONLY)) {
                val granted = authRepository.requestScope(GoogleScopes.GMAIL_READONLY)
                if (granted.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        error = "Autorisation Gmail requise pour lancer l'analyse."
                    )
                    return@launch
                }
            }

            gmailRepository.scan(DataSource.GMAIL).collect { progress ->
                _uiState.value = when (progress) {
                    is ScanProgress.InProgress ->
                        _uiState.value.copy(isScanning = true, itemsScanned = progress.itemsScanned)
                    is ScanProgress.Completed ->
                        _uiState.value.copy(isScanning = false, items = progress.items)
                    is ScanProgress.Failed ->
                        _uiState.value.copy(isScanning = false, error = progress.reason)
                }
            }
        }
    }

    fun toggleSelection(itemId: String) {
        val current = _uiState.value.selectedIds
        _uiState.value = _uiState.value.copy(
            selectedIds = if (itemId in current) current - itemId else current + itemId
        )
    }

    fun proceedToCleanupWithSelection() {
        val selected = _uiState.value.items.filter { it.id in _uiState.value.selectedIds }
        selectionRepository.setSelection(selected)
    }
}
