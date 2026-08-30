package com.asdevelopers.academy.core.backup

import com.asdevelopers.academy.core.flashcard.FlashcardReviewState
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** سازگاری Backup قدیمی باید بدون پایین‌آوردن شماره Schema فایل جدید حفظ شود. */
class BackupModelsTest {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun `schema one decodes with empty additive lists`() {
        val legacy = json.decodeFromString<AcademyBackup>(
            """{"schemaVersion":1,"createdAtEpochMillis":123}"""
        )

        assertEquals(1, legacy.schemaVersion)
        assertTrue(legacy.learningCompletions.isEmpty())
        assertTrue(legacy.flashcardReviewStates.isEmpty())
    }

    @Test
    fun `new backups advertise schema two`() {
        val backup = AcademyBackup(createdAtEpochMillis = 123)
        val decoded = json.decodeFromString<AcademyBackup>(json.encodeToString(backup))

        assertEquals(2, decoded.schemaVersion)
        assertEquals(AcademyBackup.CURRENT_SCHEMA_VERSION, decoded.schemaVersion)
    }

    @Test
    fun `flashcard review history survives backup round trip`() {
        val state = FlashcardReviewState(
            courseId = "sample",
            flashcardId = "sample-card",
            repetitions = 3,
            intervalDays = 8,
            easeFactor = 2.65,
            dueAtEpochMillis = 9_999L,
            lastReviewedAtEpochMillis = 1_234L
        )
        val backup = AcademyBackup(
            createdAtEpochMillis = 2_000L,
            flashcardReviewStates = listOf(state)
        )

        val decoded = json.decodeFromString<AcademyBackup>(json.encodeToString(backup))

        assertEquals(listOf(state), decoded.flashcardReviewStates)
    }
}
