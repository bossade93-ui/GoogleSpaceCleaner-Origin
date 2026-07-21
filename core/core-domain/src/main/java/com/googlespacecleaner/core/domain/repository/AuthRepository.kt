package com.googlespacecleaner.core.domain.repository

import kotlinx.coroutines.flow.StateFlow

/**
 * Contrat d'authentification, placé dans core-domain (et non feature:auth) afin
 * que les autres modules feature (Drive, Photos, Gmail) puissent déclencher une
 * demande de scope incrémentale sans créer de dépendance feature -> feature.
 * L'implémentation concrète (GoogleAuthRepository) reste dans feature:auth et
 * est liée à cette interface via @Binds dans feature/auth/di/AuthModule.
 */
sealed interface AuthState {
    data object SignedOut : AuthState
    data class SignedIn(val account: GoogleAccountInfo) : AuthState
    data class Error(val message: String) : AuthState
}

data class GoogleAccountInfo(
    val email: String,
    val displayName: String,
    val photoUrl: String?
)

interface AuthRepository {
    val authState: StateFlow<AuthState>

    suspend fun signIn(): Result<GoogleAccountInfo>
    suspend fun signOut()

    /** Demande un scope Google supplémentaire de façon incrémentale. */
    suspend fun requestScope(scope: String): Result<Unit>

    fun hasScope(scope: String): Boolean
}

object GoogleScopes {
    const val DRIVE_READONLY = "https://www.googleapis.com/auth/drive.readonly"
    const val GMAIL_READONLY = "https://www.googleapis.com/auth/gmail.readonly"
    const val DRIVE_FILE = "https://www.googleapis.com/auth/drive.file" // pour la suppression
    const val PHOTOS_PICKER = "https://www.googleapis.com/auth/photospicker.mediaitems.readonly"
}
