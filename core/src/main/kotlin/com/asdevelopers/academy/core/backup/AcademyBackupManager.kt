package com.asdevelopers.academy.core.backup

import androidx.room.withTransaction
import com.asdevelopers.academy.core.database.AcademyDatabase
import com.asdevelopers.academy.core.database.AchievementEntity
import com.asdevelopers.academy.core.database.BookmarkEntity
import com.asdevelopers.academy.core.database.ExerciseDraftEntity
import com.asdevelopers.academy.core.database.FlashcardProgressEntity
import com.asdevelopers.academy.core.database.LessonProgressEntity
import com.asdevelopers.academy.core.database.LearningCompletionEntity
import com.asdevelopers.academy.core.database.ProjectProgressEntity
import com.asdevelopers.academy.core.database.QuizResultEntity
import com.asdevelopers.academy.core.database.UserNoteEntity
import com.asdevelopers.academy.core.exercise.ExerciseDraft
import com.asdevelopers.academy.core.progress.LessonProgress
import com.asdevelopers.academy.core.progress.LessonStatus
import com.asdevelopers.academy.core.progress.LearningCompletion
import com.asdevelopers.academy.core.progress.LearningTargetType
import com.asdevelopers.academy.core.project.ProjectProgress
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

/** نتیجه Restore برای نمایش شفاف موفقیت یا ناسازگاری فایل. */
sealed interface RestoreResult {
    data class Success(val restoredRecords: Int) : RestoreResult
    data class UnsupportedSchema(val schemaVersion: Int) : RestoreResult
    data class Failure(val message: String, val cause: Throwable? = null) : RestoreResult
}

/**
 * Backup Manager تمام داده‌های غیرقابل‌بازسازی کاربر را در JSON نسخه‌دار صادر و Transactional بازیابی می‌کند.
 */
class AcademyBackupManager(
    private val database: AcademyDatabase,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }
) {
    suspend fun export(output: OutputStream, nowEpochMillis: Long) {
        // Snapshot از همه DAOها قبل از نوشتن فایل ساخته می‌شود تا قالب خروجی مستقل از Room بماند.
        val backup = AcademyBackup(
            createdAtEpochMillis = nowEpochMillis,
            lessonProgress = database.progressDao().getAll().map(LessonProgressEntity::toBackupModel),
            learningCompletions = database.learningCompletionDao().getAll()
                .mapNotNull(LearningCompletionEntity::toBackupModel),
            bookmarks = database.bookmarkDao().getAll().map(BookmarkEntity::toBackupModel),
            notes = database.userNoteDao().getAll().map(UserNoteEntity::toBackupModel),
            quizResults = database.quizResultDao().getAll().map(QuizResultEntity::toBackupModel),
            exerciseDrafts = database.exerciseDraftDao().getAll().map(ExerciseDraftEntity::toBackupModel),
            projectProgress = database.projectProgressDao().getAll().map(ProjectProgressEntity::toBackupModel),
            achievements = database.achievementDao().getAll().map(AchievementEntity::toBackupModel),
            flashcardProgress = database.flashcardProgressDao().getAll().map(FlashcardProgressEntity::toBackupModel)
        )
        // Stream در اختیار Caller است و Manager آن را نمی‌بندد تا SAF lifecycle شکسته نشود.
        output.writer(Charsets.UTF_8).apply { write(json.encodeToString(backup)); flush() }
    }

    suspend fun restore(input: InputStream): RestoreResult = try {
        // فایل ابتدا کامل Decode می‌شود؛ JSON ناقص هیچ تغییری در دیتابیس ایجاد نمی‌کند.
        val backup = json.decodeFromString<AcademyBackup>(input.bufferedReader(Charsets.UTF_8).readText())
        if (backup.schemaVersion !in AcademyBackup.MIN_SUPPORTED_SCHEMA_VERSION..AcademyBackup.CURRENT_SCHEMA_VERSION) {
            return RestoreResult.UnsupportedSchema(backup.schemaVersion)
        }

        var restored = 0
        // همه جدول‌ها در یک Transaction Upsert می‌شوند تا شکست میانه Restore داده نیمه‌کاره نسازد.
        database.withTransaction {
            database.progressDao().upsertAll(backup.lessonProgress.map(LessonProgress::toEntity))
            database.learningCompletionDao().upsertAll(backup.learningCompletions.map(LearningCompletion::toEntity))
            database.bookmarkDao().upsertAll(backup.bookmarks.map(BackupBookmark::toEntity))
            database.userNoteDao().upsertAll(backup.notes.map(BackupNote::toEntity))
            database.quizResultDao().insertAll(backup.quizResults.map(BackupQuizResult::toEntity))
            database.exerciseDraftDao().upsertAll(backup.exerciseDrafts.map(ExerciseDraft::toEntity))
            database.projectProgressDao().upsertAll(backup.projectProgress.map(ProjectProgress::toEntity))
            database.achievementDao().insertAll(backup.achievements.map(BackupAchievement::toEntity))
            database.flashcardProgressDao().upsertAll(backup.flashcardProgress.map(BackupFlashcardProgress::toEntity))
            restored = backup.lessonProgress.size + backup.learningCompletions.size + backup.bookmarks.size + backup.notes.size +
                backup.quizResults.size + backup.exerciseDrafts.size + backup.projectProgress.size + backup.achievements.size +
                backup.flashcardProgress.size
        }
        RestoreResult.Success(restored)
    } catch (error: Exception) {
        RestoreResult.Failure(error.message ?: "Backup restore failed", error)
    }
}

private fun LessonProgressEntity.toBackupModel(): LessonProgress = LessonProgress(
    lessonId = lessonId,
    status = runCatching { LessonStatus.valueOf(status) }.getOrDefault(LessonStatus.NOT_STARTED),
    progressPercent = progressPercent,
    lastBlockIndex = lastBlockIndex,
    studySeconds = studySeconds,
    lastOpenedAtEpochMillis = lastOpenedAt,
    completedAtEpochMillis = completedAt,
    courseId = courseId
)

private fun LearningCompletionEntity.toBackupModel(): LearningCompletion? {
    val parsedType = runCatching { LearningTargetType.valueOf(targetType) }.getOrNull() ?: return null
    return LearningCompletion(courseId, parsedType, targetId, completed, completedAt)
}

private fun BookmarkEntity.toBackupModel() = BackupBookmark(id, courseId, targetType, targetId, lessonId, createdAt)
private fun UserNoteEntity.toBackupModel() = BackupNote(id, courseId, lessonId, blockId, text, updatedAt)
private fun QuizResultEntity.toBackupModel() = BackupQuizResult(
    attemptId, courseId, quizId, scorePercent, correctCount, wrongCount,
    weakTags.split(SEPARATOR).filter(String::isNotBlank).toSet(), completedAt
)
private fun ExerciseDraftEntity.toBackupModel() = ExerciseDraft(courseId, exerciseId, answer, updatedAt)
private fun ProjectProgressEntity.toBackupModel() = ProjectProgress(
    courseId, projectId, completedMilestoneIds.split(SEPARATOR).filter(String::isNotBlank).toSet(), draft, updatedAt, completedAt
)
private fun AchievementEntity.toBackupModel() = BackupAchievement(courseId, achievementId, unlockedAt)
private fun FlashcardProgressEntity.toBackupModel() = BackupFlashcardProgress(
    courseId = courseId,
    cardId = cardId,
    repetitions = repetitions,
    intervalDays = intervalDays,
    easeFactor = easeFactor,
    lastReviewedEpochDay = lastReviewedEpochDay,
    dueEpochDay = dueEpochDay,
    updatedAtEpochMillis = updatedAt
)

private fun LessonProgress.toEntity() = LessonProgressEntity(
    courseId, lessonId, status.name, progressPercent, lastBlockIndex, studySeconds,
    lastOpenedAtEpochMillis, completedAtEpochMillis
)
private fun LearningCompletion.toEntity() = LearningCompletionEntity(
    stableKey, courseId, targetType.name, targetId, completed, completedAtEpochMillis
)
private fun BackupBookmark.toEntity() = BookmarkEntity(id, courseId, targetType, targetId, lessonId, createdAtEpochMillis)
private fun BackupNote.toEntity() = UserNoteEntity(id, courseId, lessonId, blockId, text, updatedAtEpochMillis)
private fun BackupQuizResult.toEntity() = QuizResultEntity(
    attemptId, courseId, quizId, scorePercent, correctCount, wrongCount, weakTags.sorted().joinToString(SEPARATOR), completedAtEpochMillis
)
private fun ExerciseDraft.toEntity() = ExerciseDraftEntity(courseId, exerciseId, answer, updatedAtEpochMillis)
private fun ProjectProgress.toEntity() = ProjectProgressEntity(
    courseId, projectId, completedMilestoneIds.sorted().joinToString(SEPARATOR), draft, updatedAtEpochMillis, completedAtEpochMillis
)
private fun BackupAchievement.toEntity() = AchievementEntity(courseId, achievementId, unlockedAtEpochMillis)
private fun BackupFlashcardProgress.toEntity() = FlashcardProgressEntity(
    courseId = courseId,
    cardId = cardId,
    repetitions = repetitions,
    intervalDays = intervalDays,
    easeFactor = easeFactor,
    lastReviewedEpochDay = lastReviewedEpochDay,
    dueEpochDay = dueEpochDay,
    updatedAt = updatedAtEpochMillis
)

private const val SEPARATOR = "|"
