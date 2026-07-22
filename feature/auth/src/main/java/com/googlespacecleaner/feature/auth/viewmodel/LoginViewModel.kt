package com.googlespacecleaner.feature.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.googlespacecleaner.feature.auth.data.GoogleAuthRepository
import com.googlespacecleaner.core.domain.repository.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: GoogleAuthRepository
) : ViewModel() {

    val authState = authRepository.authState

    fun getSignInIntent() = authRepository.getSignInIntent()

    /** À appeler avec le résultat de l'ActivityResultContract StartActivityForResult. */
    fun onSignInResult(data: android.content.Intent?) {
        viewModelScope.launch {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account: GoogleSignInAccount = task.result
                authRepository.handleSignInResult(account)
            } catch (e: Exception) {
                authRepository.reportSignInFailure(
                    e.message ?: "La connexion Google a échoué. Veuillez réessayer."
                )
            }
        }
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }
}
