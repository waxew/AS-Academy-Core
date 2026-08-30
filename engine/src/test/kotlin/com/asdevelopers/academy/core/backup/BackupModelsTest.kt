package com.asdevelopers.academy.core.backup

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
    fun `schema one decodes with new collections empty`() {
        val legacy = json.decodeFromString<AcademyBackup>(
            """{"schemaVersion":1,"createdAtEpochMillis":123}"""
        )

        assertEquals(1, legacy.schemaVersion)
        assertTrue(legacy.learningCompletions.isEmpty())
        assertTrue(legacy.flashcardProgress.isEmpty())
    }

    @Test
    fun `schema two decodes without review progress`() {
        val previous = json.decodeFromString<AcademyBackup>(
            """{"schemaVersion":2,"createdAtEpochMillis":456,"achievements":[]}"""
        )

        assertEquals(2, previous.schemaVersion)
        assertTrue(previous.flashcardProgress.isEmpty())
    }

    @Test
    fun `new backups advertise schema three and preserve review progress`() {
        val progress = BackupFlashcardProgress(
            courseId = "sample",
            cardId = "flashcard-sample",
            repetitions = 2,
            intervalDays = 3,
            easeFactor = 2.5,
            lastReviewedEpochDay = 20_000,
            dueEpochDay = 20_003,
            updatedAtEpochMillis = 1_234_567
        )
        val backup = AcademyBackup(createdAtEpochMillis = 123, flashcardProgress = listOf(progress))
        val decoded = json.decodeFromString<AcademyBackup>(json.encodeToString(backup))

        assertEquals(3, decoded.schemaVersion)
        assertEquals(AcademyBackup.CURRENT_SCHEMA_VERSION, decoded.schemaVersion)
        assertEquals(progress, decoded.flashcardProgress.single())
    }
}
