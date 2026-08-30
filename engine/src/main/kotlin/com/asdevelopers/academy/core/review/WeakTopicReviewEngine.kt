package com.asdevelopers.academy.core.review

import com.asdevelopers.academy.core.content.CourseBundle
import com.asdevelopers.academy.core.quiz.QuizScore
import kotlinx.serialization.Serializable

/** پیشنهاد مرور یک درس بر اساس Tagهای ضعف استخراج‌شده از Quiz Engine. */
@Serializable
data class LessonReviewRecommendation(
    val lessonId: String,
    val title: String,
    val matchedTags: Set<String>,
    val priority: Int
)

/**
 * موتور مشترک تشخیص موضوعات ضعیف.
 * QuizScore فقط weakTags را تولید می‌کند و این Engine آن‌ها را به درس‌های واقعی Course نگاشت می‌کند.
 */
object WeakTopicReviewEngine {

    /** فراوانی ضعف هر Tag را از چند نتیجه آزمون محاسبه می‌کند. */
    fun weakTagFrequency(scores: List<QuizScore>): Map<String, Int> = scores
        .flatMap { it.weakTags }
        .groupingBy { it }
        .eachCount()
        .toList()
        .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first })
        .toMap(linkedMapOf())

    /**
     * درس‌هایی را که بیشترین هم‌پوشانی با ضعف‌های تکرارشونده دارند رتبه‌بندی می‌کند.
     * priority مجموع فراوانی Tagهای ضعف مشترک با هر درس است.
     */
    fun recommendLessons(
        bundle: CourseBundle,
        scores: List<QuizScore>,
        limit: Int = 10
    ): List<LessonReviewRecommendation> {
        require(limit > 0) { "limit must be positive" }
        val frequency = weakTagFrequency(scores)
        if (frequency.isEmpty()) return emptyList()

        return bundle.lessons.mapNotNull { lesson ->
            val matched = lesson.tags.filterTo(linkedSetOf()) { it in frequency }
            if (matched.isEmpty()) return@mapNotNull null
            val priority = matched.sumOf { frequency.getValue(it) }
            LessonReviewRecommendation(
                lessonId = lesson.id,
                title = lesson.title,
                matchedTags = matched,
                priority = priority
            )
        }
            .sortedWith(
                compareByDescending<LessonReviewRecommendation> { it.priority }
                    .thenBy { it.title }
                    .thenBy { it.lessonId }
            )
            .take(limit)
    }
}
