package com.asdevelopers.academy.core.quiz

import kotlinx.serialization.Serializable

/** انواع سؤال پشتیبانی‌شده توسط Quiz Engine مرکزی. */
@Serializable
enum class QuestionType {
    MULTIPLE_CHOICE,
    MULTIPLE_SELECT,
    TRUE_FALSE,
    CODE_OUTPUT,
    FILL_CODE,
    FIND_ERROR,
    ORDER_STEPS,
    MATCHING
}

/** پاسخ یک سؤال؛ matchKey برای سؤال Matching و order برای مرتب‌سازی استفاده می‌شود. */
@Serializable
data class QuizAnswer(
    val id: String,
    val text: String,
    val isCorrect: Boolean,
    val matchKey: String? = null,
    val order: Int? = null
)

/** سؤال استاندارد قابل استفاده در تمام Course Packageها. */
@Serializable
data class QuizQuestion(
    val id: String,
    val type: QuestionType,
    val question: String,
    val explanation: String,
    val tags: Set<String> = emptySet(),
    val answers: List<QuizAnswer> = emptyList(),
    val points: Int = 1,
    val codeLanguage: String? = null
) {
    init {
        // امتیاز صفر یا منفی محاسبه نتیجه را نامعتبر می‌کند.
        require(points > 0) { "question points must be positive" }
    }
}

/** آزمون استاندارد که می‌تواند به درس یا فصل متصل شود. */
@Serializable
data class Quiz(
    val id: String,
    val lessonId: String?,
    val title: String,
    val passingScorePercent: Int,
    val questions: List<QuizQuestion>,
    val courseId: String = "",
    val chapterId: String? = null,
    val shuffleQuestions: Boolean = false,
    val shuffleAnswers: Boolean = false
) {
    init {
        // نمره قبولی همیشه یک درصد استاندارد است.
        require(passingScorePercent in 0..100) { "passingScorePercent must be between 0 and 100" }
    }
}

/** پاسخ‌های انتخاب‌شده کاربر برای یک سؤال. */
@Serializable
data class QuestionSubmission(
    val questionId: String,
    /** سؤال‌های انتخابی با Set تصحیح می‌شوند و ترتیب انتخاب اهمیتی ندارد. */
    val selectedAnswerIds: Set<String> = emptySet(),
    /** پاسخ FILL_CODE با متن پاسخ صحیح مقایسه می‌شود. */
    val textAnswer: String? = null,
    /** ORDER_STEPS ترتیب کامل Stable ID پاسخ‌ها را نگه می‌دارد. */
    val orderedAnswerIds: List<String> = emptyList(),
    /** MATCHING برای هر answerId کلید جفت انتخاب‌شده را نگه می‌دارد. */
    val matchedAnswerKeys: Map<String, String> = emptyMap()
)

/** نتیجه کامل آزمون برای ذخیره در دیتابیس و تحلیل نقاط ضعف. */
@Serializable
data class QuizScore(
    val scorePercent: Int,
    val earnedPoints: Int,
    val totalPoints: Int,
    val correctQuestionIds: Set<String>,
    val wrongQuestionIds: Set<String>,
    val weakTags: Set<String>,
    val passed: Boolean
)
