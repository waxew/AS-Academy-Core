package com.asdevelopers.academy.core.backup

import com.asdevelopers.academy.core.exercise.ExerciseDraft
import com.asdevelopers.academy.core.progress.LessonProgress
import com.asdevelopers.academy.core.progress.LearningCompletion
import com.asdevelopers.academy.core.project.ProjectProgress
import kotlinx.serialization.Serializable

/** Bookmark مستقل از دیتابیس برای انتقال امن بین نسخه‌ها. */
@Serializable
data class BackupBookmark(
    val id: String,
    val courseId: String,
    val targetType: String,
    val targetId: String,
    val lessonId: String?,
    val createdAtEpochMillis: Long
)

/** یادداشت کاربر بخشی از داده غیرقابل‌بازسازی و الزامی Backup است. */
@Serializable
data class BackupNote(
    val id: String,
    val courseId: String,
    val lessonId: String,
    val blockId: String?,
    val text: String,
    val updatedAtEpochMillis: Long
)

/** تاریخچه آزمون برای تحلیل روند و نقاط ضعف پس از Restore حفظ می‌شود. */
@Serializable
data class BackupQuizResult(
    val attemptId: String,
    val courseId: String,
    val quizId: String,
    val scorePercent: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val weakTags: Set<String>,
    val completedAtEpochMillis: Long
)

/** نشان بازشده و زمان دریافت آن بخشی از سابقه یادگیری کاربر است. */
@Serializable
data class BackupAchievement(
    val courseId: String,
    val achievementId: String,
    val unlockedAtEpochMillis: Long
)

/**
 * وضعیت مرور Flashcard مستقل از Room نگهداری می‌شود تا Backup بین نسخه‌ها و دستگاه‌ها پایدار بماند.
 * courseId برای جلوگیری از برخورد Stable ID کارت‌ها بین Courseهای مختلف الزامی است.
 */
@Serializable
data class BackupFlashcardProgress(
    val courseId: String,
    val cardId: String,
    val repetitions: Int,
    val intervalDays: Int,
    val easeFactor: Double,
    val lastReviewedEpochDay: Long? = null,
    val dueEpochDay: Long,
    val updatedAtEpochMillis: Long
)

/** قالب نسخه‌دار Backup؛ Search Index عمداً ذخیره نمی‌شود چون از Course قابل بازسازی است. */
@Serializable
data class AcademyBackup(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val createdAtEpochMillis: Long,
    val lessonProgress: List<LessonProgress> = emptyList(),
    val learningCompletions: List<LearningCompletion> = emptyList(),
    val bookmarks: List<BackupBookmark> = emptyList(),
    val notes: List<BackupNote> = emptyList(),
    val quizResults: List<BackupQuizResult> = emptyList(),
    val exerciseDrafts: List<ExerciseDraft> = emptyList(),
    val projectProgress: List<ProjectProgress> = emptyList(),
    val achievements: List<BackupAchievement> = emptyList(),
    /** نسخه‌های Backup قدیمی این فیلد را ندارند و با مقدار خالی بدون از دست رفتن داده قبلی Decode می‌شوند. */
    val flashcardProgress: List<BackupFlashcardProgress> = emptyList()
) {
    companion object {
        /** نسخه 3 وضعیت Spaced Review را اضافه می‌کند؛ فیلد جدید optional/default است و backward-compatible می‌ماند. */
        const val CURRENT_SCHEMA_VERSION: Int = 3

        /** نسخه 1 با مقادیر پیش‌فرض فیلدهای جدید همچنان قابل بازیابی است. */
        const val MIN_SUPPORTED_SCHEMA_VERSION: Int = 1
    }
}
