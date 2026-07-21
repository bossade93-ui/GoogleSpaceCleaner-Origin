package com.googlespacecleaner.feature.drivescan.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.googlespacecleaner.core.domain.model.DataSource
import com.googlespacecleaner.core.domain.model.ItemFlag
import com.googlespacecleaner.core.domain.model.ScannedItem
import com.googlespacecleaner.core.domain.repository.AuthRepository
import com.googlespacecleaner.core.domain.repository.GoogleScopes
import com.googlespacecleaner.core.domain.repository.SelectionRepository
import com.googlespacecleaner.feature.drivescan.repository.DriveRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DriveFilter { ALL, DUPLICATES, LARGE, OLD }

data class DriveScanUiState(
    val isScanning: Boolean = false,
    val itemsScanned: Int = 0,
    val allItems: List<ScannedItem> = emptyList(),
    val activeFilter: DriveFilter = DriveFilter.ALL,
    val selectedIds: Set<String> = emptySet(),
    val error: String? = null
) {
    val filteredItems: List<ScannedItem>
        get() = when (activeFilter) {
            DriveFilter.ALL -> allItems
            DriveFilter.DUPLICATES -> allItems.filter { ItemFlag.DUPLICATE in it.flags }
            DriveFilter.LARGE -> allItems.filter { ItemFlag.LARGE_FILE in it.flags }
            DriveFilter.OLD -> allItems.filter { ItemFlag.OLD_FILE in it.flags }
        }

    val recoverableSpaceBytes: Long
        get() = allItems.filter { ItemFlag.DUPLICATE in it.flags }.sumOf { it.sizeBytes }
}

@HiltViewModel
class DriveScanViewModel @Inject constructor(
    private val driveRepository: DriveRepositoryImpl,
    private val authRepository: AuthRepository,
    private val selectionRepository: SelectionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DriveScanUiState())
    val uiState: StateFlow<DriveScanUiState> = _uiState.asStateFlow()

    init {
        loadCachedItems()
    }

    private fun loadCachedItems() {
        viewModelScope.launch {
            val cached = driveRepository.getCachedItems(DataSource.DRIVE)
            _uiState.value = _uiState.value.copy(allItems = cached)
        }
    }

    fun startScan() {
        viewModelScope.launch {
            if (!authRepository.hasScope(GoogleScopes.DRIVE_READONLY)) {
                val granted = authRepository.requestScope(GoogleScopes.DRIVE_READONLY)
                if (granted.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        error = "Autorisation Google Drive requise pour lancer l'analyse."
                    )
                    return@launch
                }
            }

            driveRepository.scan(DataSource.DRIVE).collect { progress ->
                _uiState.value = when (progress) {
                    is com.googlespacecleaner.core.domain.repository.ScanProgress.InProgress ->
                        _uiState.value.copy(isScanning = true, itemsScanned = progress.itemsScanned)

                    is com.googlespacecleaner.core.domain.repository.ScanProgress.Completed ->
                        _uiState.value.copy(isScanning = false, allItems = progress.items)

                    is com.googlespacecleaner.core.domain.repository.ScanProgress.Failed ->
                        _uiState.value.copy(isScanning = false, error = progress.reason)
                }
            }
        }
    }

    fun setFilter(filter: DriveFilter) {
        _uiState.value = _uiState.value.copy(activeFilter = filter)
    }

    fun toggleSelection(itemId: String) {
        val current = _uiState.value.selectedIds
        _uiState.value = _uiState.value.copy(
            selectedIds = if (itemId in current) current - itemId else current + itemId
        )
    }

    /** Transmet les items cochés à l'écran de prévisualisation/suppression. */
    fun proceedToCleanupWithSelection() {
        val selectedItems = _uiState.value.allItems.filter { it.id in _uiState.value.selectedIds }
        selectionRepository.setSelection(selectedItems)
    }
}
