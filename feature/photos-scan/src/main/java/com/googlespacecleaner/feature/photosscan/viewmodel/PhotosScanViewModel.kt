package com.googlespacecleaner.feature.photosscan.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.googlespacecleaner.core.data.local.db.ScannedItemDao
import com.googlespacecleaner.core.data.mapper.toEntity
import com.googlespacecleaner.core.domain.model.ScannedItem
import com.googlespacecleaner.core.domain.repository.AuthRepository
import com.googlespacecleaner.core.domain.repository.GoogleScopes
import com.googlespacecleaner.core.domain.usecase.DetectDuplicatesUseCase
import com.googlespacecleaner.core.domain.usecase.DetectLargeAndOldFilesUseCase
import com.googlespacecleaner.feature.photosscan.picker.PhotosPickerRepository
import com.googlespacecleaner.feature.photosscan.picker.PickerSessionState
import com.googlespacecleaner.feature.photosscan.takeout.TakeoutImportProgress
import com.googlespacecleaner.feature.photosscan.takeout.TakeoutImportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PhotosScanUiState(
    val pickerUriToOpen: String? = null, // déclenche l'ouverture d'un Custom Tab côté UI
    val isProcessing: Boolean = false,
    val progressLabel: String? = null,
    val items: List<ScannedItem> = emptyList(),
    val error: String? = null
) {
    val recoverableSpaceBytes: Long
        get() = items.filter {
            com.googlespacecleaner.core.domain.model.ItemFlag.DUPLICATE in it.flags
        }.sumOf { it.sizeBytes }
}

@HiltViewModel
class PhotosScanViewModel @Inject constructor(
    private val pickerRepository: PhotosPickerRepository,
    private val takeoutRepository: TakeoutImportRepository,
    private val authRepository: AuthRepository,
    private val scannedItemDao: ScannedItemDao,
    private val detectDuplicates: DetectDuplicatesUseCase,
    private val detectLargeAndOld: DetectLargeAndOldFilesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PhotosScanUiState())
    val uiState: StateFlow<PhotosScanUiState> = _uiState.asStateFlow()

    /** Lance le flux "sélection rapide" via le Photos Picker. */
    fun launchPicker() {
        viewModelScope.launch {
            if (!authRepository.hasScope(GoogleScopes.PHOTOS_PICKER)) {
                val granted = authRepository.requestScope(GoogleScopes.PHOTOS_PICKER)
                if (granted.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        error = "Autorisation Google Photos requise pour utiliser le sélecteur."
                    )
                    return@launch
                }
            }

            pickerRepository.startPickerFlow().collect { state ->
                _uiState.value = when (state) {
                    is PickerSessionState.AwaitingUserSelection ->
                        _uiState.value.copy(pickerUriToOpen = state.pickerUri, isProcessing = true)

                    is PickerSessionState.Completed ->
                        _uiState.value.copy(
                            pickerUriToOpen = null,
                            isProcessing = false,
                            items = applyDetection(state.items)
                        )

                    is PickerSessionState.TimedOut ->
                        _uiState.value.copy(pickerUriToOpen = null, isProcessing = false, error = state.message)
                }
            }
        }
    }

    /** Lance l'import d'une archive Takeout sélectionnée via Storage Access Framework. */
    fun importTakeoutArchive(zipUri: Uri) {
        viewModelScope.launch {
            takeoutRepository.importArchive(zipUri).collect { progress ->
                _uiState.value = when (progress) {
                    is TakeoutImportProgress.InProgress ->
                        _uiState.value.copy(
                            isProcessing = true,
                            progressLabel = "${progress.entriesProcessed} fichiers analysés…"
                        )

                    is TakeoutImportProgress.Completed ->
                        _uiState.value.copy(
                            isProcessing = false,
                            progressLabel = null,
                            items = applyDetection(_uiState.value.items + progress.items)
                        )

                    is TakeoutImportProgress.Failed ->
                        _uiState.value.copy(isProcessing = false, progressLabel = null, error = progress.reason)
                }
            }
        }
    }

    /** Le Picker n'a pas de hash de contenu ; on ne réapplique la détection de doublons
     * que sur les items qui en ont (essentiellement Takeout), les items Picker sont
     * seulement passés dans la détection volumineux/ancien. */
    private fun applyDetection(items: List<ScannedItem>): List<ScannedItem> {
        val withDuplicates = detectDuplicates(items)
        val final = detectLargeAndOld(withDuplicates)
        viewModelScope.launch {
            // Mise en cache Room, symétrique à DriveRepositoryImpl, indispensable
            // pour que le Dashboard puisse calculer l'espace utilisé/récupérable
            // pour Photos sans devoir relancer un scan.
            scannedItemDao.insertAll(final.map { it.toEntity() })
        }
        return final
    }

    fun consumePickerUriHandled() {
        _uiState.value = _uiState.value.copy(pickerUriToOpen = null)
    }
}
