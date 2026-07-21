package com.googlespacecleaner.core.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ScannedItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ScannedItemEntity>)

    @Query("SELECT * FROM scanned_items WHERE source = :source")
    suspend fun getBySource(source: String): List<ScannedItemEntity>

    @Query("SELECT * FROM scanned_items WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<ScannedItemEntity>

    @Query("DELETE FROM scanned_items WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM scanned_items WHERE source = :source")
    suspend fun clearSource(source: String)
}
