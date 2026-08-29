package com.asdevelopers.academy.core.progress

/** موتور محاسبه Progress که هیچ وابستگی به Room یا UI ندارد. */
object ProgressEngine {

    /** پیشرفت یک درس را بر اساس آخرین Block مشاهده‌شده محاسبه می‌کند. */
    fun updateFromBlock(
        current: LessonProgress,
        viewedBlockIndex: Int,
        totalBlocks: Int,
        additionalStudySeconds: Long,
        openedAtEpochMillis: Long
    ): LessonProgress {
        // درس بدون Block یک Package نامعتبر است و نباید وارد موتور شود.
        require(totalBlocks > 0) { "totalBlocks must be positive" }
        // Index باید داخل محدوده محتوای همان درس باشد.
        require(viewedBlockIndex in 0 until totalBlocks) { "viewedBlockIndex is outside lesson blocks" }
        // زمان افزوده‌شده منفی پذیرفته نمی‌شود.
        require(additionalStudySeconds >= 0) { "additionalStudySeconds cannot be negative" }

        // بیشترین Block دیده‌شده حفظ می‌شود تا برگشت کاربر درصد را کم نکند.
        val lastBlock = maxOf(current.lastBlockIndex, viewedBlockIndex)
        // مشاهده Block آخر به معنی تکمیل مسیر مطالعه درس است.
        val calculatedPercent = (((lastBlock + 1).toDouble() / totalBlocks) * 100).toInt().coerceIn(0, 100)
        // وضعیت تکمیل فقط در صددرصد ثبت می‌شود؛ Review دستی می‌تواند بعداً آن را تغییر دهد.
        val nextStatus = if (calculatedPercent == 100) LessonStatus.COMPLETED else LessonStatus.IN_PROGRESS

        return current.copy(
            status = nextStatus,
            progressPercent = calculatedPercent,
            lastBlockIndex = lastBlock,
            studySeconds = current.studySeconds + additionalStudySeconds,
            lastOpenedAtEpochMillis = openedAtEpochMillis,
            completedAtEpochMillis = current.completedAtEpochMillis
                ?: openedAtEpochMillis.takeIf { nextStatus == LessonStatus.COMPLETED }
        )
    }

    /** خلاصه Progress را حتی برای لیست خالی بدون تقسیم بر صفر می‌سازد. */
    fun summarize(progress: List<LessonProgress>, totalLessons: Int): ProgressSummary {
        // تعداد کل از تعداد رکوردهای واقعی کمتر نمی‌تواند باشد.
        require(totalLessons >= progress.size) { "totalLessons cannot be smaller than progress records" }
        // رکورد تکراری یک درس Summary را دو بار می‌شمارد و باید پیش از محاسبه آشکار شود.
        require(progress.map { it.courseId to it.lessonId }.distinct().size == progress.size) {
            "progress contains duplicate course/lesson records"
        }
        val completed = progress.count { it.status == LessonStatus.COMPLETED }
        val inProgress = progress.count { it.status == LessonStatus.IN_PROGRESS }
        val needsReview = progress.count { it.status == LessonStatus.NEEDS_REVIEW }
        val percent = if (totalLessons == 0) 0 else (completed * 100 / totalLessons)
        return ProgressSummary(totalLessons, completed, inProgress, needsReview, percent)
    }
}
