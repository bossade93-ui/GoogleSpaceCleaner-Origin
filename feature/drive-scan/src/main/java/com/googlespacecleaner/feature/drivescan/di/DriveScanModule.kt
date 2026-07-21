package com.googlespacecleaner.feature.drivescan.di

import com.googlespacecleaner.core.data.local.db.ScannedItemDao
import com.googlespacecleaner.core.domain.usecase.DetectDuplicatesUseCase
import com.googlespacecleaner.core.domain.usecase.DetectLargeAndOldFilesUseCase
import com.googlespacecleaner.core.network.drive.DriveApiService
import com.googlespacecleaner.feature.drivescan.repository.DriveRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DriveScanModule {

    @Provides
    @Singleton
    fun provideDriveRepository(
        driveApi: DriveApiService,
        dao: ScannedItemDao,
        detectDuplicates: DetectDuplicatesUseCase,
        detectLargeAndOld: DetectLargeAndOldFilesUseCase
    ): DriveRepositoryImpl = DriveRepositoryImpl(driveApi, dao, detectDuplicates, detectLargeAndOld)
}
