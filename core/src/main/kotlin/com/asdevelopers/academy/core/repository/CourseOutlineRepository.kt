package com.asdevelopers.academy.core.repository

import com.asdevelopers.academy.core.content.CourseBundle
import com.asdevelopers.academy.core.database.LessonProgressEntity
import com.asdevelopers.academy.core.database.ProgressDao
import com.asdevelopers.academy.core.progress.CourseOutline
import com.asdevelopers.academy.core.progress.CourseOutlineEngine
import com.asdevelopers.academy.core.progress.LessonProgress
import com.asdevelopers.academy.core.progress.LessonStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository عمومی Home/Outline، Room را به CourseOutlineEngine متصل می‌کند.
 * Hostهای Course نباید DAO، Migration legacy یا تبدیل Entity را برای ساخت Home دوباره پیاده‌سازی کنند.
 */
class CourseOutlineRepository(private val progressDao: ProgressDao) {
    fun observe(
        bundle: CourseBundle,
        unlockThresholdPercent: Int = 80
    ): Flow<CourseOutline> {
        val courseId = bundle.manifest.courseId
        require(courseId.isNotBlank()) { "courseId is required for course outline" }
        return progressDao.observeCourseWithLegacy(courseId).map { entities ->
            CourseOutlineEngine.build(
                bundle = bundle,
                progress = entities.map(LessonProgressEntity::toModel),
                unlockThresholdPercent = unlockThresholdPercent
            )
        }
    }
}

/** Entityهای Migration قدیمی نیز با Status امن به مدل Engine تبدیل می‌شوند. */
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
