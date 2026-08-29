package com.asdevelopers.academy.core.repository

import com.asdevelopers.academy.core.database.AchievementDao
import com.asdevelopers.academy.core.database.AchievementEntity
import com.asdevelopers.academy.core.database.BookmarkDao
import com.asdevelopers.academy.core.database.BookmarkEntity
import com.asdevelopers.academy.core.database.ExerciseDraftDao
import com.asdevelopers.academy.core.database.ExerciseDraftEntity
import com.asdevelopers.academy.core.database.LessonProgressEntity
import com.asdevelopers.academy.core.database.LearningCompletionDao
import com.asdevelopers.academy.core.database.LearningCompletionEntity
import com.asdevelopers.academy.core.database.ProgressDao
import com.asdevelopers.academy.core.database.ProjectProgressDao
import com.asdevelopers.academy.core.database.ProjectProgressEntity
import com.asdevelopers.academy.core.database.QuizResultDao
import com.asdevelopers.academy.core.database.QuizResultEntity
import com.asdevelopers.academy.core.database.SearchDao
import com.asdevelopers.academy.core.database.SearchIndexEntity
import com.asdevelopers.academy.core.database.UserNoteDao
import com.asdevelopers.academy.core.database.UserNoteEntity
import com.asdevelopers.academy.core.content.CourseBundle
import com.asdevelopers.academy.core.exercise.ExerciseDraft
import com.asdevelopers.academy.core.progress.LearningDashboard
import com.asdevelopers.academy.core.progress.LessonProgress
import com.asdevelopers.academy.core.progress.LessonStatus
import com.asdevelopers.academy.core.progress.LearningCompletion
import com.asdevelopers.academy.core.progress.LearningPathEngine
import com.asdevelopers.academy.core.progress.LearningTargetType
import com.asdevelopers.academy.core.project.ProjectProgress
import com.asdevelopers.academy.core.quiz.Quiz
import com.asdevelopers.academy.core.quiz.QuizScore
import com.asdevelopers.academy.core.search.FtsQueryBuilder
import com.asdevelopers.academy.core.search.SearchDocument
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/** Repository واحد Progress؛ ViewModelها هیچ‌گاه مستقیماً DAO را مصرف نمی‌کنند. */
class ProgressRepository(private val dao: ProgressDao) {
    fun observeLesson(courseId: String, lessonId: String): Flow<LessonProgress?> =
        dao.observe(courseId, lessonId).map { entity -> entity?.toModel() }

    fun observeCourse(courseId: String): Flow<List<LessonProgress>> =
        dao.observeCourse(courseId).map { items -> items.map(LessonProgressEntity::toModel) }

    /** Dashboard، قفل Levelها و مقصد ادامه یادگیری را بدون منطق تکراری در ViewModelهای Course می‌سازد. */
    fun observeDashboard(
        bundle: CourseBundle,
        unlockThresholdPercent: Int = 80
    ): Flow<LearningDashboard> = dao.observeCourseWithLegacy(bundle.manifest.courseId).map { items ->
        LearningPathEngine.buildDashboard(
            bundle = bundle,
            progress = items.map(LessonProgressEntity::toModel),
            unlockThresholdPercent = unlockThresholdPercent
        )
    }

    suspend fun save(progress: LessonProgress) {
        // courseId خالی فقط برای Migration نسخه آزمایشی مجاز بود و داده جدید باید شناسه واقعی داشته باشد.
        require(progress.courseId.isNotBlank()) { "courseId is required for new progress records" }
        dao.upsert(progress.toEntity())
    }
}

/** تکمیل Exercise/Project را با کلید چنددوره‌ای ثبت و برای Dashboard منتشر می‌کند. */
class LearningCompletionRepository(private val dao: LearningCompletionDao) {
    fun observeCourse(courseId: String): Flow<List<LearningCompletion>> =
        dao.observeCourse(courseId).map { items -> items.mapNotNull(LearningCompletionEntity::toModel) }

    fun observeTarget(
        courseId: String,
        targetType: LearningTargetType,
        targetId: String
    ): Flow<LearningCompletion?> =
        dao.observe(courseId, targetType.name, targetId).map { it?.toModel() }

    suspend fun save(completion: LearningCompletion) {
        // مقدار خالی فقط از Migration می‌آید و نباید توسط Feature جدید تولید شود.
        require(completion.courseId.isNotBlank()) { "courseId is required for new completion records" }
        dao.upsert(completion.toEntity())
    }
}

/** Repository Bookmark عملیات مشترک افزودن، مشاهده و حذف را متمرکز می‌کند. */
class BookmarkRepository(private val dao: BookmarkDao) {
    fun observeCourse(courseId: String): Flow<List<BookmarkEntity>> = dao.observeCourse(courseId)

    suspend fun add(courseId: String, targetType: String, targetId: String, lessonId: String?, createdAt: Long): String {
        val id = UUID.randomUUID().toString()
        dao.upsert(BookmarkEntity(id, courseId, targetType, targetId, lessonId, createdAt))
        return id
    }

    suspend fun remove(id: String) = dao.delete(id)
}

/** Repository یادداشت از ذخیره متن خالی جلوگیری می‌کند. */
class UserNoteRepository(private val dao: UserNoteDao) {
    fun observeLesson(courseId: String, lessonId: String): Flow<List<UserNoteEntity>> =
        dao.observeForLesson(courseId, lessonId)

    suspend fun save(id: String?, courseId: String, lessonId: String, blockId: String?, text: String, updatedAt: Long): String {
        require(text.isNotBlank()) { "note text cannot be blank" }
        val resolvedId = id ?: UUID.randomUUID().toString()
        dao.upsert(UserNoteEntity(resolvedId, courseId, lessonId, blockId, text.trim(), updatedAt))
        return resolvedId
    }

    suspend fun remove(id: String) = dao.delete(id)
}

/** نتیجه Quiz Engine با اطلاعات تحلیل نقاط ضعف در Room ثبت می‌شود. */
class QuizHistoryRepository(private val dao: QuizResultDao) {
    fun observeQuiz(courseId: String, quizId: String): Flow<List<QuizResultEntity>> =
        dao.observeForQuiz(courseId, quizId)

    suspend fun record(quiz: Quiz, score: QuizScore, completedAt: Long): String {
        val attemptId = UUID.randomUUID().toString()
        dao.insert(
            QuizResultEntity(
                attemptId = attemptId,
                courseId = quiz.courseId,
                quizId = quiz.id,
                scorePercent = score.scorePercent,
                correctCount = score.correctQuestionIds.size,
                wrongCount = score.wrongQuestionIds.size,
                weakTags = score.weakTags.sorted().joinToString(TAG_SEPARATOR),
                completedAt = completedAt
            )
        )
        return attemptId
    }
}

/** Draft تمرین بدون نیاز به کد تکراری Autosave در هر دوره نگهداری می‌شود. */
class ExerciseDraftRepository(private val dao: ExerciseDraftDao) {
    fun observe(courseId: String, exerciseId: String): Flow<ExerciseDraft?> =
        dao.observe(courseId, exerciseId).map { it?.toModel() }

    suspend fun save(draft: ExerciseDraft) = dao.upsert(draft.toEntity())
}

/** وضعیت Milestoneهای پروژه با قرارداد Engine نگهداری می‌شود. */
class ProjectProgressRepository(private val dao: ProjectProgressDao) {
    fun observe(courseId: String, projectId: String): Flow<ProjectProgress?> =
        dao.observe(courseId, projectId).map { it?.toModel() }

    suspend fun save(progress: ProjectProgress) = dao.upsert(progress.toEntity())
}

/** Search Repository Query خام UI را قبل از ارسال به FTS پاک‌سازی می‌کند. */
class SearchRepository(private val dao: SearchDao) {
    suspend fun search(courseId: String, rawQuery: String, limit: Int = 50): List<SearchIndexEntity> {
        val ftsQuery = FtsQueryBuilder.build(rawQuery)
        if (ftsQuery.isBlank()) return emptyList()
        return dao.search(courseId, ftsQuery, limit.coerceIn(1, 100))
    }

    suspend fun replaceCourse(courseId: String, documents: List<SearchDocument>) {
        dao.clearCourse(courseId)
        dao.insertAll(documents.map { SearchIndexEntity(it.courseId, it.refId, it.refType, it.title, it.body) })
    }
}

/** Achievement بازشده فقط یک بار ثبت و به‌صورت Flow نمایش داده می‌شود. */
class AchievementRepository(private val dao: AchievementDao) {
    fun observeCourse(courseId: String): Flow<List<AchievementEntity>> = dao.observeCourse(courseId)
    suspend fun unlock(courseId: String, achievementId: String, unlockedAt: Long) =
        dao.insert(AchievementEntity(courseId, achievementId, unlockedAt))
}

private fun LessonProgressEntity.toModel(): LessonProgress = LessonProgress(
    lessonId = lessonId,
    status = runCatching { LessonStatus.valueOf(status) }.getOrDefault(LessonStatus.NOT_STARTED),
    progressPercent = progressPercent,
    lastBlockIndex = lastBlockIndex,
    studySeconds = studySeconds,
    lastOpenedAtEpochMillis = lastOpenedAt,
    completedAtEpochMillis = completedAt,
    courseId = courseId
)

private fun LessonProgress.toEntity(): LessonProgressEntity = LessonProgressEntity(
    courseId = courseId,
    lessonId = lessonId,
    status = status.name,
    progressPercent = progressPercent,
    lastBlockIndex = lastBlockIndex,
    studySeconds = studySeconds,
    lastOpenedAt = lastOpenedAtEpochMillis,
    completedAt = completedAtEpochMillis
)

private fun LearningCompletionEntity.toModel(): LearningCompletion? {
    val parsedType = runCatching { LearningTargetType.valueOf(targetType) }.getOrNull() ?: return null
    return LearningCompletion(courseId, parsedType, targetId, completed, completedAt)
}

private fun LearningCompletion.toEntity(): LearningCompletionEntity = LearningCompletionEntity(
    key = stableKey,
    courseId = courseId,
    targetType = targetType.name,
    targetId = targetId,
    completed = completed,
    completedAt = completedAtEpochMillis
)

private fun ExerciseDraftEntity.toModel(): ExerciseDraft =
    ExerciseDraft(courseId, exerciseId, answer, updatedAt)

private fun ExerciseDraft.toEntity(): ExerciseDraftEntity =
    ExerciseDraftEntity(courseId, exerciseId, answer, updatedAtEpochMillis)

private fun ProjectProgressEntity.toModel(): ProjectProgress = ProjectProgress(
    courseId = courseId,
    projectId = projectId,
    completedMilestoneIds = completedMilestoneIds.split(ID_SEPARATOR).filter(String::isNotBlank).toSet(),
    draft = draft,
    updatedAtEpochMillis = updatedAt,
    completedAtEpochMillis = completedAt
)

private fun ProjectProgress.toEntity(): ProjectProgressEntity = ProjectProgressEntity(
    courseId = courseId,
    projectId = projectId,
    completedMilestoneIds = completedMilestoneIds.sorted().joinToString(ID_SEPARATOR),
    draft = draft,
    updatedAt = updatedAtEpochMillis,
    completedAt = completedAtEpochMillis
)

private const val ID_SEPARATOR = "|"
private const val TAG_SEPARATOR = "|"
