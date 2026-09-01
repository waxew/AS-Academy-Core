package com.asdevelopers.academy.core.repository

import com.asdevelopers.academy.core.database.LessonProgressEntity
import com.asdevelopers.academy.core.database.ProgressDao
import com.asdevelopers.academy.core.progress.LessonProgress
import com.asdevelopers.academy.core.progress.LessonStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Workflow مشترک Lesson Progress.
 * Course Hostها فقط Stable ID و زمان/موقعیت مطالعه را می‌دهند و نباید Entity یا قواعد Status را تکرار کنند.
 */
class LessonProgressRepository(private val dao: ProgressDao) {

    /** وضعیت یک Lesson به‌صورت مدل Engine و نه Entity دیتابیس در اختیار UI قرار می‌گیرد. */
    fun observe(courseId: String, lessonId: String): Flow<LessonProgress?> {
        require(courseId.isNotBlank()) { "courseId is required for lesson progress" }
        require(lessonId.isNotBlank()) { "lessonId is required for lesson progress" }
        return dao.observe(courseId, lessonId).map { it?.toModel() }
    }

    /**
     * بازشدن درس آخرین زمان مطالعه را ثبت می‌کند.
     * درس تکمیل‌شده دوباره IN_PROGRESS نمی‌شود؛ فقط lastOpenedAt آن تازه می‌شود.
     */
    suspend fun markOpened(
        courseId: String,
        lessonId: String,
        openedAtEpochMillis: Long
    ): LessonProgress {
        requireIds(courseId, lessonId)
        val current = dao.observe(courseId, lessonId).first()?.toModel()
        val updated = (current ?: LessonProgress(courseId = courseId, lessonId = lessonId)).copy(
            status = when (current?.status) {
                LessonStatus.COMPLETED -> LessonStatus.COMPLETED
                LessonStatus.NEEDS_REVIEW -> LessonStatus.NEEDS_REVIEW
                else -> LessonStatus.IN_PROGRESS
            },
            lastOpenedAtEpochMillis = openedAtEpochMillis
        )
        dao.upsert(updated.toEntity())
        return updated
    }

    /**
     * موقعیت مطالعه از index Block به درصد پایدار تبدیل می‌شود.
     * درصد هیچ‌گاه با Scroll عقب کاهش نمی‌یابد و Study Time به مقدار قبلی افزوده می‌شود.
     */
    suspend fun savePosition(
        courseId: String,
        lessonId: String,
        lastBlockIndex: Int,
        totalBlocks: Int,
        studySecondsDelta: Long,
        updatedAtEpochMillis: Long
    ): LessonProgress {
        requireIds(courseId, lessonId)
        require(lastBlockIndex >= 0) { "lastBlockIndex cannot be negative" }
        require(totalBlocks > 0) { "totalBlocks must be positive" }
        require(studySecondsDelta >= 0) { "studySecondsDelta cannot be negative" }

        val current = dao.observe(courseId, lessonId).first()?.toModel()
        val calculatedPercent = (((lastBlockIndex + 1).coerceAtMost(totalBlocks)) * 100 / totalBlocks)
            .coerceIn(0, 99)
        val updated = (current ?: LessonProgress(courseId = courseId, lessonId = lessonId)).copy(
            status = when (current?.status) {
                LessonStatus.COMPLETED -> LessonStatus.COMPLETED
                LessonStatus.NEEDS_REVIEW -> LessonStatus.NEEDS_REVIEW
                else -> LessonStatus.IN_PROGRESS
            },
            progressPercent = if (current?.status == LessonStatus.COMPLETED) 100 else maxOf(
                current?.progressPercent ?: 0,
                calculatedPercent
            ),
            lastBlockIndex = maxOf(current?.lastBlockIndex ?: 0, lastBlockIndex),
            studySeconds = (current?.studySeconds ?: 0L) + studySecondsDelta,
            lastOpenedAtEpochMillis = updatedAtEpochMillis
        )
        dao.upsert(updated.toEntity())
        return updated
    }

    /** Explicit completion تنها مسیر تبدیل Lesson به 100٪ است. */
    suspend fun markCompleted(
        courseId: String,
        lessonId: String,
        completedAtEpochMillis: Long,
        lastBlockIndex: Int = 0
    ): LessonProgress {
        requireIds(courseId, lessonId)
        val current = dao.observe(courseId, lessonId).first()?.toModel()
        val updated = (current ?: LessonProgress(courseId = courseId, lessonId = lessonId)).copy(
            status = LessonStatus.COMPLETED,
            progressPercent = 100,
            lastBlockIndex = maxOf(current?.lastBlockIndex ?: 0, lastBlockIndex),
            lastOpenedAtEpochMillis = completedAtEpochMillis,
            completedAtEpochMillis = current?.completedAtEpochMillis ?: completedAtEpochMillis
        )
        dao.upsert(updated.toEntity())
        return updated
    }

    /** Quiz/Review می‌تواند Lesson تکمیل‌شده را برای مرور دوباره علامت‌گذاری کند بدون حذف Completion time. */
    suspend fun markNeedsReview(
        courseId: String,
        lessonId: String,
        updatedAtEpochMillis: Long
    ): LessonProgress {
        requireIds(courseId, lessonId)
        val current = dao.observe(courseId, lessonId).first()?.toModel()
        val updated = (current ?: LessonProgress(courseId = courseId, lessonId = lessonId)).copy(
            status = LessonStatus.NEEDS_REVIEW,
            lastOpenedAtEpochMillis = updatedAtEpochMillis
        )
        dao.upsert(updated.toEntity())
        return updated
    }

    private fun requireIds(courseId: String, lessonId: String) {
        require(courseId.isNotBlank()) { "courseId is required for lesson progress" }
        require(lessonId.isNotBlank()) { "lessonId is required for lesson progress" }
    }
}

private fun LessonProgressEntity.toModel(): LessonProgress = LessonProgress(
    lessonId = lessonId,
    status = runCatching { LessonStatus.valueOf(status) }.getOrDefault(LessonStatus.NOT_STARTED),
    progressPercent = progressPercent,
    lastBlockIndex = lastBlockIndex,
    studySeconds = studySeconds,
    lastOpenedAtEpochMillis = lastOpenedAt,
    completedAtEpochMillis = completedAt,
    courseId = courseId
)

private fun LessonProgress.toEntity(): LessonProgressEntity = LessonProgressEntity(
    courseId = courseId,
    lessonId = lessonId,
    status = status.name,
    progressPercent = progressPercent,
    lastBlockIndex = lastBlockIndex,
    studySeconds = studySeconds,
    lastOpenedAt = lastOpenedAtEpochMillis,
    completedAt = completedAtEpochMillis
)
