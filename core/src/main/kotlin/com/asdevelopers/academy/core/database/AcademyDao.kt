package com.asdevelopers.academy.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {
    @Query("SELECT * FROM lesson_progress WHERE lessonId = :lessonId LIMIT 1")
    fun observe(lessonId: String): Flow<LessonProgressEntity?>

    @Query("SELECT * FROM lesson_progress")
    fun observeAll(): Flow<List<LessonProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LessonProgressEntity)
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface QuizResultDao {
    @Query("SELECT * FROM quiz_results WHERE quizId = :quizId ORDER BY completedAt DESC")
    fun observeForQuiz(quizId: String): Flow<List<QuizResultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: QuizResultEntity)
}

@Dao
interface UserNoteDao {
    @Query("SELECT * FROM user_notes WHERE lessonId = :lessonId ORDER BY updatedAt DESC")
    fun observeForLesson(lessonId: String): Flow<List<UserNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UserNoteEntity)

    @Query("DELETE FROM user_notes WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface SearchDao {
    @Query("SELECT * FROM search_index WHERE search_index MATCH :query LIMIT :limit")
    suspend fun search(query: String, limit: Int = 50): List<SearchIndexEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SearchIndexEntity>)

    @Query("DELETE FROM search_index")
    suspend fun clear()
}
