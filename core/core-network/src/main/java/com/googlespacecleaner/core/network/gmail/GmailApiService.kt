package com.googlespacecleaner.core.network.gmail

import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Endpoints Gmail API v1 utilisés. Scope requis : gmail.readonly en V1 pour
 * la recherche, gmail.modify pour la mise à la corbeille (demandé de façon
 * incrémentale uniquement au moment d'une suppression, voir GoogleScopes).
 */
interface GmailApiService {

    /**
     * Recherche des messages via la syntaxe de recherche Gmail native.
     * Ex: "has:attachment larger:25M" pour les pièces jointes de plus de 25 Mo.
     */
    @GET("users/me/messages")
    suspend fun listMessages(
        @Query("q") query: String = "has:attachment larger:10M",
        @Query("maxResults") maxResults: Int = 100,
        @Query("pageToken") pageToken: String? = null
    ): GmailMessageListResponse

    @GET("users/me/messages/{messageId}")
    suspend fun getMessage(
        @Path("messageId") messageId: String,
        @Query("format") format: String = "metadata",
        @Query("metadataHeaders") headers: List<String> = listOf("Subject", "From")
    ): GmailMessageDto

    /** Déplace le message vers la corbeille Gmail (rétention 30 jours, comme Drive). */
    @POST("users/me/messages/{messageId}/trash")
    suspend fun trashMessage(@Path("messageId") messageId: String): GmailMessageDto

    /** Restaure un message depuis la corbeille (annulation, tant que non expiré). */
    @POST("users/me/messages/{messageId}/untrash")
    suspend fun untrashMessage(@Path("messageId") messageId: String): GmailMessageDto
}

data class GmailMessageListResponse(
    val messages: List<GmailMessageRefDto>?,
    val nextPageToken: String?,
    val resultSizeEstimate: Int
)

data class GmailMessageRefDto(
    val id: String,
    val threadId: String
)

data class GmailMessageDto(
    val id: String,
    val internalDate: String?, // timestamp en millisecondes, sous forme de String
    val sizeEstimate: Long?,
    val payload: GmailPayloadDto?
)

data class GmailPayloadDto(
    val headers: List<GmailHeaderDto>?
)

data class GmailHeaderDto(
    val name: String,
    val value: String
)
