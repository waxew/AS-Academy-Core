package com.asdevelopers.academy.core.flashcard

import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.serialization.Serializable

/** ارزیابی کاربر بعد از دیدن پاسخ یک Flashcard. */
@Serializable
enum class FlashcardRecallRating {
    /** پاسخ فراموش شده و کارت باید خیلی زود دوباره دیده شود. */
    AGAIN,
    /** پاسخ با زحمت یادآوری شده است. */
    HARD,
    /** پاسخ با اطمینان معمولی یادآوری شده است. */
    GOOD,
    /** پاسخ بسیار آسان بوده و فاصله مرور می‌تواند بیشتر شود. */
    EASY
}

/**
 * وضعیت مستقل از UI یک کارت برای مرور فاصله‌دار.
 *
 * State با Stable ID کارت ذخیره می‌شود تا Update محتوای Course تاریخچه مرور کاربر را از بین نبرد.
 */
@Serializable
data class FlashcardReviewState(
    val courseId: String,
    val flashcardId: String,
    val repetitions: Int = 0,
    val intervalDays: Int = 0,
    val easeFactor: Double = DEFAULT_EASE_FACTOR,
    val dueAtEpochMillis: Long = 0L,
    val lastReviewedAtEpochMillis: Long? = null
) {
    companion object {
        const val DEFAULT_EASE_FACTOR: Double = 2.5
    }
}

/** خروجی یک Review شامل State جدید و فاصله تصمیم‌گرفته‌شده است. */
data class FlashcardReviewResult(
    val state: FlashcardReviewState,
    val scheduledIntervalDays: Int
)

/**
 * موتور مرور فاصله‌دار مشترک AS Academy.
 *
 * الگوریتم عمداً ساده و deterministic است: از ایده SM-2 استفاده می‌کند، اما Rating چهارحالته موبایل را
 * مستقیماً می‌پذیرد. هیچ Course نباید نسخه جداگانه این منطق را پیاده‌سازی کند.
 */
object FlashcardReviewEngine {
    private const val DAY_MILLIS = 24L * 60L * 60L * 1000L
    private const val MIN_EASE_FACTOR = 1.3

    /** State بعدی را بر اساس نتیجه مرور محاسبه می‌کند. */
    fun review(
        state: FlashcardReviewState,
        rating: FlashcardRecallRating,
        reviewedAtEpochMillis: Long
    ): FlashcardReviewResult {
        require(reviewedAtEpochMillis >= 0L) { "reviewedAtEpochMillis cannot be negative" }

        val nextEase = when (rating) {
            FlashcardRecallRating.AGAIN -> max(MIN_EASE_FACTOR, state.easeFactor - 0.20)
            FlashcardRecallRating.HARD -> max(MIN_EASE_FACTOR, state.easeFactor - 0.15)
            FlashcardRecallRating.GOOD -> state.easeFactor
            FlashcardRecallRating.EASY -> state.easeFactor + 0.15
        }

        val nextRepetitions = when (rating) {
            FlashcardRecallRating.AGAIN -> 0
            else -> state.repetitions + 1
        }

        val nextIntervalDays = when (rating) {
            FlashcardRecallRating.AGAIN -> 1
            FlashcardRecallRating.HARD -> max(1, (max(1, state.intervalDays) * 1.2).roundToInt())
            FlashcardRecallRating.GOOD -> when (state.repetitions) {
                0 -> 1
                1 -> 3
                else -> max(1, (max(1, state.intervalDays) * nextEase).roundToInt())
            }
            FlashcardRecallRating.EASY -> when (state.repetitions) {
                0 -> 4
                else -> max(1, (max(1, state.intervalDays) * nextEase * 1.3).roundToInt())
            }
        }

        val nextState = state.copy(
            repetitions = nextRepetitions,
            intervalDays = nextIntervalDays,
            easeFactor = nextEase,
            dueAtEpochMillis = reviewedAtEpochMillis + (nextIntervalDays * DAY_MILLIS),
            lastReviewedAtEpochMillis = reviewedAtEpochMillis
        )
        return FlashcardReviewResult(nextState, nextIntervalDays)
    }

    /** فقط کارت‌های موعدرسیده را به ترتیب قدیمی‌ترین موعد برمی‌گرداند. */
    fun dueCards(
        states: Collection<FlashcardReviewState>,
        nowEpochMillis: Long
    ): List<FlashcardReviewState> = states
        .asSequence()
        .filter { it.dueAtEpochMillis <= nowEpochMillis }
        .sortedWith(compareBy<FlashcardReviewState> { it.dueAtEpochMillis }.thenBy { it.flashcardId })
        .toList()
}
