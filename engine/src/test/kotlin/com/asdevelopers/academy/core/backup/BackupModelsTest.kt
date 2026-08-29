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
    fun `schema one decodes with an empty completion list`() {
        val legacy = json.decodeFromString<AcademyBackup>(
            """{"schemaVersion":1,"createdAtEpochMillis":123}"""
        )

        assertEquals(1, legacy.schemaVersion)
        assertTrue(legacy.learningCompletions.isEmpty())
    }

    @Test
    fun `new backups advertise schema two`() {
        val backup = AcademyBackup(createdAtEpochMillis = 123)
        val decoded = json.decodeFromString<AcademyBackup>(json.encodeToString(backup))

        assertEquals(2, decoded.schemaVersion)
        assertEquals(AcademyBackup.CURRENT_SCHEMA_VERSION, decoded.schemaVersion)
    }
}
