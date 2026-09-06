package com.asdevelopers.academy.core.backend

/** Public backend boundary owned by Core. Provider SDKs (for example Supabase) stay behind this API. */
interface AcademyBackend {
    val auth: AcademyAuthGateway
    val sync: AcademySyncGateway
    val storage: AcademyStorageGateway
}

interface AcademyAuthGateway {
    suspend fun currentUserId(): String?
}

interface AcademySyncGateway {
    suspend fun syncCourse(courseId: String): AcademySyncResult
}

interface AcademyStorageGateway {
    suspend fun resolveCourseContentUrl(courseId: String, version: String): String?
}

data class AcademySyncResult(
    val changed: Boolean,
    val message: String? = null
)

/**
 * Optional production-grade backend capability set.
 *
 * This extends, rather than replaces, the Foundation v1 boundary so existing hosts/providers stay
 * source-compatible while remote providers can opt into explicit auth lifecycle, deterministic
 * sync semantics and versioned storage metadata.
 */
interface AcademyBackendV2 : AcademyBackend {
    val authLifecycle: AcademyAuthLifecycleGateway
    val advancedSync: AcademyAdvancedSyncGateway
    val versionedStorage: AcademyVersionedStorageGateway
}

data class AcademyAuthSession(
    val userId: String,
    val expiresAtEpochSeconds: Long? = null
)

sealed interface AcademyAuthResult {
    data class Authenticated(val session: AcademyAuthSession) : AcademyAuthResult
    data class Rejected(val reason: String) : AcademyAuthResult
    data class RetryableFailure(val reason: String) : AcademyAuthResult
}

interface AcademyAuthLifecycleGateway {
    suspend fun currentSession(): AcademyAuthSession?
    suspend fun signInWithEmail(email: String, password: String): AcademyAuthResult
    suspend fun refreshSession(): AcademyAuthResult
    suspend fun signOut()
}

enum class AcademySyncReason {
    APP_START,
    USER_REQUEST,
    CONTENT_OPEN,
    BACKGROUND
}

enum class AcademySyncConflictPolicy {
    SERVER_WINS,
    CLIENT_WINS,
    LATEST_WINS,
    MANUAL
}

data class AcademySyncRequest(
    val courseId: String,
    val reason: AcademySyncReason,
    val idempotencyKey: String,
    val cursor: String? = null,
    val conflictPolicy: AcademySyncConflictPolicy = AcademySyncConflictPolicy.LATEST_WINS
)

enum class AcademySyncStatus {
    NO_CHANGES,
    APPLIED_REMOTE,
    UPLOADED_LOCAL,
    CONFLICT,
    RETRYABLE_FAILURE,
    FATAL_FAILURE
}

data class AcademySyncOutcome(
    val status: AcademySyncStatus,
    val changed: Boolean,
    val nextCursor: String? = null,
    val message: String? = null
)

interface AcademyAdvancedSyncGateway {
    suspend fun sync(request: AcademySyncRequest): AcademySyncOutcome
}

data class AcademyStorageObject(
    val url: String,
    val sha256: String? = null,
    val sizeBytes: Long? = null,
    val expiresAtEpochSeconds: Long? = null
)

interface AcademyVersionedStorageGateway {
    suspend fun resolveCourseContent(courseId: String, version: String): AcademyStorageObject?
}

/** Safe default for offline-first Academy apps that have no remote backend configured. */
object OfflineAcademyBackend : AcademyBackendV2 {
    private object OfflineAuth : AcademyAuthGateway, AcademyAuthLifecycleGateway {
        override suspend fun currentUserId(): String? = null
        override suspend fun currentSession(): AcademyAuthSession? = null
        override suspend fun signInWithEmail(email: String, password: String): AcademyAuthResult =
            AcademyAuthResult.Rejected("offline")
        override suspend fun refreshSession(): AcademyAuthResult = AcademyAuthResult.Rejected("offline")
        override suspend fun signOut() = Unit
    }

    private object OfflineSync : AcademySyncGateway, AcademyAdvancedSyncGateway {
        override suspend fun syncCourse(courseId: String): AcademySyncResult =
            AcademySyncResult(changed = false, message = "offline")

        override suspend fun sync(request: AcademySyncRequest): AcademySyncOutcome =
            AcademySyncOutcome(
                status = AcademySyncStatus.NO_CHANGES,
                changed = false,
                nextCursor = request.cursor,
                message = "offline"
            )
    }

    private object OfflineStorage : AcademyStorageGateway, AcademyVersionedStorageGateway {
        override suspend fun resolveCourseContentUrl(courseId: String, version: String): String? = null
        override suspend fun resolveCourseContent(courseId: String, version: String): AcademyStorageObject? = null
    }

    override val auth: AcademyAuthGateway = OfflineAuth
    override val sync: AcademySyncGateway = OfflineSync
    override val storage: AcademyStorageGateway = OfflineStorage
    override val authLifecycle: AcademyAuthLifecycleGateway = OfflineAuth
    override val advancedSync: AcademyAdvancedSyncGateway = OfflineSync
    override val versionedStorage: AcademyVersionedStorageGateway = OfflineStorage
}
