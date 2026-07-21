package com.googlespacecleaner.feature.photosscan.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.googlespacecleaner.core.ui.components.ItemThumbnail
import com.googlespacecleaner.feature.photosscan.viewmodel.PhotosScanViewModel

@Composable
fun PhotosScanScreen(
    viewModel: PhotosScanViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val takeoutLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importTakeoutArchive(it) }
    }

    // Ouvre le pickerUri dans le navigateur/Custom Tab dès qu'une session est créée.
    LaunchedEffect(state.pickerUriToOpen) {
        state.pickerUriToOpen?.let { uri ->
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
            viewModel.consumePickerUriHandled()
        }
    }

    Scaffold { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {

            // Bannière explicative sur la limitation de l'API Google Photos
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Google limite l'accès automatique à votre photothèque. " +
                        "Choisissez une méthode d'analyse ci-dessous.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Row(modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                    onClick = { viewModel.launchPicker() }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Sélectionner des albums", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Rapide — choisissez les albums à analyser",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { takeoutLauncher.launch(arrayOf("application/zip")) }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Importer un export Takeout", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Analyse complète — nécessite un export préalable depuis takeout.google.com",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (state.isProcessing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 16.dp))
                state.progressLabel?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                }
            }

            if (state.items.isNotEmpty()) {
                Text(
                    "Espace récupérable estimé : ${state.recoverableSpaceBytes / (1024 * 1024)} Mo",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )

                LazyVerticalGrid(columns = GridCells.Fixed(3)) {
                    items(state.items, key = { it.id }) { item ->
                        Column(modifier = Modifier.padding(4.dp)) {
                            ItemThumbnail(
                                thumbnailUrl = item.thumbnailUrl,
                                modifier = Modifier.fillMaxWidth(),
                                size = 96.dp
                            )
                            Text(
                                item.name,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Bannière permanente rappelant la limitation de suppression
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Text(
                    "Suppression manuelle requise dans l'app Google Photos : " +
                        "l'API ne permet pas à cette application de supprimer des éléments " +
                        "qu'elle n'a pas créés.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
