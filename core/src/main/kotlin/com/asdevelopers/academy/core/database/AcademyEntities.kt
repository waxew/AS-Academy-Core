package com.asdevelopers.academy.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey

/** پیشرفت هر درس؛ کلید ترکیبی برای پشتیبانی هم‌زمان چند Course استفاده می‌شود. */
@Entity(
    tableName = "lesson_progress",
    primaryKeys = ["courseId", "lessonId"],
    indices = [Index("lessonId")]
)
data class LessonProgressEntity(
    val courseId: String,
    val lessonId: String,
    val status: String,
    val progressPercent: Int,
    val lastBlockIndex: Int,
    val studySeconds: Long,
    val lastOpenedAt: Long?,
    val completedAt: Long?
)

/** تکمیل آیتم‌های آموزشی غیر درسی مثل Exercise و Project با جداسازی Courseها. */
@Entity(
    tableName = "learning_completion",
    indices = [Index(value = ["courseId", "targetType"]), Index(value = ["courseId", "targetId"])]
)
data class LearningCompletionEntity(
    @PrimaryKey val key: String,
    @ColumnInfo(defaultValue = "''") val courseId: String,
    val targetType: String,
    val targetId: String,
    val completed: Boolean,
    val completedAt: Long
)

/** Bookmark می‌تواند به درس، کد، تمرین، پروژه یا واژه‌نامه اشاره کند. */
@Entity(tableName = "bookmarks", indices = [Index("courseId"), Index(value = ["courseId", "targetId"])])
data class BookmarkEntity(
    @PrimaryKey val id: String,
    // Default SQL با Migration 1→2 یکسان است تا Room schema validation اختلاف گزارش نکند.
    @ColumnInfo(defaultValue = "''") val courseId: String,
    val targetType: String,
    val targetId: String,
    val lessonId: String?,
    val createdAt: Long
)

/** نتیجه هر بار شرکت در آزمون برای تحلیل روند یادگیری نگهداری می‌شود. */
@Entity(tableName = "quiz_results", indices = [Index(value = ["courseId", "quizId"])])
data class QuizResultEntity(
    @PrimaryKey val attemptId: String,
    @ColumnInfo(defaultValue = "''") val courseId: String,
    val quizId: String,
    val scorePercent: Int,
    val correctCount: Int,
    val wrongCount: Int,
    @ColumnInfo(defaultValue = "''") val weakTags: String,
    val completedAt: Long
)

/** یادداشت شخصی کاربر روی درس یا Block مشخص. */
@Entity(tableName = "user_notes", indices = [Index(value = ["courseId", "lessonId"])])
data class UserNoteEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(defaultValue = "''") val courseId: String,
    val lessonId: String,
    val blockId: String?,
    val text: String,
    val updatedAt: Long
)

/** ایندکس Full Text Search مستقل از Course خاص. */
@Fts4
@Entity(tableName = "search_index")
data class SearchIndexEntity(
    val courseId: String,
    val refId: String,
    val refType: String,
    val title: String,
    val body: String
)

/** پیش‌نویس تمرین با کلید Course/Exercise ذخیره می‌شود تا کاربر ادامه کار را از دست ندهد. */
@Entity(tableName = "exercise_drafts", primaryKeys = ["courseId", "exerciseId"])
data class ExerciseDraftEntity(
    val courseId: String,
    val exerciseId: String,
    val answer: String,
    val updatedAt: Long
)

/** وضعیت پروژه عملی و Milestoneهای تکمیل‌شده به‌صورت قابل Backup نگهداری می‌شود. */
@Entity(tableName = "project_progress", primaryKeys = ["courseId", "projectId"])
data class ProjectProgressEntity(
    val courseId: String,
    val projectId: String,
    val completedMilestoneIds: String,
    val draft: String,
    val updatedAt: Long,
    val completedAt: Long?
)

/** نشان‌های بازشده برای نمایش سریع و حفظ تاریخ اولین دریافت ذخیره می‌شوند. */
@Entity(tableName = "achievements", primaryKeys = ["courseId", "achievementId"])
data class AchievementEntity(
    val courseId: String,
    val achievementId: String,
    val unlockedAt: Long
)
