package com.googlespacecleaner.feature.auth.di

import com.googlespacecleaner.core.domain.repository.AuthRepository
import com.googlespacecleaner.feature.auth.data.GoogleAuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * GoogleAuthRepository est instancié via son propre constructeur @Inject
 * (voir GoogleAuthRepository.kt) : il ne faut PAS ajouter ici de @Provides
 * qui le construirait une seconde fois, sous peine d'erreur de compilation
 * Dagger "GoogleAuthRepository is bound multiple times".
 * Ce module se limite donc au @Binds qui expose l'implémentation via
 * l'interface AuthRepository (core-domain), pour que les autres modules
 * feature (Drive, Photos, Gmail) puissent l'injecter sans dépendre de
 * feature:auth.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthBindsModule {

    @Binds
    abstract fun bindAuthRepository(impl: GoogleAuthRepository): AuthRepository
}
