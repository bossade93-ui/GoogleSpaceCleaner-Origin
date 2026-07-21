package com.googlespacecleaner.feature.drivescan.repository

import com.googlespacecleaner.core.data.local.db.ScannedItemDao
import com.googlespacecleaner.core.data.mapper.toDomain
import com.googlespacecleaner.core.data.mapper.toEntity
import com.googlespacecleaner.core.domain.model.DataSource
import com.googlespacecleaner.core.domain.model.ScanSession
import com.googlespacecleaner.core.domain.model.ScannedItem
import com.googlespacecleaner.core.domain.repository.ScanProgress
import com.googlespacecleaner.core.domain.repository.ScanRepository
import com.googlespacecleaner.core.domain.usecase.DetectDuplicatesUseCase
import com.googlespacecleaner.core.domain.usecase.DetectLargeAndOldFilesUseCase
import com.googlespacecleaner.core.network.drive.DriveApiService
import com.googlespacecleaner.core.network.drive.DriveFileDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

class DriveRepositoryImpl @Inject constructor(
    private val driveApi: DriveApiService,
    private val dao: ScannedItemDao,
    private val detectDuplicates: DetectDuplicatesUseCase,
    private val detectLargeAndOld: DetectLargeAndOldFilesUseCase
) : ScanRepository {

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)

    override fun scan(source: DataSource): Flow<ScanProgress> = flow {
        require(source == DataSource.DRIVE) { "DriveRepositoryImpl ne gère que DataSource.DRIVE" }

        val startedAt = System.currentTimeMillis()
        val allFiles = mutableListOf<DriveFileDto>()
        var pageToken: String? = null
        var page = 0

        do {
            val response = driveApi.listFiles(pageToken = pageToken)
            allFiles += response.files
            pageToken = response.nextPageToken
            page++
            emit(ScanProgress.InProgress(itemsScanned = allFiles.size, estimatedTotal = null))
        } while (pageToken != null)

        var items = allFiles.map { it.toDomainItem() }
        items = detectDuplicates(items)
        items = detectLargeAndOld(items)

        dao.clearSource(DataSource.DRIVE.name)
        dao.insertAll(items.map { it.toEntity() })

        val session = ScanSession(
            id = UUID.randomUUID().toString(),
            source = DataSource.DRIVE,
            startedAt = startedAt,
            completedAt = System.currentTimeMillis(),
            itemsFound = items.size,
            spaceRecoverableBytes = items
                .filter { com.googlespacecleaner.core.domain.model.ItemFlag.DUPLICATE in it.flags }
                .sumOf { it.sizeBytes }
        )

        emit(ScanProgress.Completed(session, items))
    }

    override suspend fun getCachedItems(source: DataSource): List<ScannedItem> =
        dao.getBySource(source.name).map { it.toDomain() }

    override suspend fun getScanSessions(source: DataSource): List<ScanSession> =
        emptyList() // Historique complet implémenté avec feature:history (étape suivante)

    private fun DriveFileDto.toDomainItem(): ScannedItem {
        val createdMillis = runCatching { isoFormat.parse(createdTime)?.time }.getOrNull()
            ?: System.currentTimeMillis()
        val modifiedMillis = runCatching { isoFormat.parse(modifiedTime)?.time }.getOrNull()
            ?: createdMillis

        return ScannedItem(
            id = id,
            source = DataSource.DRIVE,
            name = name,
            sizeBytes = size ?: 0L,
            mimeType = mimeType,
            createdAt = createdMillis,
            modifiedAt = modifiedMillis,
            contentHash = md5Checksum, // null pour les Google Docs/Sheets natifs
            thumbnailUrl = thumbnailLink
        )
    }
}
