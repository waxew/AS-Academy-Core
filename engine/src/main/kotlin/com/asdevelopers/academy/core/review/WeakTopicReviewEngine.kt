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
 * ورودی می‌تواند QuizScore زنده یا مجموعه Tagهای ذخیره‌شده در Room/Backup باشد؛ منطق رتبه‌بندی فقط یک‌بار در Core است.
 */
object WeakTopicReviewEngine {

    /** فراوانی ضعف هر Tag را از چند نتیجه زنده Quiz Engine محاسبه می‌کند. */
    fun weakTagFrequency(scores: List<QuizScore>): Map<String, Int> =
        weakTagFrequencyFromSets(scores.map(QuizScore::weakTags))

    /**
     * همان تحلیل برای تاریخچه Persist شده؛ Repository مجبور نیست QuizScore مصنوعی بسازد.
     * هر Set نماینده Weak Tags یک Attempt مستقل است.
     */
    fun weakTagFrequencyFromSets(weakTagSets: List<Set<String>>): Map<String, Int> = weakTagSets
        .flatMap { it }
        .filter(String::isNotBlank)
        .groupingBy { it }
        .eachCount()
        .toList()
        .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first })
        .toMap(linkedMapOf())

    /** مسیر قبلی QuizScore برای سازگاری API حفظ می‌شود. */
    fun recommendLessons(
        bundle: CourseBundle,
        scores: List<QuizScore>,
        limit: Int = 10
    ): List<LessonReviewRecommendation> = recommendLessonsFromWeakTags(
        bundle = bundle,
        weakTagSets = scores.map(QuizScore::weakTags),
        limit = limit
    )

    /**
     * درس‌هایی را که بیشترین هم‌پوشانی با ضعف‌های تکرارشونده Persist شده دارند رتبه‌بندی می‌کند.
     * priority مجموع فراوانی Tagهای ضعف مشترک با هر درس است.
     */
    fun recommendLessonsFromWeakTags(
        bundle: CourseBundle,
        weakTagSets: List<Set<String>>,
        limit: Int = 10
    ): List<LessonReviewRecommendation> {
        require(limit > 0) { "limit must be positive" }
        val frequency = weakTagFrequencyFromSets(weakTagSets)
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
