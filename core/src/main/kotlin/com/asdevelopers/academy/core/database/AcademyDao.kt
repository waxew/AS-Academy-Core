package com.asdevelopers.academy.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {
    @Query("SELECT * FROM lesson_progress WHERE courseId = :courseId AND lessonId = :lessonId LIMIT 1")
    fun observe(courseId: String, lessonId: String): Flow<LessonProgressEntity?>

    @Query("SELECT * FROM lesson_progress WHERE courseId = :courseId")
    fun observeCourse(courseId: String): Flow<List<LessonProgressEntity>>

    /** رکوردهای Course خالی فقط برای محاسبه Dashboard دوره‌ای که Stable ID آن‌ها را می‌شناسد خوانده می‌شوند. */
    @Query("SELECT * FROM lesson_progress WHERE courseId = :courseId OR courseId = ''")
    fun observeCourseWithLegacy(courseId: String): Flow<List<LessonProgressEntity>>

    @Query("SELECT * FROM lesson_progress")
    suspend fun getAll(): List<LessonProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LessonProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<LessonProgressEntity>)
}

@Dao
interface LearningCompletionDao {
    @Query("SELECT * FROM learning_completion WHERE courseId = :courseId ORDER BY completedAt DESC")
    fun observeCourse(courseId: String): Flow<List<LearningCompletionEntity>>

    @Query("SELECT * FROM learning_completion WHERE courseId = :courseId AND targetType = :targetType AND targetId = :targetId LIMIT 1")
    fun observe(courseId: String, targetType: String, targetId: String): Flow<LearningCompletionEntity?>

    @Query("SELECT * FROM learning_completion")
    suspend fun getAll(): List<LearningCompletionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LearningCompletionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<LearningCompletionEntity>)
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE courseId = :courseId ORDER BY createdAt DESC")
    fun observeCourse(courseId: String): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks")
    suspend fun getAll(): List<BookmarkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun delete(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<BookmarkEntity>)
}

@Dao
interface QuizResultDao {
    @Query("SELECT * FROM quiz_results WHERE courseId = :courseId AND quizId = :quizId ORDER BY completedAt DESC")
    fun observeForQuiz(courseId: String, quizId: String): Flow<List<QuizResultEntity>>

    @Query("SELECT * FROM quiz_results")
    fun observeAll(): Flow<List<QuizResultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: QuizResultEntity)

    @Query("SELECT * FROM quiz_results")
    suspend fun getAll(): List<QuizResultEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<QuizResultEntity>)
}

@Dao
interface UserNoteDao {
    @Query("SELECT * FROM user_notes WHERE courseId = :courseId AND lessonId = :lessonId ORDER BY updatedAt DESC")
    fun observeForLesson(courseId: String, lessonId: String): Flow<List<UserNoteEntity>>

    @Query("SELECT * FROM user_notes")
    suspend fun getAll(): List<UserNoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UserNoteEntity)

    @Query("DELETE FROM user_notes WHERE id = :id")
    suspend fun delete(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<UserNoteEntity>)
}

@Dao
interface SearchDao {
    @Query("SELECT * FROM search_index WHERE search_index MATCH :query AND courseId = :courseId LIMIT :limit")
    suspend fun search(courseId: String, query: String, limit: Int = 50): List<SearchIndexEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SearchIndexEntity>)

    @Query("DELETE FROM search_index WHERE courseId = :courseId")
    suspend fun clearCourse(courseId: String)
}

@Dao
interface ExerciseDraftDao {
    @Query("SELECT * FROM exercise_drafts WHERE courseId = :courseId AND exerciseId = :exerciseId LIMIT 1")
    fun observe(courseId: String, exerciseId: String): Flow<ExerciseDraftEntity?>

    @Query("SELECT * FROM exercise_drafts")
    suspend fun getAll(): List<ExerciseDraftEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ExerciseDraftEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ExerciseDraftEntity>)
}

@Dao
interface ProjectProgressDao {
    @Query("SELECT * FROM project_progress WHERE courseId = :courseId AND projectId = :projectId LIMIT 1")
    fun observe(courseId: String, projectId: String): Flow<ProjectProgressEntity?>

    @Query("SELECT * FROM project_progress")
    suspend fun getAll(): List<ProjectProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ProjectProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ProjectProgressEntity>)
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements WHERE courseId = :courseId ORDER BY unlockedAt")
    fun observeCourse(courseId: String): Flow<List<AchievementEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: AchievementEntity)

    @Query("SELECT * FROM achievements")
    suspend fun getAll(): List<AchievementEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<AchievementEntity>)
}

/**
 * DAO مرور Flashcard فقط State کاربر را نگه می‌دارد؛ متن کارت همیشه از Course Package خوانده می‌شود.
 * این جداسازی باعث می‌شود Update محتوای کارت بدون تکرار متن در دیتابیس و بدون حذف سابقه مرور انجام شود.
 */
@Dao
interface FlashcardReviewDao {
    @Query("SELECT * FROM flashcard_review_state WHERE courseId = :courseId AND flashcardId = :flashcardId LIMIT 1")
    fun observe(courseId: String, flashcardId: String): Flow<FlashcardReviewEntity?>

    /** نسخه suspend برای محاسبه State بعدی هنگام ثبت Rating استفاده می‌شود. */
    @Query("SELECT * FROM flashcard_review_state WHERE courseId = :courseId AND flashcardId = :flashcardId LIMIT 1")
    suspend fun get(courseId: String, flashcardId: String): FlashcardReviewEntity?

    /** صف مرور امروز فقط کارت‌های موعدرسیده همان Course را به ترتیب قدیمی‌ترین موعد برمی‌گرداند. */
    @Query("SELECT * FROM flashcard_review_state WHERE courseId = :courseId AND dueAt <= :nowEpochMillis ORDER BY dueAt ASC, flashcardId ASC LIMIT :limit")
    fun observeDue(courseId: String, nowEpochMillis: Long, limit: Int = 100): Flow<List<FlashcardReviewEntity>>

    /** Snapshot یک Session مرور را ثابت نگه می‌دارد تا تغییر Flow بعد از Rating باعث پرش Index نشود. */
    @Query("SELECT * FROM flashcard_review_state WHERE courseId = :courseId AND dueAt <= :nowEpochMillis ORDER BY dueAt ASC, flashcardId ASC LIMIT :limit")
    suspend fun getDue(courseId: String, nowEpochMillis: Long, limit: Int = 100): List<FlashcardReviewEntity>

    @Query("SELECT * FROM flashcard_review_state WHERE courseId = :courseId ORDER BY dueAt ASC, flashcardId ASC")
    fun observeCourse(courseId: String): Flow<List<FlashcardReviewEntity>>

    @Query("SELECT * FROM flashcard_review_state")
    suspend fun getAll(): List<FlashcardReviewEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FlashcardReviewEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<FlashcardReviewEntity>)

    /** کارت تازه فقط یک بار Seed می‌شود و Review History موجود هرگز overwrite نمی‌شود. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(items: List<FlashcardReviewEntity>)
}
