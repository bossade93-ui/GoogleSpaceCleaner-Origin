package com.googlespacecleaner.core.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Base de données locale. L'ouverture se fait via SupportFactory (SQLCipher)
 * configuré dans core-security/EncryptedStorageProvider avec une clé stockée
 * dans EncryptedSharedPreferences — voir core:core-security.
 */
@Database(
    entities = [ScannedItemEntity::class, CleanupActionEntity::class],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scannedItemDao(): ScannedItemDao
    abstract fun cleanupActionDao(): CleanupActionDao
}
