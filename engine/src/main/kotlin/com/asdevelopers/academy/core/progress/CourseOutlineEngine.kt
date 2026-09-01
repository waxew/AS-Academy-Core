package com.asdevelopers.academy.core.progress

import com.asdevelopers.academy.core.content.CourseBundle
import com.asdevelopers.academy.course.model.CourseLevelType

/** وضعیت یک Lesson در مدل آماده نمایش Home/Outline. */
data class CourseOutlineLesson(
    val id: String,
    val title: String,
    val summary: String,
    val order: Int,
    val estimatedMinutes: Int,
    val status: LessonStatus,
    val progressPercent: Int
)

/** فصل همراه Progress تجمیعی و درس‌های مرتب‌شده. */
data class CourseOutlineChapter(
    val id: String,
    val title: String,
    val description: String,
    val order: Int,
    val totalLessons: Int,
    val completedLessons: Int,
    val progressPercent: Int,
    val lessons: List<CourseOutlineLesson>
)

/** Level آماده نمایش؛ قفل مسیر از همان LearningPathEngine مرکزی گرفته می‌شود. */
data class CourseOutlineLevel(
    val id: String,
    val type: CourseLevelType,
    val title: String,
    val description: String,
    val order: Int,
    val isUnlocked: Boolean,
    val totalLessons: Int,
    val completedLessons: Int,
    val progressPercent: Int,
    val chapters: List<CourseOutlineChapter>
)

/** مدل یکپارچه Home تا Course Host مجبور به join کردن Level/Chapter/Lesson/Progress نشود. */
data class CourseOutline(
    val courseId: String,
    val title: String,
    val totalLessons: Int,
    val completedLessons: Int,
    val progressPercent: Int,
    val nextLessonId: String?,
    val levels: List<CourseOutlineLevel>
)

/**
 * Engine مشترک ساخت Outline سلسله‌مراتبی Course.
 * ترتیب، Progress و Level locking یک‌بار در Core محاسبه می‌شوند و Courseها فقط مدل خروجی را نمایش می‌دهند.
 */
object CourseOutlineEngine {
    fun build(
        bundle: CourseBundle,
        progress: Collection<LessonProgress>,
        unlockThresholdPercent: Int = 80
    ): CourseOutline {
        val dashboard = LearningPathEngine.buildDashboard(bundle, progress, unlockThresholdPercent)
        val courseId = bundle.manifest.courseId
        val lessonIds = bundle.lessons.mapTo(mutableSetOf()) { it.id }
        val progressByLesson = progress
            .filter { it.lessonId in lessonIds && (it.courseId.isBlank() || it.courseId == courseId) }
            .groupBy(LessonProgress::lessonId)
            .mapValues { (_, records) ->
                records.maxWith(
                    compareBy<LessonProgress> { if (it.courseId == courseId) 1 else 0 }
                        .thenBy { it.lastOpenedAtEpochMillis ?: 0L }
                )
            }
        val levelStateById = dashboard.levels.associateBy(LevelProgressState::levelId)

        val outlineLevels = bundle.levels.sortedBy { it.order }.map { level ->
            val chapters = bundle.chapters
                .filter { it.levelId == level.id }
                .sortedBy { it.order }
                .map { chapter ->
                    val lessons = bundle.lessons
                        .filter { it.chapterId == chapter.id }
                        .sortedBy { it.order }
                        .map { lesson ->
                            val saved = progressByLesson[lesson.id]
                            CourseOutlineLesson(
                                id = lesson.id,
                                title = lesson.title,
                                summary = lesson.summary,
                                order = lesson.order,
                                estimatedMinutes = lesson.estimatedMinutes,
                                status = saved?.status ?: LessonStatus.NOT_STARTED,
                                progressPercent = saved?.progressPercent ?: 0
                            )
                        }
                    val completed = lessons.count { it.status == LessonStatus.COMPLETED }
                    CourseOutlineChapter(
                        id = chapter.id,
                        title = chapter.title,
                        description = chapter.description,
                        order = chapter.order,
                        totalLessons = lessons.size,
                        completedLessons = completed,
                        progressPercent = percentage(completed, lessons.size),
                        lessons = lessons
                    )
                }
            val state = levelStateById.getValue(level.id)
            CourseOutlineLevel(
                id = level.id,
                type = level.type,
                title = level.title,
                description = level.description,
                order = level.order,
                isUnlocked = state.isUnlocked,
                totalLessons = state.totalLessons,
                completedLessons = state.completedLessons,
                progressPercent = state.percent,
                chapters = chapters
            )
        }

        return CourseOutline(
            courseId = courseId,
            title = bundle.manifest.titleFa,
            totalLessons = dashboard.totalLessons,
            completedLessons = dashboard.completedLessons,
            progressPercent = dashboard.percent,
            nextLessonId = dashboard.nextLessonId,
            levels = outlineLevels
        )
    }

    private fun percentage(completed: Int, total: Int): Int =
        if (total == 0) 0 else completed * 100 / total
}
