package com.asdevelopers.academy.core.backup

/**
 * Backup فقط داده‌های کاربر را شامل می‌شود؛ محتوای Course قابل بازیابی/دانلود است و نباید در Backup تکرار شود.
 */
data class AcademyBackup(
    val formatVersion: Int = 1,
    val createdAt: Long,
    val profileName: String?,
    val progressJson: String,
    val bookmarksJson: String,
    val quizResultsJson: String,
    val notesJson: String,
    val settingsJson: String
)

interface BackupCodec {
    fun encode(backup: AcademyBackup): ByteArray
    fun decode(bytes: ByteArray): AcademyBackup
}
