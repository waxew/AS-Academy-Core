package com.asdevelopers.academy.core.repository

import com.asdevelopers.academy.core.backend.AcademyUserSyncGateway
import com.asdevelopers.academy.core.backend.AcademyUserSyncPushResult
import com.asdevelopers.academy.core.backend.AcademyUserSyncRecord
import com.asdevelopers.academy.core.database.SyncOutboxDao
import com.asdevelopers.academy.core.database.SyncOutboxEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UserSyncOutboxRepositoryTest {
    @Test
    fun acceptedRecordsAreRemovedFromDurableQueue() = runBlocking {
        val dao = FakeOutboxDao()
        val gateway = object : AcademyUserSyncGateway {
            override suspend fun push(records: List<AcademyUserSyncRecord>) =
                AcademyUserSyncPushResult(accepted = records.size)

            override suspend fun pull(courseId: String, changedAfterIso8601: String?) = emptyList<AcademyUserSyncRecord>()
        }
        val repository = UserSyncOutboxRepository(dao, gateway) { 1_000L }
        repository.enqueue(record("op-1"))

        val result = repository.flush()

        assertEquals(1, result.accepted)
        assertEquals(0, repository.pendingCount())
    }

    @Test
    fun retryableFailureKeepsRecordAndSchedulesLaterAttempt() = runBlocking {
        val dao = FakeOutboxDao()
        val gateway = object : AcademyUserSyncGateway {
            override suspend fun push(records: List<AcademyUserSyncRecord>) =
                AcademyUserSyncPushResult(accepted = 0, retryable = true, message = "offline")

            override suspend fun pull(courseId: String, changedAfterIso8601: String?) = emptyList<AcademyUserSyncRecord>()
        }
        val repository = UserSyncOutboxRepository(dao, gateway) { 1_000L }
        repository.enqueue(record("op-2"))

        val result = repository.flush()

        assertTrue(result.retryScheduled)
        assertEquals(1, repository.pendingCount())
        assertTrue(dao.items.single().nextAttemptAtEpochMillis > 1_000L)
        assertEquals(1, dao.items.single().attemptCount)
    }

    @Test(expected = IllegalArgumentException::class)
    fun oversizedPayloadCannotTurnDatabaseIntoFileStorage() = runBlocking {
        val repository = UserSyncOutboxRepository(FakeOutboxDao(), NoopGateway) { 1_000L }
        repository.enqueue(record("op-big", payload = "x".repeat(65 * 1024)))
    }

    private fun record(operationId: String, payload: String = "{\"progress\":50}") = AcademyUserSyncRecord(
        operationId = operationId,
        courseId = "basic",
        entityType = "lesson_progress",
        entityId = "lesson-1",
        payloadJson = payload,
        updatedAtIso8601 = "2026-09-06T10:00:00Z"
    )

    private object NoopGateway : AcademyUserSyncGateway {
        override suspend fun push(records: List<AcademyUserSyncRecord>) = AcademyUserSyncPushResult(accepted = records.size)
        override suspend fun pull(courseId: String, changedAfterIso8601: String?) = emptyList<AcademyUserSyncRecord>()
    }

    private class FakeOutboxDao : SyncOutboxDao {
        val items = mutableListOf<SyncOutboxEntity>()

        override suspend fun ready(nowEpochMillis: Long, limit: Int): List<SyncOutboxEntity> =
            items.filter { it.nextAttemptAtEpochMillis <= nowEpochMillis }
                .sortedWith(compareBy<SyncOutboxEntity> { it.createdAtEpochMillis }.thenBy { it.operationId })
                .take(limit)

        override suspend fun upsert(entity: SyncOutboxEntity) {
            items.removeAll { it.operationId == entity.operationId }
            items += entity
        }

        override suspend fun delete(operationIds: List<String>) {
            items.removeAll { it.operationId in operationIds }
        }

        override suspend fun scheduleRetry(operationIds: List<String>, nextAttemptAtEpochMillis: Long) {
            val replacements = items.map { item ->
                if (item.operationId in operationIds) {
                    item.copy(
                        attemptCount = item.attemptCount + 1,
                        nextAttemptAtEpochMillis = nextAttemptAtEpochMillis
                    )
                } else item
            }
            items.clear()
            items += replacements
        }

        override suspend fun count(): Int = items.size
    }
}
