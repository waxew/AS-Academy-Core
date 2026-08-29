package com.asdevelopers.academy.core.progress

/** وضعیت استاندارد مطالعه یک درس در تمام اپ‌های AS Academy. */
enum class LessonStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED,
    NEEDS_REVIEW
}

/**
 * مدل مشترک پیشرفت کاربر.
 * نگهداری این قرارداد در Core مانع ایجاد پیاده‌سازی متفاوت Progress در هر اپ می‌شود.
 */
data class LessonProgress(
    val lessonId: String,
    val status: LessonStatus = LessonStatus.NOT_STARTED,
    val progressPercent: Int = 0,
    val lastBlockIndex: Int = 0,
    val studySeconds: Long = 0,
    val lastOpenedAtEpochMillis: Long? = null,
    val completedAtEpochMillis: Long? = null
) {
    init {
        // درصد پیشرفت باید همیشه در بازه معتبر باشد تا UI و محاسبات سطح/دوره ناسازگار نشوند.
        require(progressPercent in 0..100) { "progressPercent must be between 0 and 100" }
        require(lastBlockIndex >= 0) { "lastBlockIndex cannot be negative" }
        require(studySeconds >= 0) { "studySeconds cannot be negative" }
    }
}
