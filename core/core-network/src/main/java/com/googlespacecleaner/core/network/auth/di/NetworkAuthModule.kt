package com.googlespacecleaner.core.network.auth.di

import com.googlespacecleaner.core.network.auth.OAuthInterceptor
import com.googlespacecleaner.core.network.auth.TokenProvider
import com.googlespacecleaner.core.security.TokenManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkAuthModule {

    @Provides
    @Singleton
    fun provideTokenProvider(tokenManager: TokenManager): TokenProvider =
        object : TokenProvider {
            override fun getCurrentAccessToken(): String =
                tokenManager.getAccessToken().orEmpty()

            override fun refreshAccessTokenBlocking(): String {
                // Implémentation complète : appel bloquant à GoogleAuthUtil.getToken()
                // avec le refresh token stocké. Simplifié ici pour le squelette.
                return tokenManager.getAccessToken().orEmpty()
            }
        }

    @Provides
    @Singleton
    fun provideOAuthInterceptor(tokenProvider: TokenProvider): OAuthInterceptor =
        OAuthInterceptor(tokenProvider)

    /**
     * Client OkHttp unique, partagé par tous les services Google (Drive, Gmail,
     * Photos Picker). Ne jamais en déclarer un second ailleurs : Hilt lèverait
     * une erreur de binding dupliqué (deux @Provides pour le même type non qualifié).
     */
    @Provides
    @Singleton
    fun provideSharedOkHttpClient(oAuthInterceptor: OAuthInterceptor): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            // BASIC uniquement : ne jamais logger les corps de requête/réponse
            // (métadonnées Drive, objets d'emails... = données personnelles).
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(oAuthInterceptor)
            .addInterceptor(logging)
            .build()
    }
}
