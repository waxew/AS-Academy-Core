package com.asdevelopers.academy.core.repository

import com.asdevelopers.academy.core.content.CourseBundle
import com.asdevelopers.academy.core.database.FlashcardProgressDao
import com.asdevelopers.academy.core.database.FlashcardProgressEntity
import com.asdevelopers.academy.core.review.Flashcard
import com.asdevelopers.academy.core.review.FlashcardProgress
import com.asdevelopers.academy.core.review.ReviewRating
import com.asdevelopers.academy.core.review.SpacedReviewEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Repository مشترک Review، Engine مستقل از Android را به Persistence Room متصل می‌کند.
 * Courseها فقط Course Bundle و Rating کاربر را می‌دهند و نباید الگوریتم زمان‌بندی یا Entity دیتابیس را تکرار کنند.
 */
class FlashcardReviewRepository(private val dao: FlashcardProgressDao) {

    /** تمام وضعیت‌های مرور یک Course برای Dashboard/Statistics. */
    fun observeCourse(courseId: String): Flow<List<FlashcardProgress>> =
        dao.observeCourse(courseId).map { items -> items.map(FlashcardProgressEntity::toModel) }

    /** فقط Progressهایی که موعدشان رسیده برای آمار low-level برگردانده می‌شوند. */
    fun observeDue(courseId: String, currentEpochDay: Long): Flow<List<FlashcardProgress>> =
        dao.observeDue(courseId, currentEpochDay).map { items -> items.map(FlashcardProgressEntity::toModel) }

    /**
     * Session واقعی را از Glossary + Progress می‌سازد؛ کارت‌های هرگز دیده‌نشده نیز due محسوب می‌شوند.
     * limit از Sessionهای بسیار طولانی جلوگیری می‌کند و برای همه Courseها یک پیش‌فرض UX ثابت می‌سازد.
     */
    fun observeDueCards(
        bundle: CourseBundle,
        currentEpochDay: Long,
        limit: Int = DEFAULT_SESSION_SIZE
    ): Flow<List<Flashcard>> {
        require(limit > 0) { "review session limit must be positive" }
        val courseId = bundle.manifest.courseId
        require(courseId.isNotBlank()) { "courseId is required for review session" }
        val cards = SpacedReviewEngine.fromGlossary(bundle.glossary)

        return dao.observeCourse(courseId).map { entities ->
            val progress = entities
                .map(FlashcardProgressEntity::toModel)
                .associateBy(FlashcardProgress::cardId)
            SpacedReviewEngine.dueCards(cards, progress, currentEpochDay).take(limit)
        }
    }

    /**
     * UI Session باید لیست ثابت ابتدای جلسه داشته باشد؛ Rating هر کارت نباید index کارت‌های بعدی را جابه‌جا کند.
     * این متد اولین Snapshot Flow را می‌گیرد و برای همان Session ثابت نگه می‌دارد.
     */
    suspend fun loadDueCards(
        bundle: CourseBundle,
        currentEpochDay: Long,
        limit: Int = DEFAULT_SESSION_SIZE
    ): List<Flashcard> = observeDueCards(bundle, currentEpochDay, limit).first()

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

    companion object {
        /** پیش‌فرض 20 کارت، Session اولیه را کوتاه نگه می‌دارد و Course در صورت نیاز می‌تواند مقدار دیگری بدهد. */
        const val DEFAULT_SESSION_SIZE: Int = 20
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
