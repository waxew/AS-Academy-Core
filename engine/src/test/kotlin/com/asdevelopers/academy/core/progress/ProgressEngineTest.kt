package com.asdevelopers.academy.core.progress

import kotlin.test.Test
import kotlin.test.assertEquals

/** Progress با برگشت کاربر به Block قبلی کاهش پیدا نمی‌کند. */
class ProgressEngineTest {
    @Test
    fun `viewing final block completes lesson`() {
        val initial = LessonProgress(lessonId = "sample-lesson-001", courseId = "sample")
        val result = ProgressEngine.updateFromBlock(initial, 3, 4, 60, 1_000)
        assertEquals(100, result.progressPercent)
        assertEquals(LessonStatus.COMPLETED, result.status)
        assertEquals(1_000, result.completedAtEpochMillis)
    }

    @Test
    fun `opening an older block keeps highest progress`() {
        val current = LessonProgress(
            lessonId = "sample-lesson-001",
            courseId = "sample",
            status = LessonStatus.IN_PROGRESS,
            progressPercent = 75,
            lastBlockIndex = 2
        )
        val result = ProgressEngine.updateFromBlock(current, 0, 4, 10, 2_000)
        assertEquals(75, result.progressPercent)
        assertEquals(2, result.lastBlockIndex)
    }
}
