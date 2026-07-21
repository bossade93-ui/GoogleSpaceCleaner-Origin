package com.googlespacecleaner.feature.cleanup.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.googlespacecleaner.feature.cleanup.viewmodel.CleanupResult
import com.googlespacecleaner.feature.cleanup.viewmodel.CleanupViewModel

@Composable
fun CleanupPreviewScreen(
    onDone: () -> Unit,
    viewModel: CleanupViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showFirstConfirmation by remember { mutableStateOf(false) }
    var showFinalConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(state.result) {
        if (state.result is CleanupResult.Success) onDone()
    }

    Scaffold { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {

            Text(
                "${state.selectedItems.size} éléments sélectionnés — " +
                    "${state.totalSizeBytes / (1024 * 1024)} Mo à libérer",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(state.items, key = { it.id }) { item ->
                    ListItem(
                        headlineContent = { Text(item.name) },
                        supportingContent = { Text("${item.sizeBytes / (1024 * 1024)} Mo") },
                        leadingContent = {
                            Checkbox(
                                checked = item.id in state.checkedIds,
                                onCheckedChange = { viewModel.toggleItem(item.id) }
                            )
                        }
                    )
                }
            }

            when (val result = state.result) {
                is CleanupResult.InProgress -> CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
                is CleanupResult.Failure -> Text(
                    result.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp)
                )
                else -> Unit
            }

            Button(
                onClick = { showFirstConfirmation = true },
                enabled = state.selectedItems.isNotEmpty() && state.result !is CleanupResult.InProgress,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Text("Déplacer vers la corbeille")
            }
        }
    }

    // Première confirmation
    if (showFirstConfirmation) {
        AlertDialog(
            onDismissRequest = { showFirstConfirmation = false },
            title = { Text("Confirmer la suppression") },
            text = {
                Text(
                    "${state.selectedItems.size} éléments seront déplacés vers la corbeille. " +
                        "Cette action peut être annulée depuis Google Drive pendant 30 jours."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showFirstConfirmation = false
                    showFinalConfirmation = true
                }) { Text("Continuer") }
            },
            dismissButton = {
                TextButton(onClick = { showFirstConfirmation = false }) { Text("Annuler") }
            }
        )
    }

    // Seconde confirmation (double validation avant toute suppression, exigence du cahier des charges)
    if (showFinalConfirmation) {
        AlertDialog(
            onDismissRequest = { showFinalConfirmation = false },
            title = { Text("Dernière confirmation") },
            text = { Text("Êtes-vous certain(e) ? Cette action ne peut pas être annulée depuis l'application.") },
            confirmButton = {
                TextButton(onClick = {
                    showFinalConfirmation = false
                    viewModel.confirmDeletion()
                }) { Text("Confirmer la suppression") }
            },
            dismissButton = {
                TextButton(onClick = { showFinalConfirmation = false }) { Text("Annuler") }
            }
        )
    }
}
