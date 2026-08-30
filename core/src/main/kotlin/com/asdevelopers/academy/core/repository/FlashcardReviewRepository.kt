package com.asdevelopers.academy.core.repository

import com.asdevelopers.academy.core.database.FlashcardReviewDao
import com.asdevelopers.academy.core.database.FlashcardReviewEntity
import com.asdevelopers.academy.core.flashcard.FlashcardRecallRating
import com.asdevelopers.academy.core.flashcard.FlashcardReviewEngine
import com.asdevelopers.academy.core.flashcard.FlashcardReviewState
import com.asdevelopers.academy.course.model.Flashcard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository مشترک مرور Flashcard، Engine خالص را به Room متصل می‌کند.
 *
 * Courseها نباید State یا الگوریتم زمان‌بندی جداگانه بسازند؛ فقط Flashcardهای محتوایی را تحویل Core می‌دهند.
 */
class FlashcardReviewRepository(
    private val dao: FlashcardReviewDao
) {
    /** State یک کارت را برای UI یا ViewModel به‌صورت reactive منتشر می‌کند. */
    fun observeState(courseId: String, flashcardId: String): Flow<FlashcardReviewState?> =
        dao.observe(courseId, flashcardId).map { entity -> entity?.toModel() }

    /** تمام Stateهای یک Course را بر اساس موعد مرور برمی‌گرداند. */
    fun observeCourse(courseId: String): Flow<List<FlashcardReviewState>> =
        dao.observeCourse(courseId).map { items -> items.map(FlashcardReviewEntity::toModel) }

    /** صف کارت‌های موعدرسیده برای Dashboard و صفحه مرور روزانه. */
    fun observeDue(
        courseId: String,
        nowEpochMillis: Long,
        limit: Int = 100
    ): Flow<List<FlashcardReviewState>> {
        require(courseId.isNotBlank()) { "courseId is required" }
        require(nowEpochMillis >= 0L) { "nowEpochMillis cannot be negative" }
        return dao.observeDue(courseId, nowEpochMillis, limit.coerceIn(1, 500))
            .map { items -> items.map(FlashcardReviewEntity::toModel) }
    }

    /**
     * کارت‌های Course را با موعد صفر Seed می‌کند تا کارت دیده‌نشده از همان ابتدا در صف مرور قرار گیرد.
     * OnConflictStrategy.IGNORE باعث می‌شود Import یا Update Course هیچ Review History قبلی را بازنویسی نکند.
     */
    suspend fun seedCourse(courseId: String, cards: Collection<Flashcard>) {
        require(courseId.isNotBlank()) { "courseId is required" }
        val states = cards
            .asSequence()
            .filter { it.courseId == courseId }
            .distinctBy(Flashcard::id)
            .map { card -> FlashcardReviewState(courseId = courseId, flashcardId = card.id).toEntity() }
            .toList()
        if (states.isNotEmpty()) dao.insertIfAbsent(states)
    }

    /**
     * Rating کاربر را به State بعدی تبدیل و همان نتیجه را پایدار می‌کند.
     * اگر کارت هنوز Seed نشده باشد، State اولیه به‌طور خودکار ساخته می‌شود.
     */
    suspend fun recordReview(
        courseId: String,
        flashcardId: String,
        rating: FlashcardRecallRating,
        reviewedAtEpochMillis: Long
    ): FlashcardReviewState {
        require(courseId.isNotBlank()) { "courseId is required" }
        require(flashcardId.isNotBlank()) { "flashcardId is required" }

        val current = dao.get(courseId, flashcardId)?.toModel()
            ?: FlashcardReviewState(courseId = courseId, flashcardId = flashcardId)
        val result = FlashcardReviewEngine.review(current, rating, reviewedAtEpochMillis)
        dao.upsert(result.state.toEntity())
        return result.state
    }

    /** Restore یا import داخلی می‌تواند State معتبر را مستقیماً ذخیره کند. */
    suspend fun save(state: FlashcardReviewState) {
        require(state.courseId.isNotBlank()) { "courseId is required" }
        require(state.flashcardId.isNotBlank()) { "flashcardId is required" }
        dao.upsert(state.toEntity())
    }
}

/** تبدیل Room به مدل مستقل Engine در یک نقطه مرکزی انجام می‌شود. */
private fun FlashcardReviewEntity.toModel(): FlashcardReviewState = FlashcardReviewState(
    courseId = courseId,
    flashcardId = flashcardId,
    repetitions = repetitions,
    intervalDays = intervalDays,
    easeFactor = easeFactor,
    dueAtEpochMillis = dueAt,
    lastReviewedAtEpochMillis = lastReviewedAt
)

/** تبدیل Engine به Room از نشت جزئیات دیتابیس به Featureها جلوگیری می‌کند. */
private fun FlashcardReviewState.toEntity(): FlashcardReviewEntity = FlashcardReviewEntity(
    courseId = courseId,
    flashcardId = flashcardId,
    repetitions = repetitions,
    intervalDays = intervalDays,
    easeFactor = easeFactor,
    dueAt = dueAtEpochMillis,
    lastReviewedAt = lastReviewedAtEpochMillis
)
