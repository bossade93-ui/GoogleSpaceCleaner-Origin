package com.googlespacecleaner.feature.cleanup.di

import com.googlespacecleaner.core.data.local.db.CleanupActionDao
import com.googlespacecleaner.core.data.local.db.ScannedItemDao
import com.googlespacecleaner.core.domain.repository.CleanupRepository
import com.googlespacecleaner.core.network.drive.DriveApiService
import com.googlespacecleaner.core.network.gmail.GmailApiService
import com.googlespacecleaner.feature.cleanup.repository.CleanupRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CleanupModule {

    @Provides
    @Singleton
    fun provideCleanupRepository(
        driveApi: DriveApiService,
        gmailApi: GmailApiService,
        scannedItemDao: ScannedItemDao,
        cleanupActionDao: CleanupActionDao
    ): CleanupRepository = CleanupRepositoryImpl(driveApi, gmailApi, scannedItemDao, cleanupActionDao)
}
