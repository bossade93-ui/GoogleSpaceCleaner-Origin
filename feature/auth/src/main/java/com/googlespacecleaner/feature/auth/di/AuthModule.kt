package com.googlespacecleaner.feature.auth.di

import android.content.Context
import com.googlespacecleaner.core.domain.repository.AuthRepository
import com.googlespacecleaner.core.security.TokenManager
import com.googlespacecleaner.feature.auth.data.GoogleAuthRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideGoogleAuthRepository(
        @ApplicationContext context: Context,
        tokenManager: TokenManager
    ): GoogleAuthRepository = GoogleAuthRepository(context, tokenManager)
}

/**
 * Module séparé (classe abstraite) car @Binds nécessite une fonction abstraite,
 * incompatible avec un `object` porteur de @Provides concrets.
 * Permet aux autres modules feature (Drive, Photos, Gmail) d'injecter
 * AuthRepository (interface, core-domain) sans dépendre de feature:auth.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthBindsModule {

    @Binds
    abstract fun bindAuthRepository(impl: GoogleAuthRepository): AuthRepository
}
