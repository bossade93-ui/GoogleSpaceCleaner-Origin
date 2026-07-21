package com.googlespacecleaner.core.data.local.db.di

import android.content.Context
import androidx.room.Room
import com.googlespacecleaner.core.data.local.db.AppDatabase
import com.googlespacecleaner.core.data.local.db.CleanupActionDao
import com.googlespacecleaner.core.data.local.db.ScannedItemDao
import com.googlespacecleaner.core.security.DbKeyProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        dbKeyProvider: DbKeyProvider
    ): AppDatabase {
        // Charge la bibliothèque native SQLCipher avant toute ouverture de base.
        SQLiteDatabase.loadLibs(context)

        val passphrase = dbKeyProvider.getOrCreatePassphrase()
        val factory = SupportFactory(passphrase)

        return Room.databaseBuilder(context, AppDatabase::class.java, "gs_cleaner.db")
            .openHelperFactory(factory)
            // TODO(avant publication) : remplacer par une vraie Migration(1, 2) dès
            // qu'une version est en production avec des utilisateurs réels. Acceptable
            // tant qu'aucune release publique n'existe (perte du cache local uniquement,
            // regénérable par un nouveau scan — aucune donnée Google n'est perdue).
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideScannedItemDao(db: AppDatabase): ScannedItemDao = db.scannedItemDao()

    @Provides
    @Singleton
    fun provideCleanupActionDao(db: AppDatabase): CleanupActionDao = db.cleanupActionDao()
}
