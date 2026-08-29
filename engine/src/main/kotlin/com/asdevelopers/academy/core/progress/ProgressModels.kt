package com.asdevelopers.academy.core.progress

import kotlinx.serialization.Serializable

/** وضعیت استاندارد مطالعه یک درس در تمام اپ‌های AS Academy. */
@Serializable
enum class LessonStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED,
    NEEDS_REVIEW
}

/**
 * مدل مشترک پیشرفت کاربر؛ courseId از تداخل Stable IDها در نسخه All-in-One جلوگیری می‌کند.
 */
@Serializable
data class LessonProgress(
    val lessonId: String,
    val status: LessonStatus = LessonStatus.NOT_STARTED,
    val progressPercent: Int = 0,
    val lastBlockIndex: Int = 0,
    val studySeconds: Long = 0,
    val lastOpenedAtEpochMillis: Long? = null,
    val completedAtEpochMillis: Long? = null,
    val courseId: String = ""
) {
    init {
        // درصد خارج از بازه باعث نمایش و محاسبه نادرست در تمام مصرف‌کننده‌ها می‌شود.
        require(progressPercent in 0..100) { "progressPercent must be between 0 and 100" }
        // موقعیت آخرین Block و زمان مطالعه هیچ‌گاه منفی نیستند.
        require(lastBlockIndex >= 0) { "lastBlockIndex cannot be negative" }
        require(studySeconds >= 0) { "studySeconds cannot be negative" }
    }
}

/** خلاصه قابل نمایش پیشرفت یک فصل، سطح یا کل دوره. */
@Serializable
data class ProgressSummary(
    val totalLessons: Int,
    val completedLessons: Int,
    val inProgressLessons: Int,
    val needsReviewLessons: Int,
    val percent: Int
)
