package com.googlespacecleaner.core.network.drive.di

import com.googlespacecleaner.core.network.drive.DriveApiService
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
object DriveNetworkModule {

    private const val DRIVE_BASE_URL = "https://www.googleapis.com/drive/v3/"

    @Provides
    @Singleton
    fun provideDriveApiService(client: OkHttpClient): DriveApiService {
        return Retrofit.Builder()
            .baseUrl(DRIVE_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DriveApiService::class.java)
    }
}
