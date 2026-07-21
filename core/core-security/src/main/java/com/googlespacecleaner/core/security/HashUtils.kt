package com.googlespacecleaner.core.security

import java.security.MessageDigest

/**
 * Calcule un hash SHA-256 pour les fichiers où l'API ne fournit pas de checksum
 * natif (ex: Google Docs/Sheets natifs, exports Takeout). Pour les fichiers Drive
 * binaires, le md5Checksum natif de l'API est préféré (voir DriveApiService).
 */
object HashUtils {

    fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
