package com.asdevelopers.academy.core.backend

/** Public backend boundary owned by Core. Provider SDKs and protocols stay behind this API. */
interface AcademyBackend {
    val auth: AcademyAuthGateway
    val sync: AcademySyncGateway
    val storage: AcademyStorageGateway

    /** Optional authenticated user-data synchronization surface. */
    val userSync: AcademyUserSyncGateway
        get() = OfflineAcademyUserSyncGateway
}

interface AcademyAuthGateway {
    suspend fun currentUserId(): String?
}

/** Optional richer auth surface for backends that own authenticated sessions. */
interface AcademySessionAuthGateway : AcademyAuthGateway {
    suspend fun currentSession(): AcademyAuthSession?
    suspend fun signInWithPassword(email: String, password: String): AcademyAuthSession
    suspend fun refreshSession(): AcademyAuthSession?
    suspend fun signOut()
}

data class AcademyAuthSession(
    val userId: String,
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochSeconds: Long
) {
    fun isExpired(nowEpochSeconds: Long, skewSeconds: Long = 30L): Boolean =
        expiresAtEpochSeconds <= nowEpochSeconds + skewSeconds
}

interface AcademySyncGateway {
    suspend fun syncCourse(courseId: String): AcademySyncResult

    /**
     * Version-aware sync probe. Existing providers remain source compatible because the default
     * implementation delegates to the Foundation v1 method.
     */
    suspend fun syncCourse(
        courseId: String,
        localVersion: String?,
        localSha256: String?
    ): AcademySyncResult = syncCourse(courseId)
}

/**
 * Provider-neutral, idempotent user-data sync boundary. Payload stays opaque JSON so Core does not
 * couple progress/notes/bookmarks persistence to a provider-specific model.
 */
interface AcademyUserSyncGateway {
    suspend fun push(records: List<AcademyUserSyncRecord>): AcademyUserSyncPushResult
    suspend fun pull(courseId: String, changedAfterIso8601: String? = null): List<AcademyUserSyncRecord>
}

data class AcademyUserSyncRecord(
    val operationId: String,
    val courseId: String,
    val entityType: String,
    val entityId: String,
    val payloadJson: String,
    val updatedAtIso8601: String,
    val deletedAtIso8601: String? = null
)

data class AcademyUserSyncPushResult(
    val accepted: Int,
    val retryable: Boolean = false,
    val message: String? = null
)

interface AcademyStorageGateway {
    suspend fun resolveCourseContentUrl(courseId: String, version: String): String?
}

data class AcademySyncResult(
    val changed: Boolean,
    val message: String? = null,
    val remoteVersion: String? = null,
    val remoteSha256: String? = null
)

object OfflineAcademyUserSyncGateway : AcademyUserSyncGateway {
    override suspend fun push(records: List<AcademyUserSyncRecord>): AcademyUserSyncPushResult =
        AcademyUserSyncPushResult(
            accepted = 0,
            retryable = records.isNotEmpty(),
            message = "offline"
        )

    override suspend fun pull(courseId: String, changedAfterIso8601: String?): List<AcademyUserSyncRecord> =
        emptyList()
}

/** Safe default for offline-first Academy apps that have no remote backend configured. */
object OfflineAcademyBackend : AcademyBackend {
    override val auth: AcademyAuthGateway = object : AcademyAuthGateway {
        override suspend fun currentUserId(): String? = null
    }

    override val sync: AcademySyncGateway = object : AcademySyncGateway {
        override suspend fun syncCourse(courseId: String): AcademySyncResult =
            AcademySyncResult(changed = false, message = "offline")
    }

    override val storage: AcademyStorageGateway = object : AcademyStorageGateway {
        override suspend fun resolveCourseContentUrl(courseId: String, version: String): String? = null
    }

    override val userSync: AcademyUserSyncGateway = OfflineAcademyUserSyncGateway
}
