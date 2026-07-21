package com.googlespacecleaner.feature.dashboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.googlespacecleaner.core.data.local.db.ScannedItemDao
import com.googlespacecleaner.core.data.mapper.toDomain
import com.googlespacecleaner.core.domain.model.DataSource
import com.googlespacecleaner.core.domain.model.ItemFlag
import com.googlespacecleaner.core.domain.repository.AuthRepository
import com.googlespacecleaner.core.domain.repository.AuthState
import com.googlespacecleaner.core.domain.repository.CleanupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val accountEmail: String? = null,
    val perSourceBytes: Map<DataSource, Long> = emptyMap(),
    val recoverableBytes: Long = 0L,
    val recentCleanupsCount: Int = 0,
    val isLoading: Boolean = true
) {
    val totalUsedBytes: Long get() = perSourceBytes.values.sum()
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val scannedItemDao: ScannedItemDao,
    private val cleanupRepository: CleanupRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.authState.collect { state ->
                if (state is AuthState.SignedIn) {
                    _uiState.value = _uiState.value.copy(accountEmail = state.account.email)
                }
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Drive, Photos et Gmail sont désormais tous mis en cache de façon
            // persistante. Les tailles Photos Picker peuvent être sous-estimées
            // (voir PhotosPickerRepository) ; Gmail n'a pas de détection de
            // doublons (pas de hash fourni par l'API, voir GmailRepositoryImpl).
            val driveItems = scannedItemDao.getBySource(DataSource.DRIVE.name).map { it.toDomain() }
            val photosItems = (
                scannedItemDao.getBySource(DataSource.PHOTOS_PICKER.name) +
                    scannedItemDao.getBySource(DataSource.PHOTOS_TAKEOUT.name)
                ).map { it.toDomain() }
            val gmailItems = scannedItemDao.getBySource(DataSource.GMAIL.name).map { it.toDomain() }

            val perSource = mapOf(
                DataSource.DRIVE to driveItems.sumOf { it.sizeBytes },
                DataSource.PHOTOS_PICKER to photosItems.sumOf { it.sizeBytes },
                DataSource.GMAIL to gmailItems.sumOf { it.sizeBytes }
            )

            val recoverable = (driveItems + photosItems + gmailItems)
                .filter { ItemFlag.DUPLICATE in it.flags }
                .sumOf { it.sizeBytes }

            val history = cleanupRepository.getHistory()

            _uiState.value = _uiState.value.copy(
                perSourceBytes = perSource,
                recoverableBytes = recoverable,
                recentCleanupsCount = history.size,
                isLoading = false
            )
        }
    }
}
