package com.asdevelopers.academy.core.backend

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AcademyBackendContractTest {
    @Test
    fun offlineBackendExposesV2CapabilitiesWithoutBreakingV1Boundary() {
        val backend: AcademyBackend = OfflineAcademyBackend
        assertTrue(backend is AcademyBackendV2)
        assertTrue(backend.auth === OfflineAcademyBackend.authLifecycle)
        assertTrue(backend.sync === OfflineAcademyBackend.advancedSync)
        assertTrue(backend.storage === OfflineAcademyBackend.versionedStorage)
    }

    @Test
    fun syncRequestDefaultsToLatestWinsAndPreservesCursorInOutcomeModel() {
        val request = AcademySyncRequest(
            courseId = "basic",
            reason = AcademySyncReason.BACKGROUND,
            idempotencyKey = "sync-basic-1",
            cursor = "cursor-42"
        )
        assertEquals(AcademySyncConflictPolicy.LATEST_WINS, request.conflictPolicy)

        val outcome = AcademySyncOutcome(
            status = AcademySyncStatus.NO_CHANGES,
            changed = false,
            nextCursor = request.cursor
        )
        assertEquals("cursor-42", outcome.nextCursor)
        assertNull(outcome.message)
    }

    @Test
    fun storageMetadataCanCarryIntegrityAndExpiryHints() {
        val objectRef = AcademyStorageObject(
            url = "https://example.invalid/course.zip",
            sha256 = "a".repeat(64),
            sizeBytes = 1024,
            expiresAtEpochSeconds = 1_800_000_000
        )
        assertEquals(64, objectRef.sha256?.length)
        assertEquals(1024, objectRef.sizeBytes)
    }
}
