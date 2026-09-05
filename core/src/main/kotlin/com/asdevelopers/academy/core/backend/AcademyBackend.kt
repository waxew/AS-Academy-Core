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
}
