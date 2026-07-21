package com.googlespacecleaner.feature.dashboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.googlespacecleaner.core.domain.model.DataSource
import com.googlespacecleaner.feature.dashboard.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    onOpenDrive: () -> Unit,
    onOpenPhotos: () -> Unit,
    onOpenGmail: () -> Unit,
    onOpenHistory: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {

            state.accountEmail?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }

            Text(
                "${state.totalUsedBytes / (1024 * 1024)} Mo utilisés au total",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )

            Text(
                "💡 Espace récupérable estimé : ${state.recoverableBytes / (1024 * 1024)} Mo",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (state.isLoading) {
                CircularProgressIndicator()
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ServiceCard(
                    modifier = Modifier.weight(1f),
                    title = "Drive",
                    sizeMb = (state.perSourceBytes[DataSource.DRIVE] ?: 0L) / (1024 * 1024),
                    onClick = onOpenDrive
                )
                ServiceCard(
                    modifier = Modifier.weight(1f),
                    title = "Photos",
                    sizeMb = (state.perSourceBytes[DataSource.PHOTOS_PICKER] ?: 0L) / (1024 * 1024),
                    onClick = onOpenPhotos
                )
            }

            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                ServiceCard(
                    modifier = Modifier.weight(1f),
                    title = "Gmail",
                    sizeMb = (state.perSourceBytes[DataSource.GMAIL] ?: 0L) / (1024 * 1024),
                    onClick = onOpenGmail
                )
            }

            TextButton(
                onClick = onOpenHistory,
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Text("Voir l'historique des nettoyages (${state.recentCleanupsCount})")
            }
        }
    }
}

@Composable
private fun ServiceCard(
    modifier: Modifier = Modifier,
    title: String,
    sizeMb: Long,
    onClick: () -> Unit
) {
    Card(modifier = modifier, onClick = onClick) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text("$sizeMb Mo", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
