package com.asdevelopers.academy.core.repository

import com.asdevelopers.academy.core.backend.AcademyUserSyncGateway
import com.asdevelopers.academy.core.backend.AcademyUserSyncRecord
import com.asdevelopers.academy.core.database.SyncOutboxDao
import com.asdevelopers.academy.core.database.SyncOutboxEntity

/**
 * Persists small user-state mutations until the configured backend accepts them.
 * Course files and educational assets are never enqueued here.
 */
class UserSyncOutboxRepository(
    private val dao: SyncOutboxDao,
    private val gateway: AcademyUserSyncGateway,
    private val nowEpochMillis: () -> Long = System::currentTimeMillis
) {
    suspend fun enqueue(record: AcademyUserSyncRecord) {
        require(record.operationId.isNotBlank()) { "operationId is required" }
        require(record.courseId.isNotBlank()) { "courseId is required" }
        require(record.entityType.isNotBlank()) { "entityType is required" }
        require(record.entityId.isNotBlank()) { "entityId is required" }
        require(record.payloadJson.toByteArray(Charsets.UTF_8).size <= MAX_PAYLOAD_BYTES) {
            "sync payload exceeds $MAX_PAYLOAD_BYTES bytes; educational files must use content storage"
        }
        val now = nowEpochMillis()
        dao.upsert(
            SyncOutboxEntity(
                operationId = record.operationId,
                courseId = record.courseId,
                entityType = record.entityType,
                entityId = record.entityId,
                payloadJson = record.payloadJson,
                updatedAtIso8601 = record.updatedAtIso8601,
                deletedAtIso8601 = record.deletedAtIso8601,
                createdAtEpochMillis = now,
                attemptCount = 0,
                nextAttemptAtEpochMillis = now
            )
        )
    }

    suspend fun flush(limit: Int = 50): UserSyncFlushResult {
        val ready = dao.ready(nowEpochMillis(), limit.coerceIn(1, 100))
        if (ready.isEmpty()) return UserSyncFlushResult(queued = 0, accepted = 0, retryScheduled = false)

        val result = gateway.push(ready.map(SyncOutboxEntity::toRecord))
        val operationIds = ready.map { it.operationId }
        return when {
            result.accepted == ready.size -> {
                dao.delete(operationIds)
                UserSyncFlushResult(queued = ready.size, accepted = result.accepted, retryScheduled = false)
            }
            result.retryable -> {
                val highestAttempt = ready.maxOf { it.attemptCount } + 1
                val next = nowEpochMillis() + retryDelayMillis(highestAttempt)
                dao.scheduleRetry(operationIds, next)
                UserSyncFlushResult(
                    queued = ready.size,
                    accepted = result.accepted,
                    retryScheduled = true,
                    message = result.message
                )
            }
            else -> UserSyncFlushResult(
                queued = ready.size,
                accepted = result.accepted,
                retryScheduled = false,
                message = result.message
            )
        }
    }

    suspend fun pendingCount(): Int = dao.count()

    private fun retryDelayMillis(attempt: Int): Long {
        val exponent = (attempt - 1).coerceIn(0, 8)
        return (BASE_RETRY_MILLIS shl exponent).coerceAtMost(MAX_RETRY_MILLIS)
    }

    private companion object {
        const val MAX_PAYLOAD_BYTES = 64 * 1024
        const val BASE_RETRY_MILLIS = 30_000L
        const val MAX_RETRY_MILLIS = 60L * 60L * 1000L
    }
}

data class UserSyncFlushResult(
    val queued: Int,
    val accepted: Int,
    val retryScheduled: Boolean,
    val message: String? = null
)

private fun SyncOutboxEntity.toRecord() = AcademyUserSyncRecord(
    operationId = operationId,
    courseId = courseId,
    entityType = entityType,
    entityId = entityId,
    payloadJson = payloadJson,
    updatedAtIso8601 = updatedAtIso8601,
    deletedAtIso8601 = deletedAtIso8601
)
