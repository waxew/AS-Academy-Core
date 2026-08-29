package com.asdevelopers.academy.core.exercise

/** انواع تمرین عمومی که Courseها می‌توانند از آن‌ها استفاده کنند. */
enum class ExerciseType {
    READ_AND_ANSWER,
    WRITE_CODE,
    COMPLETE_CODE,
    FIX_CODE,
    PREDICT_OUTPUT,
    BUILD_FEATURE
}

/** سطح سختی مشترک تمرین‌ها. */
enum class ExerciseDifficulty {
    EASY,
    MEDIUM,
    HARD,
    EXPERT
}

/** مدل استاندارد Exercise Engine. */
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
    val explanation: String? = null
)
