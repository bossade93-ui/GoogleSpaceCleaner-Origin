package com.googlespacecleaner.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable
import com.googlespacecleaner.feature.auth.ui.LoginScreen
import com.googlespacecleaner.feature.drivescan.ui.DriveScanScreen
import com.googlespacecleaner.feature.photosscan.ui.PhotosScanScreen
import com.googlespacecleaner.feature.cleanup.ui.CleanupPreviewScreen
import com.googlespacecleaner.feature.dashboard.ui.DashboardScreen
import com.googlespacecleaner.feature.gmailscan.ui.GmailScanScreen
import com.googlespacecleaner.feature.history.ui.HistoryScreen

/**
 * Graphe de navigation racine de l'application.
 * Chaque route sera reliée à l'écran Compose du module feature correspondant
 * au fur et à mesure du développement de chaque module (étape 7 de la méthode).
 */
object AppRoutes {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val DRIVE_SCAN = "drive_scan"
    const val PHOTOS_SCAN = "photos_scan"
    const val GMAIL_SCAN = "gmail_scan"
    const val CLEANUP_PREVIEW = "cleanup_preview"
    const val HISTORY = "history"
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = AppRoutes.LOGIN) {
        composable(AppRoutes.LOGIN) {
            LoginScreen(
                onSignedIn = {
                    navController.navigate(AppRoutes.DASHBOARD) {
                        popUpTo(AppRoutes.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        composable(AppRoutes.DASHBOARD) {
            DashboardScreen(
                onOpenDrive = { navController.navigate(AppRoutes.DRIVE_SCAN) },
                onOpenPhotos = { navController.navigate(AppRoutes.PHOTOS_SCAN) },
                onOpenGmail = { navController.navigate(AppRoutes.GMAIL_SCAN) },
                onOpenHistory = { navController.navigate(AppRoutes.HISTORY) }
            )
        }
        composable(AppRoutes.DRIVE_SCAN) {
            DriveScanScreen(
                onProceedToCleanup = {
                    navController.navigate(AppRoutes.CLEANUP_PREVIEW)
                }
            )
        }
        composable(AppRoutes.PHOTOS_SCAN) {
            PhotosScanScreen()
        }
        composable(AppRoutes.GMAIL_SCAN) {
            GmailScanScreen(
                onProceedToCleanup = { navController.navigate(AppRoutes.CLEANUP_PREVIEW) }
            )
        }
        composable(AppRoutes.CLEANUP_PREVIEW) {
            CleanupPreviewScreen(
                onDone = { navController.popBackStack() }
            )
        }
        composable(AppRoutes.HISTORY) {
            HistoryScreen()
        }
    }
}
