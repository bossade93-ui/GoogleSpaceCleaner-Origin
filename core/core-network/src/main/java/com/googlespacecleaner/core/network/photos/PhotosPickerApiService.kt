package com.googlespacecleaner.core.network.photos

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * La Photos Picker API fonctionne par session :
 * 1. createSession() -> retourne un pickerUri à ouvrir (Custom Tab / navigateur)
 * 2. L'utilisateur choisit ses photos/albums dans l'UI Google Photos
 * 3. getSession() est interrogé périodiquement (polling) jusqu'à mediaItemsSet=true
 * 4. listMediaItems() retourne alors les éléments sélectionnés
 *
 * Scope requis : https://www.googleapis.com/auth/photospicker.mediaitems.readonly
 */
interface PhotosPickerApiService {

    @POST("v1/sessions")
    suspend fun createSession(): PickerSessionDto

    @GET("v1/sessions/{sessionId}")
    suspend fun getSession(@Path("sessionId") sessionId: String): PickerSessionDto

    @GET("v1/mediaItems")
    suspend fun listMediaItems(
        @Query("sessionId") sessionId: String,
        @Query("pageSize") pageSize: Int = 100,
        @Query("pageToken") pageToken: String? = null
    ): PickedMediaItemsResponse
}

data class PickerSessionDto(
    val id: String,
    val pickerUri: String,
    val mediaItemsSet: Boolean = false,
    val pollingConfig: PollingConfigDto? = null,
    val expireTime: String? = null
)

data class PollingConfigDto(
    val pollInterval: String?, // ex: "5s"
    val timeoutIn: String?
)

data class PickedMediaItemsResponse(
    val mediaItems: List<PickedMediaItemDto>?,
    val nextPageToken: String?
)

data class PickedMediaItemDto(
    val id: String,
    val createTime: String,
    val type: String, // "PHOTO" ou "VIDEO"
    val mediaFile: MediaFileDto?
)

data class MediaFileDto(
    val baseUrl: String,
    val mimeType: String,
    val filename: String,
    val mediaFileMetadata: MediaFileMetadataDto?
)

data class MediaFileMetadataDto(
    val width: Int?,
    val height: Int?
)
