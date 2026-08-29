package com.asdevelopers.academy.core.exercise

import kotlinx.serialization.Serializable

/** انواع تمرین عمومی که Courseها می‌توانند بدون پیاده‌سازی موتور تازه تعریف کنند. */
@Serializable
enum class ExerciseType {
    READ_AND_ANSWER,
    WRITE_CODE,
    COMPLETE_CODE,
    FIX_CODE,
    PREDICT_OUTPUT,
    BUILD_FEATURE
}

/** سطح سختی مشترک تمرین‌ها. */
@Serializable
enum class ExerciseDifficulty {
    EASY,
    MEDIUM,
    HARD,
    EXPERT
}

/** مدل استاندارد Exercise Engine. */
@Serializable
data class Exercise(
    val id: String,
    val lessonId: String,
    val title: String,
    val description: String,
    val type: ExerciseType,
    val difficulty: ExerciseDifficulty,
    val starterCode: String? = null,
    val expectedOutput: String? = null,
    val hints: List<String> = emptyList(),
    val solution: String? = null,
    val explanation: String? = null,
    val courseId: String = "",
    val language: String? = null,
    val tags: Set<String> = emptySet()
)

/** پیش‌نویس تمرین کاربر مستقل از UI و دیتابیس تعریف می‌شود. */
@Serializable
data class ExerciseDraft(
    val courseId: String,
    val exerciseId: String,
    val answer: String,
    val updatedAtEpochMillis: Long
)

/** نتیجه ارزیابی می‌تواند خودکار، دستی یا فقط راهنمای آموزشی باشد. */
@Serializable
data class ExerciseEvaluation(
    val accepted: Boolean,
    val scorePercent: Int,
    val feedback: String,
    val actualOutput: String? = null
)

/** Adapter هر زبان فقط این قرارداد را پیاده می‌کند و UI/ذخیره‌سازی را تکرار نمی‌کند. */
fun interface ExerciseEvaluator {
    suspend fun evaluate(exercise: Exercise, answer: String): ExerciseEvaluation
}
