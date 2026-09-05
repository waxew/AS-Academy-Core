package com.asdevelopers.academy.core.model

/** Public bookmark model exposed by Core. Persistence details remain internal to Core. */
data class AcademyBookmark(
    val id: String,
    val courseId: String,
    val targetType: String,
    val targetId: String,
    val lessonId: String?,
    val createdAtEpochMillis: Long
)

/** Public user-note model exposed to presentation consumers. */
data class AcademyUserNote(
    val id: String,
    val courseId: String,
    val lessonId: String,
    val blockId: String?,
    val text: String,
    val updatedAtEpochMillis: Long
)

/** Public immutable quiz-attempt model; storage serialization is hidden inside Core. */
data class AcademyQuizAttempt(
    val attemptId: String,
    val courseId: String,
    val quizId: String,
    val scorePercent: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val weakTags: List<String>,
    val completedAtEpochMillis: Long
)

/** Public search result returned by Core search services. */
data class AcademySearchResult(
    val courseId: String,
    val refId: String,
    val refType: String,
    val title: String,
    val body: String
)

/** Public achievement projection exposed to UI. */
data class AcademyUnlockedAchievement(
    val courseId: String,
    val achievementId: String,
    val unlockedAtEpochMillis: Long
)
