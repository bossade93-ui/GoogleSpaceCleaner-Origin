package com.googlespacecleaner.core.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CleanupActionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(action: CleanupActionEntity)

    @Query("SELECT * FROM cleanup_actions ORDER BY timestamp DESC")
    suspend fun getAll(): List<CleanupActionEntity>

    @Query("SELECT * FROM cleanup_actions WHERE id = :id")
    suspend fun getById(id: String): CleanupActionEntity?

    @Query("UPDATE cleanup_actions SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)
}
