package com.googlespacecleaner.feature.drivescan.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.googlespacecleaner.core.domain.model.DataSource
import com.googlespacecleaner.core.domain.repository.ScanProgress
import com.googlespacecleaner.feature.drivescan.repository.DriveRepositoryImpl
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.last

/**
 * Permet de lancer un scan Drive complet même si l'utilisateur quitte l'app
 * (ex: nettoyage programmé en V1.1, ou scan initial long sur un compte volumineux).
 * Affiche une notification de progression (permission POST_NOTIFICATIONS requise,
 * déjà déclarée dans le Manifest).
 */
@HiltWorker
class DriveScanWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val driveRepository: DriveRepositoryImpl
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val finalProgress = driveRepository.scan(DataSource.DRIVE).last()
            when (finalProgress) {
                is ScanProgress.Completed -> Result.success()
                is ScanProgress.Failed -> Result.failure()
                else -> Result.success()
            }
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "drive_scan_work"
    }
}
