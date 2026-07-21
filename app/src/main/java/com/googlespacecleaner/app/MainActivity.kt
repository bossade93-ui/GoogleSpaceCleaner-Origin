package com.googlespacecleaner.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.googlespacecleaner.app.navigation.AppNavGraph
import com.googlespacecleaner.core.ui.theme.GoogleSpaceCleanerTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activité unique de l'application (architecture single-activity).
 * Toute la navigation entre écrans est gérée par Jetpack Navigation Compose
 * via AppNavGraph.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GoogleSpaceCleanerTheme {
                AppNavGraph()
            }
        }
    }
}
