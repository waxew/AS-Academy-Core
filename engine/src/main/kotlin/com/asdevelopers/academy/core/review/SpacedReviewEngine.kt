package com.asdevelopers.academy.core.review

import com.asdevelopers.academy.course.model.GlossaryEntry
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

/** درجه سختی پاسخ کاربر برای زمان‌بندی مرور بعدی. */
@Serializable
enum class ReviewRating {
    AGAIN,
    HARD,
    GOOD,
    EASY
}

/**
 * Flashcard عمومی AS Academy.
 * Course لازم نیست فایل Flashcard جداگانه نگه دارد؛ کارت‌های پایه می‌توانند مستقیماً از Glossary ساخته شوند.
 */
@Serializable
data class Flashcard(
    val id: String,
    val front: String,
    val back: String,
    val aliases: List<String> = emptyList(),
    val relatedLessonIds: List<String> = emptyList(),
    val tags: Set<String> = emptySet()
)

/** وضعیت مرور یک کارت برای ذخیره در Persistence مشترک. */
@Serializable
data class FlashcardProgress(
    val cardId: String,
    val repetitions: Int = 0,
    val intervalDays: Int = 0,
    val easeFactor: Double = 2.5,
    val lastReviewedEpochDay: Long? = null,
    val dueEpochDay: Long = 0
)

/**
 * موتور سبک Spaced Review.
 * منطق مستقل از Android و Course است تا تمام اپ‌های AS Academy رفتار مرور یکسان داشته باشند.
 */
object SpacedReviewEngine {

    /** Glossary همان منبع حقیقت واژگان است و بدون duplication به Flashcard تبدیل می‌شود. */
    fun fromGlossary(entries: List<GlossaryEntry>): List<Flashcard> = entries.map { entry ->
        Flashcard(
            id = "flashcard-${entry.id}",
            front = entry.term,
            back = entry.definition,
            aliases = entry.aliases,
            relatedLessonIds = entry.relatedLessonIds,
            tags = entry.tags
        )
    }

    /**
     * فاصله مرور بعدی را با الگوریتمی پایدار و قابل پیش‌بینی محاسبه می‌کند.
     * AGAIN زنجیره موفقیت را reset می‌کند؛ HARD رشد کم، GOOD رشد معمول و EASY رشد سریع‌تر دارد.
     */
    fun schedule(
        cardId: String,
        current: FlashcardProgress? = null,
        rating: ReviewRating,
        reviewedEpochDay: Long
    ): FlashcardProgress {
        val previous = current ?: FlashcardProgress(cardId = cardId, dueEpochDay = reviewedEpochDay)
        require(previous.cardId == cardId) { "progress cardId must match reviewed card" }

        val newEase = when (rating) {
            ReviewRating.AGAIN -> (previous.easeFactor - 0.20).coerceAtLeast(1.30)
            ReviewRating.HARD -> (previous.easeFactor - 0.15).coerceAtLeast(1.30)
            ReviewRating.GOOD -> previous.easeFactor.coerceAtLeast(1.30)
            ReviewRating.EASY -> (previous.easeFactor + 0.15).coerceAtLeast(1.30)
        }

        val newRepetitions = if (rating == ReviewRating.AGAIN) 0 else previous.repetitions + 1
        val interval = when (rating) {
            ReviewRating.AGAIN -> 1
            ReviewRating.HARD -> if (previous.repetitions == 0) {
                1
            } else {
                (previous.intervalDays.coerceAtLeast(1) * 1.2).roundToInt().coerceAtLeast(1)
            }
            ReviewRating.GOOD -> when (previous.repetitions) {
                0 -> 1
                1 -> 3
                else -> (previous.intervalDays.coerceAtLeast(1) * newEase).roundToInt().coerceAtLeast(1)
            }
            ReviewRating.EASY -> when (previous.repetitions) {
                0 -> 3
                1 -> 6
                else -> (previous.intervalDays.coerceAtLeast(1) * (newEase + 0.15)).roundToInt().coerceAtLeast(1)
            }
        }

        return FlashcardProgress(
            cardId = cardId,
            repetitions = newRepetitions,
            intervalDays = interval,
            easeFactor = newEase,
            lastReviewedEpochDay = reviewedEpochDay,
            dueEpochDay = reviewedEpochDay + interval
        )
    }

    /** فقط کارت‌هایی را برمی‌گرداند که موعد مرورشان رسیده یا گذشته است. */
    fun dueCards(
        cards: List<Flashcard>,
        progress: Map<String, FlashcardProgress>,
        currentEpochDay: Long
    ): List<Flashcard> = cards.filter { card ->
        progress[card.id]?.dueEpochDay?.let { it <= currentEpochDay } ?: true
    }
}
