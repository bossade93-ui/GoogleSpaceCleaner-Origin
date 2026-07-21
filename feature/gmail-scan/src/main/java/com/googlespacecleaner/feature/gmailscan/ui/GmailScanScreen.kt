package com.googlespacecleaner.feature.gmailscan.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.googlespacecleaner.feature.gmailscan.viewmodel.GmailScanViewModel

@Composable
fun GmailScanScreen(
    onProceedToCleanup: () -> Unit,
    viewModel: GmailScanViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            if (state.selectedIds.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    text = { Text("Nettoyer (${state.selectedIds.size})") },
                    icon = {},
                    onClick = {
                        viewModel.proceedToCleanupWithSelection()
                        onProceedToCleanup()
                    }
                )
            } else {
                ExtendedFloatingActionButton(
                    text = { Text(if (state.isScanning) "Analyse en cours…" else "Analyser") },
                    icon = {},
                    onClick = { viewModel.startScan() }
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {

            if (state.isScanning) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    "Messages analysés : ${state.itemsScanned}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )
            }

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
            }

            Text(
                "Pièces jointes volumineuses détectées : ${state.items.size}",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn {
                items(state.items, key = { it.id }) { item ->
                    ListItem(
                        headlineContent = { Text(item.name) },
                        supportingContent = { Text("${item.sizeBytes / (1024 * 1024)} Mo") },
                        leadingContent = {
                            Checkbox(
                                checked = item.id in state.selectedIds,
                                onCheckedChange = { viewModel.toggleSelection(item.id) }
                            )
                        }
                    )
                }
            }
        }
    }
}
