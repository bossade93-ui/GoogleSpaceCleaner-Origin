package com.googlespacecleaner.feature.gmailscan.repository

import com.googlespacecleaner.core.data.local.db.ScannedItemDao
import com.googlespacecleaner.core.data.mapper.toDomain
import com.googlespacecleaner.core.data.mapper.toEntity
import com.googlespacecleaner.core.domain.model.DataSource
import com.googlespacecleaner.core.domain.model.ScanSession
import com.googlespacecleaner.core.domain.model.ScannedItem
import com.googlespacecleaner.core.domain.repository.ScanProgress
import com.googlespacecleaner.core.domain.repository.ScanRepository
import com.googlespacecleaner.core.domain.usecase.DetectLargeAndOldFilesUseCase
import com.googlespacecleaner.core.network.gmail.GmailApiService
import com.googlespacecleaner.core.network.gmail.GmailMessageDto
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.UUID
import javax.inject.Inject

/**
 * Recherche les messages Gmail avec pièces jointes volumineuses via la syntaxe
 * de recherche native ("has:attachment larger:X"). Gmail ne fournit pas de hash
 * de contenu pour les pièces jointes : la détection de doublons (par hash) ne
 * s'applique donc pas ici, seule la détection "volumineux/ancien" est pertinente.
 */
class GmailRepositoryImpl @Inject constructor(
    private val gmailApi: GmailApiService,
    private val dao: ScannedItemDao,
    private val detectLargeAndOld: DetectLargeAndOldFilesUseCase
) : ScanRepository {

    override fun scan(source: DataSource): Flow<ScanProgress> = flow {
        require(source == DataSource.GMAIL) { "GmailRepositoryImpl ne gère que DataSource.GMAIL" }

        val startedAt = System.currentTimeMillis()
        val messageRefs = mutableListOf<String>()
        var pageToken: String? = null

        do {
            val response = gmailApi.listMessages(pageToken = pageToken)
            response.messages?.forEach { messageRefs += it.id }
            pageToken = response.nextPageToken
            emit(ScanProgress.InProgress(itemsScanned = messageRefs.size, estimatedTotal = null))
        } while (pageToken != null)

        // L'API de recherche ne renvoie que des IDs : un appel détaillé par
        // message est nécessaire pour récupérer taille/objet/date. Ces appels
        // sont parallélisés (concurrence bornée à 10) plutôt que séquentiels :
        // pour 500 messages, cela passe d'environ 500 x latence réseau à
        // ~50 x latence réseau, sans dépasser le quota par utilisateur de
        // l'API Gmail (250 unités/seconde, chaque lecture coûtant 5 unités).
        val semaphore = Semaphore(permits = 10)
        val items = coroutineScope {
            messageRefs.map { id ->
                async {
                    semaphore.withPermit {
                        runCatching { gmailApi.getMessage(id).toDomainItem() }.getOrNull()
                    }
                }
            }.mapNotNull { it.await() }
        }

        val withFlags = detectLargeAndOld(
            items,
            largeThresholdBytes = 10L * 1024 * 1024 // seuil plus bas que Drive : 10 Mo
        )

        dao.clearSource(DataSource.GMAIL.name)
        dao.insertAll(withFlags.map { it.toEntity() })

        val session = ScanSession(
            id = UUID.randomUUID().toString(),
            source = DataSource.GMAIL,
            startedAt = startedAt,
            completedAt = System.currentTimeMillis(),
            itemsFound = withFlags.size,
            spaceRecoverableBytes = withFlags.sumOf { it.sizeBytes } // tout Gmail détecté = récupérable ici
        )

        emit(ScanProgress.Completed(session, withFlags))
    }

    override suspend fun getCachedItems(source: DataSource): List<ScannedItem> =
        dao.getBySource(source.name).map { it.toDomain() }

    override suspend fun getScanSessions(source: DataSource): List<ScanSession> = emptyList()

    private fun GmailMessageDto.toDomainItem(): ScannedItem {
        val subject = payload?.headers?.firstOrNull { it.name == "Subject" }?.value ?: "(sans objet)"
        val from = payload?.headers?.firstOrNull { it.name == "From" }?.value ?: ""
        val timestamp = internalDate?.toLongOrNull() ?: System.currentTimeMillis()

        return ScannedItem(
            id = id,
            source = DataSource.GMAIL,
            name = "$subject — $from",
            sizeBytes = sizeEstimate ?: 0L,
            mimeType = "message/rfc822",
            createdAt = timestamp,
            modifiedAt = timestamp,
            contentHash = null, // pas de hash fourni par Gmail, pas de détection de doublons ici
            thumbnailUrl = null
        )
    }
}
