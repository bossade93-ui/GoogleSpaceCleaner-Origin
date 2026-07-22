package com.googlespacecleaner.feature.auth.data

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.googlespacecleaner.core.security.TokenManager
import com.googlespacecleaner.core.domain.repository.AuthRepository
import com.googlespacecleaner.core.domain.repository.AuthState
import com.googlespacecleaner.core.domain.repository.GoogleAccountInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implémentation basée sur Google Sign-In (com.google.android.gms:play-services-auth).
 *
 * Important : au premier login, on ne demande QUE le scope de base (email/profil).
 * Les scopes sensibles (Drive, Gmail) sont demandés plus tard, un par un, via
 * requestScope() — appelé par chaque module feature au moment où il en a
 * réellement besoin (incremental auth), conformément à la politique Google.
 */
@Singleton
class GoogleAuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokenManager: TokenManager
) : AuthRepository {

    private val _authState = MutableStateFlow<AuthState>(AuthState.SignedOut)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val grantedScopes = mutableSetOf<String>()

    private val baseSignInOptions: GoogleSignInOptions =
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .build()

    private var signInClient: GoogleSignInClient =
        GoogleSignIn.getClient(context, baseSignInOptions)

    fun getSignInIntent(): Intent = signInClient.signInIntent

    /**
     * Appelé depuis l'Activity avec le résultat de l'ActivityResultContract
     * de connexion. Voir LoginScreen / LoginViewModel.
     */
    fun handleSignInResult(account: GoogleSignInAccount) {
        val info = GoogleAccountInfo(
            email = account.email.orEmpty(),
            displayName = account.displayName.orEmpty(),
            photoUrl = account.photoUrl?.toString()
        )
        account.serverAuthCode?.let { /* échangé côté client via GoogleAuthUtil si besoin */ }
        _authState.value = AuthState.SignedIn(info)
    }

    override suspend fun signIn(): Result<GoogleAccountInfo> {
        // La navigation Compose déclenche réellement l'intent via
        // rememberLauncherForActivityResult dans LoginScreen ; ici on expose
        // juste l'état courant pour les cas où l'utilisateur est déjà connecté.
        val existing = GoogleSignIn.getLastSignedInAccount(context)
        return if (existing != null) {
            handleSignInResult(existing)
            Result.success(
                GoogleAccountInfo(
                    email = existing.email.orEmpty(),
                    displayName = existing.displayName.orEmpty(),
                    photoUrl = existing.photoUrl?.toString()
                )
            )
        } else {
            Result.failure(IllegalStateException("Aucune session existante, lancer getSignInIntent()"))
        }
    }

    override suspend fun signOut() {
        signInClient.signOut()
        tokenManager.clear()
        grantedScopes.clear()
        _authState.value = AuthState.SignedOut
    }

    override suspend fun requestScope(scope: String): Result<Unit> {
        return try {
            // Reconstruit le client avec le scope additionnel et relance le
            // consentement — l'UI (LoginViewModel/CallerScreen) doit lancer
            // l'intent retourné par getSignInIntent() après cet appel.
            val updatedOptions = GoogleSignInOptions.Builder(baseSignInOptions)
                .requestScopes(com.google.android.gms.common.api.Scope(scope))
                .build()
            signInClient = GoogleSignIn.getClient(context, updatedOptions)
            grantedScopes.add(scope)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun hasScope(scope: String): Boolean = scope in grantedScopes
}
