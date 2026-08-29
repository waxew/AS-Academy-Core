package com.asdevelopers.academy.core.progress

import kotlinx.serialization.Serializable

/** نوع فعالیت‌هایی که تکمیل مستقل از مطالعه Lesson دارند. */
@Serializable
enum class LearningTargetType {
    EXERCISE,
    PROJECT
}

/** وضعیت تکمیل عمومی برای Dashboard و Achievement بدون وابستگی به Room. */
@Serializable
data class LearningCompletion(
    val courseId: String,
    val targetType: LearningTargetType,
    val targetId: String,
    val completed: Boolean,
    val completedAtEpochMillis: Long
) {
    init {
        // courseId خالی فقط برای خواندن/Backup رکوردهای Migration قدیمی مجاز است؛ Repository ذخیره جدید را رد می‌کند.
        require(targetId.isNotBlank()) { "targetId cannot be blank" }
        require(completedAtEpochMillis >= 0) { "completedAtEpochMillis cannot be negative" }
    }

    /** کلید شامل Course است تا ID یکسان دو دوره در Room با هم برخورد نکند. */
    val stableKey: String
        get() = "$courseId:${targetType.name}:$targetId"
}
