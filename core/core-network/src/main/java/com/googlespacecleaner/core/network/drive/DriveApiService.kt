package com.googlespacecleaner.core.network.drive

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Endpoints Drive API v3 utilisés. Scope requis : drive.readonly en V1
 * (drive.file en écriture uniquement lors d'une action de suppression explicite).
 */
interface DriveApiService {

    @GET("files")
    suspend fun listFiles(
        @Query("pageSize") pageSize: Int = 1000,
        @Query("pageToken") pageToken: String? = null,
        @Query("fields") fields: String =
            "nextPageToken, files(id, name, size, mimeType, createdTime, modifiedTime, md5Checksum, thumbnailLink)"
    ): DriveFileListResponse

    @GET("files/{fileId}")
    suspend fun getFileMetadata(@Path("fileId") fileId: String): DriveFileDto

    /**
     * Déplace un fichier vers la corbeille Drive (jamais de suppression
     * définitive directe). L'utilisateur peut le restaurer depuis Drive
     * pendant la période de rétention standard de Google (30 jours).
     */
    @PATCH("files/{fileId}")
    suspend fun trashFile(
        @Path("fileId") fileId: String,
        @Body body: TrashFileRequest = TrashFileRequest(trashed = true)
    ): DriveFileDto
}

data class TrashFileRequest(val trashed: Boolean)

data class DriveFileListResponse(
    val nextPageToken: String?,
    val files: List<DriveFileDto>
)

data class DriveFileDto(
    val id: String,
    val name: String,
    val size: Long?,
    val mimeType: String,
    val createdTime: String,
    val modifiedTime: String,
    val md5Checksum: String?,
    val thumbnailLink: String?
)
