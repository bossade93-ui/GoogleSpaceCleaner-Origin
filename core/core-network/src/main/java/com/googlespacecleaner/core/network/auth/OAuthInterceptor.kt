package com.googlespacecleaner.core.network.auth

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Ajoute le token OAuth (lu via TokenManager, core-security) à chaque requête,
 * et déclenche un rafraîchissement automatique en cas de 401.
 * Les scopes demandés sont incrémentaux : chaque module feature ne déclenche
 * la demande de consentement que lorsqu'il en a besoin (ex: à l'ouverture
 * de l'onglet Drive), conformément aux bonnes pratiques Google.
 */
class OAuthInterceptor @Inject constructor(
    private val tokenProvider: TokenProvider
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = tokenProvider.getCurrentAccessToken()

        val authenticated = original.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        val response = chain.proceed(authenticated)

        if (response.code == 401) {
            response.close()
            val refreshed = tokenProvider.refreshAccessTokenBlocking()
            val retried = original.newBuilder()
                .header("Authorization", "Bearer $refreshed")
                .build()
            return chain.proceed(retried)
        }

        return response
    }
}

interface TokenProvider {
    fun getCurrentAccessToken(): String
    fun refreshAccessTokenBlocking(): String
}
