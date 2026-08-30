package com.asdevelopers.academy.core.repository

import com.asdevelopers.academy.core.database.FlashcardProgressDao
import com.asdevelopers.academy.core.database.FlashcardProgressEntity
import com.asdevelopers.academy.core.review.FlashcardProgress
import com.asdevelopers.academy.core.review.ReviewRating
import com.asdevelopers.academy.core.review.SpacedReviewEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Repository مشترک Review، Engine مستقل از Android را به Persistence Room متصل می‌کند.
 * Courseها فقط courseId و cardId را می‌دهند و نباید الگوریتم زمان‌بندی یا Entity دیتابیس را تکرار کنند.
 */
class FlashcardReviewRepository(private val dao: FlashcardProgressDao) {

    /** تمام وضعیت‌های مرور یک Course برای Dashboard/Statistics. */
    fun observeCourse(courseId: String): Flow<List<FlashcardProgress>> =
        dao.observeCourse(courseId).map { items -> items.map(FlashcardProgressEntity::toModel) }

    /** فقط کارت‌هایی که موعدشان رسیده برای ساخت Review Session برگردانده می‌شوند. */
    fun observeDue(courseId: String, currentEpochDay: Long): Flow<List<FlashcardProgress>> =
        dao.observeDue(courseId, currentEpochDay).map { items -> items.map(FlashcardProgressEntity::toModel) }

    /** وضعیت یک کارت برای نمایش جزئیات یا ادامه Session. */
    fun observe(courseId: String, cardId: String): Flow<FlashcardProgress?> =
        dao.observe(courseId, cardId).map { it?.toModel() }

    /**
     * پاسخ کاربر را با وضعیت فعلی زمان‌بندی و همان نتیجه را atomically به‌صورت یک Upsert ذخیره می‌کند.
     * current از دیتابیس خوانده می‌شود تا Host نتواند ناخواسته Progress قدیمی را overwrite کند.
     */
    suspend fun recordReview(
        courseId: String,
        cardId: String,
        rating: ReviewRating,
        reviewedEpochDay: Long,
        updatedAtEpochMillis: Long
    ): FlashcardProgress {
        require(courseId.isNotBlank()) { "courseId is required for review progress" }
        require(cardId.isNotBlank()) { "cardId is required for review progress" }

        val current = dao.observe(courseId, cardId).first()?.toModel()
        val scheduled = SpacedReviewEngine.schedule(
            cardId = cardId,
            current = current,
            rating = rating,
            reviewedEpochDay = reviewedEpochDay
        )
        dao.upsert(scheduled.toEntity(courseId, updatedAtEpochMillis))
        return scheduled
    }

    /** Import/restore یا ابزار مدیریتی می‌تواند وضعیت معتبر را بدون اجرای Rating ذخیره کند. */
    suspend fun save(courseId: String, progress: FlashcardProgress, updatedAtEpochMillis: Long) {
        require(courseId.isNotBlank()) { "courseId is required for review progress" }
        dao.upsert(progress.toEntity(courseId, updatedAtEpochMillis))
    }
}

private fun FlashcardProgressEntity.toModel(): FlashcardProgress = FlashcardProgress(
    cardId = cardId,
    repetitions = repetitions,
    intervalDays = intervalDays,
    easeFactor = easeFactor,
    lastReviewedEpochDay = lastReviewedEpochDay,
    dueEpochDay = dueEpochDay
)

private fun FlashcardProgress.toEntity(courseId: String, updatedAtEpochMillis: Long): FlashcardProgressEntity =
    FlashcardProgressEntity(
        courseId = courseId,
        cardId = cardId,
        repetitions = repetitions,
        intervalDays = intervalDays,
        easeFactor = easeFactor,
        lastReviewedEpochDay = lastReviewedEpochDay,
        dueEpochDay = dueEpochDay,
        updatedAt = updatedAtEpochMillis
    )
