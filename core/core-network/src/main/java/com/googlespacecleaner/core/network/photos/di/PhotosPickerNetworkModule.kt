package com.googlespacecleaner.core.network.photos.di

import com.googlespacecleaner.core.network.photos.PhotosPickerApiService
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
object PhotosPickerNetworkModule {

    private const val PICKER_BASE_URL = "https://photospicker.googleapis.com/"

    @Provides
    @Singleton
    fun providePhotosPickerApiService(client: OkHttpClient): PhotosPickerApiService {
        return Retrofit.Builder()
            .baseUrl(PICKER_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PhotosPickerApiService::class.java)
    }
}
