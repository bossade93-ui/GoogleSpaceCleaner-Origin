package com.googlespacecleaner.feature.history.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.googlespacecleaner.core.domain.model.CleanupActionType
import com.googlespacecleaner.core.domain.model.CleanupStatus
import com.googlespacecleaner.feature.history.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    Scaffold { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text("Historique des nettoyages", style = MaterialTheme.typography.headlineMedium)

            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
            }

            LazyColumn {
                items(state.actions, key = { it.id }) { action ->
                    val canUndo = action.status == CleanupStatus.COMPLETED &&
                        action.actionType == CleanupActionType.MOVE_TO_TRASH

                    ListItem(
                        headlineContent = {
                            Text("${action.itemIds.size} éléments — ${action.spaceFreedBytes / (1024 * 1024)} Mo")
                        },
                        supportingContent = {
                            Text(
                                "${dateFormat.format(Date(action.timestamp))} · " +
                                    statusLabel(action.status, action.actionType)
                            )
                        },
                        trailingContent = {
                            if (canUndo) {
                                TextButton(onClick = { viewModel.undo(action.id) }) {
                                    Text("Annuler")
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    state.undoError?.let { message ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissError() },
            title = { Text("Annulation impossible") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissError() }) { Text("OK") }
            }
        )
    }
}

private fun statusLabel(status: CleanupStatus, type: CleanupActionType): String = when {
    type == CleanupActionType.MANUAL_RECOMMENDATION_ONLY -> "Suppression manuelle recommandée (Photos)"
    status == CleanupStatus.COMPLETED -> "Terminé"
    status == CleanupStatus.FAILED -> "Échec"
    status == CleanupStatus.UNDONE -> "Annulé"
    else -> "En attente"
}
