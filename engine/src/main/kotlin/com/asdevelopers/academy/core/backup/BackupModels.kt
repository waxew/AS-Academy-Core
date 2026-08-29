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
    val achievements: List<BackupAchievement> = emptyList()
) {
    companion object {
        /** با تغییر ناسازگار قالب Backup این شماره همراه Migration افزایش می‌یابد. */
        const val CURRENT_SCHEMA_VERSION: Int = 2

        /** نسخه 1 با مقدار پیش‌فرض خالی برای learningCompletions همچنان قابل بازیابی است. */
        const val MIN_SUPPORTED_SCHEMA_VERSION: Int = 1
    }
}
