package com.asdevelopers.academy.core.quiz

/** انواع سؤال پشتیبانی‌شده توسط Quiz Engine مرکزی. */
enum class QuestionType {
    MULTIPLE_CHOICE,
    TRUE_FALSE,
    CODE_OUTPUT,
    FILL_CODE,
    FIND_ERROR,
    ORDER_STEPS,
    MATCHING
}

/** پاسخ یک سؤال. */
data class QuizAnswer(
    val id: String,
    val text: String,
    val isCorrect: Boolean
)

/** سؤال استاندارد قابل استفاده در تمام Course Packageها. */
data class QuizQuestion(
    val id: String,
    val type: QuestionType,
    val question: String,
    val explanation: String,
    val tags: Set<String> = emptySet(),
    val answers: List<QuizAnswer> = emptyList()
)

/** آزمون استاندارد. */
data class Quiz(
    val id: String,
    val lessonId: String?,
    val title: String,
    val passingScorePercent: Int,
    val questions: List<QuizQuestion>
) {
    init {
        require(passingScorePercent in 0..100) { "passingScorePercent must be between 0 and 100" }
    }
}
