package com.asdevelopers.academy.core.database

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

/** پیشرفت هر درس؛ شناسه درس همان Stable ID موجود در Course Package است. */
@Entity(tableName = "lesson_progress")
data class LessonProgressEntity(
    @PrimaryKey val lessonId: String,
    val progressPercent: Int,
    val lastBlockIndex: Int,
    val studySeconds: Long,
    val completed: Boolean,
    val lastOpenedAt: Long
)

/** Bookmark می‌تواند به درس، کد، تمرین، پروژه یا واژه‌نامه اشاره کند. */
@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val id: String,
    val targetType: String,
    val targetId: String,
    val lessonId: String?,
    val createdAt: Long
)

/** نتیجه هر بار شرکت در آزمون برای تحلیل روند یادگیری نگهداری می‌شود. */
@Entity(tableName = "quiz_results")
data class QuizResultEntity(
    @PrimaryKey val attemptId: String,
    val quizId: String,
    val scorePercent: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val completedAt: Long
)

/** یادداشت شخصی کاربر روی درس یا Block مشخص. */
@Entity(tableName = "user_notes")
data class UserNoteEntity(
    @PrimaryKey val id: String,
    val lessonId: String,
    val blockId: String?,
    val text: String,
    val updatedAt: Long
)

/** ایندکس Full Text Search مستقل از Course خاص. */
@Fts4
@Entity(tableName = "search_index")
data class SearchIndexEntity(
    val refId: String,
    val refType: String,
    val title: String,
    val body: String
)
