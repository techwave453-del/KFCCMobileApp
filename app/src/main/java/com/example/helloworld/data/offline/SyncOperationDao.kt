package com.example.helloworld.data.offline

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SyncOperationDao {
    @Query("SELECT * FROM sync_operations WHERE status = 'pending' ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getPending(limit: Int = 50): List<SyncOperationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(operation: SyncOperationEntity)

    @Query("UPDATE sync_operations SET status = :status, attempts = :attempts, lastError = :lastError WHERE operationId = :operationId")
    suspend fun updateStatus(
        operationId: String,
        status: String,
        attempts: Int,
        lastError: String?
    )

    @Query("DELETE FROM sync_operations WHERE operationId = :operationId")
    suspend fun delete(operationId: String)
}
