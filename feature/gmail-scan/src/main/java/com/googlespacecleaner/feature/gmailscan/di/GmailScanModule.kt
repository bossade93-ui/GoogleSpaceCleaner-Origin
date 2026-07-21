package com.googlespacecleaner.feature.gmailscan.di

import com.googlespacecleaner.core.data.local.db.ScannedItemDao
import com.googlespacecleaner.core.domain.usecase.DetectLargeAndOldFilesUseCase
import com.googlespacecleaner.core.network.gmail.GmailApiService
import com.googlespacecleaner.feature.gmailscan.repository.GmailRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GmailScanModule {

    @Provides
    @Singleton
    fun provideGmailRepository(
        gmailApi: GmailApiService,
        dao: ScannedItemDao,
        detectLargeAndOld: DetectLargeAndOldFilesUseCase
    ): GmailRepositoryImpl = GmailRepositoryImpl(gmailApi, dao, detectLargeAndOld)
}
