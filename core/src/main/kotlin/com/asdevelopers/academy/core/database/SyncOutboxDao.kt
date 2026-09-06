package com.asdevelopers.academy.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SyncOutboxDao {
    @Query(
        "SELECT * FROM sync_outbox WHERE nextAttemptAtEpochMillis <= :nowEpochMillis ORDER BY createdAtEpochMillis, operationId LIMIT :limit"
    )
    suspend fun ready(nowEpochMillis: Long, limit: Int): List<SyncOutboxEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SyncOutboxEntity)

    @Query("DELETE FROM sync_outbox WHERE operationId IN (:operationIds)")
    suspend fun delete(operationIds: List<String>)

    @Query(
        "UPDATE sync_outbox SET attemptCount = attemptCount + 1, nextAttemptAtEpochMillis = :nextAttemptAtEpochMillis WHERE operationId IN (:operationIds)"
    )
    suspend fun scheduleRetry(operationIds: List<String>, nextAttemptAtEpochMillis: Long)

    @Query("SELECT COUNT(*) FROM sync_outbox")
    suspend fun count(): Int
}
