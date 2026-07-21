package com.googlespacecleaner.core.security.di

import android.content.Context
import com.googlespacecleaner.core.security.DbKeyProvider
import com.googlespacecleaner.core.security.TokenManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Source unique de vérité pour les providers Hilt de core-security.
 * Ne jamais dupliquer ces @Provides dans un autre module (ex: feature:auth
 * ou core-data) sous peine d'erreur de compilation "duplicate binding".
 */
@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideTokenManager(@ApplicationContext context: Context): TokenManager =
        TokenManager(context)

    @Provides
    @Singleton
    fun provideDbKeyProvider(@ApplicationContext context: Context): DbKeyProvider =
        DbKeyProvider(context)
}
