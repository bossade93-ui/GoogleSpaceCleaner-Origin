package com.googlespacecleaner.feature.drivescan.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
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
import com.googlespacecleaner.core.ui.components.ItemThumbnail
import com.googlespacecleaner.feature.drivescan.viewmodel.DriveFilter
import com.googlespacecleaner.feature.drivescan.viewmodel.DriveScanViewModel

@Composable
fun DriveScanScreen(
    onProceedToCleanup: () -> Unit,
    viewModel: DriveScanViewModel = hiltViewModel()
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
                    "Fichiers analysés : ${state.itemsScanned}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )
            }

            Text(
                "Espace récupérable estimé : ${state.recoverableSpaceBytes / (1024 * 1024)} Mo",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                FilterChip(
                    selected = state.activeFilter == DriveFilter.ALL,
                    onClick = { viewModel.setFilter(DriveFilter.ALL) },
                    label = { Text("Tous") }
                )
                FilterChip(
                    selected = state.activeFilter == DriveFilter.DUPLICATES,
                    onClick = { viewModel.setFilter(DriveFilter.DUPLICATES) },
                    label = { Text("Doublons") }
                )
                FilterChip(
                    selected = state.activeFilter == DriveFilter.LARGE,
                    onClick = { viewModel.setFilter(DriveFilter.LARGE) },
                    label = { Text("Volumineux") }
                )
                FilterChip(
                    selected = state.activeFilter == DriveFilter.OLD,
                    onClick = { viewModel.setFilter(DriveFilter.OLD) },
                    label = { Text("Anciens") }
                )
            }

            LazyColumn {
                items(state.filteredItems, key = { it.id }) { item ->
                    ListItem(
                        headlineContent = { Text(item.name) },
                        supportingContent = {
                            Text("${item.sizeBytes / (1024 * 1024)} Mo")
                        },
                        leadingContent = {
                            ItemThumbnail(thumbnailUrl = item.thumbnailUrl)
                        },
                        trailingContent = {
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
