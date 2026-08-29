package com.asdevelopers.academy.core.progress

/** محاسبات پیشرفت در یک نقطه مرکزی نگهداری می‌شوند تا همه اپ‌ها رفتار یکسان داشته باشند. */
object ProgressCalculator {
    fun percent(completed: Int, total: Int): Int {
        if (total <= 0) return 0
        return ((completed.coerceIn(0, total) * 100.0) / total).toInt()
    }

    fun levelPercent(lessonIds: Collection<String>, progress: Collection<LessonProgress>): Int {
        if (lessonIds.isEmpty()) return 0
        val completed = progress.count { it.lessonId in lessonIds && it.status == LessonStatus.COMPLETED }
        return percent(completed, lessonIds.size)
    }
}
