package com.googlespacecleaner.core.network.gmail.di

import com.googlespacecleaner.core.network.gmail.GmailApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GmailNetworkModule {

    private const val GMAIL_BASE_URL = "https://gmail.googleapis.com/gmail/v1/"

    @Provides
    @Singleton
    fun provideGmailApiService(client: OkHttpClient): GmailApiService {
        return Retrofit.Builder()
            .baseUrl(GMAIL_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GmailApiService::class.java)
    }
}
