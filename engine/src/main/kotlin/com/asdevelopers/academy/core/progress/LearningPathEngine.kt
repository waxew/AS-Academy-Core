package com.asdevelopers.academy.core.progress

import com.asdevelopers.academy.core.content.CourseBundle

/** پیشرفت و وضعیت بازبودن یک Level برای Dashboard مشترک. */
data class LevelProgressState(
    val levelId: String,
    val totalLessons: Int,
    val completedLessons: Int,
    val percent: Int,
    val isUnlocked: Boolean
)

/** خروجی واحد صفحه خانه/پیشرفت و دکمه «ادامه یادگیری». */
data class LearningDashboard(
    val totalLessons: Int,
    val completedLessons: Int,
    val percent: Int,
    val nextLessonId: String?,
    val levels: List<LevelProgressState>
)

/**
 * ترتیب مسیر، بازشدن Level و مقصد ادامه یادگیری در JVM محاسبه می‌شود تا همه Hostها رفتار یکسان داشته باشند.
 */
object LearningPathEngine {
    fun buildDashboard(
        bundle: CourseBundle,
        progress: Collection<LessonProgress>,
        unlockThresholdPercent: Int = 80
    ): LearningDashboard {
        require(unlockThresholdPercent in 0..100) { "unlockThresholdPercent must be between 0 and 100" }

        val courseId = bundle.manifest.courseId
        val lessonIds = bundle.lessons.mapTo(mutableSetOf()) { it.id }
        // رکوردهای Migration با Course خالی تا زمان نسبت‌دادن به Course از روی Stable ID قابل استفاده می‌مانند.
        val relevantProgress = progress.filter {
            it.lessonId in lessonIds && (it.courseId.isBlank() || it.courseId == courseId)
        }
        // ممکن است پس از Migration، رکورد Course خالی و رکورد جدید همان درس هم‌زمان وجود داشته باشند.
        // رکورد Course واقعی اولویت دارد و سپس تازه‌ترین رکورد انتخاب می‌شود تا ترتیب Collection نتیجه را عوض نکند.
        val progressByLesson = relevantProgress.groupBy(LessonProgress::lessonId).mapValues { (_, records) ->
            records.maxWith(
                compareBy<LessonProgress> { if (it.courseId == courseId) 1 else 0 }
                    .thenBy { it.lastOpenedAtEpochMillis ?: 0L }
            )
        }
        val orderedLevels = bundle.levels.sortedBy { it.order }
        val orderedLessonsByLevel = orderedLevels.associate { level ->
            val chapterIds = bundle.chapters.filter { it.levelId == level.id }
                .sortedBy { it.order }
                .map { it.id }
            level.id to chapterIds.flatMap { chapterId ->
                bundle.lessons.filter { it.chapterId == chapterId }.sortedBy { it.order }
            }
        }

        val levelStates = mutableListOf<LevelProgressState>()
        orderedLevels.forEachIndexed { index, level ->
            val lessons = orderedLessonsByLevel[level.id].orEmpty()
            val completed = lessons.count { progressByLesson[it.id]?.status == LessonStatus.COMPLETED }
            val percent = if (lessons.isEmpty()) 0 else completed * 100 / lessons.size
            val previous = levelStates.lastOrNull()
            // Level اول همیشه باز است؛ Level بعدی با 80٪ پیشرفت Level قبل باز می‌شود.
            val unlocked = index == 0 || previous?.let {
                // Level خالی فقط وقتی مسیر را عبور می‌دهد که خودش در زنجیره قبلی باز شده باشد.
                it.isUnlocked && (it.totalLessons == 0 || it.percent >= unlockThresholdPercent)
            } == true
            levelStates += LevelProgressState(level.id, lessons.size, completed, percent, unlocked)
        }

        val orderedLessons = orderedLevels.flatMap { orderedLessonsByLevel[it.id].orEmpty() }
        val unlockedLevelIds = levelStates.filter(LevelProgressState::isUnlocked).mapTo(mutableSetOf()) { it.levelId }
        val unlockedLessonIds = unlockedLevelIds.flatMapTo(mutableSetOf()) { levelId ->
            orderedLessonsByLevel[levelId].orEmpty().map { it.id }
        }
        // Continue Learning هیچ‌گاه کاربر را به Level قفل‌شده نمی‌فرستد؛ حتی اگر رکورد آزمایشی قدیمی وجود داشته باشد.
        val recentInProgress = progressByLesson.values
            .filter { it.status == LessonStatus.IN_PROGRESS && it.lessonId in unlockedLessonIds }
            .maxByOrNull { it.lastOpenedAtEpochMillis ?: 0L }
            ?.lessonId
        val nextLessonId = recentInProgress ?: orderedLessons.firstOrNull { lesson ->
            lesson.id in unlockedLessonIds && progressByLesson[lesson.id]?.status != LessonStatus.COMPLETED
        }?.id

        val completedLessons = orderedLessons.count { progressByLesson[it.id]?.status == LessonStatus.COMPLETED }
        val totalLessons = orderedLessons.size
        return LearningDashboard(
            totalLessons = totalLessons,
            completedLessons = completedLessons,
            percent = if (totalLessons == 0) 0 else completedLessons * 100 / totalLessons,
            nextLessonId = nextLessonId,
            levels = levelStates
        )
    }
}
