package com.googlespacecleaner.core.data.local.selection

import com.googlespacecleaner.core.domain.model.ScannedItem
import com.googlespacecleaner.core.domain.repository.SelectionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemorySelectionRepository @Inject constructor() : SelectionRepository {

    private val _selection = MutableStateFlow<List<ScannedItem>>(emptyList())
    override val selection: StateFlow<List<ScannedItem>> = _selection.asStateFlow()

    override fun setSelection(items: List<ScannedItem>) {
        _selection.value = items
    }

    override fun clear() {
        _selection.value = emptyList()
    }
}
