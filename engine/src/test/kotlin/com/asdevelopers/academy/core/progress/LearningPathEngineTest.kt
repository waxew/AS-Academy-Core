package com.asdevelopers.academy.core.progress

import com.asdevelopers.academy.core.validCourseBundle
import com.asdevelopers.academy.course.model.Chapter
import com.asdevelopers.academy.course.model.CourseLevel
import com.asdevelopers.academy.course.model.CourseLevelType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** قفل Level و مقصد ادامه یادگیری باید در تمام Course appها یکسان بماند. */
class LearningPathEngineTest {
    @Test
    fun `next level unlocks after previous level is complete`() {
        val base = validCourseBundle()
        val secondLevel = CourseLevel("sample-advanced", "sample", CourseLevelType.ADVANCED, "پیشرفته", 1)
        val secondChapter = Chapter("sample-advanced-intro", secondLevel.id, "ادامه", "سطح دوم", 0)
        val secondLesson = base.lessons.single().copy(
            id = "sample-lesson-002",
            chapterId = secondChapter.id,
            order = 0,
            blocks = base.lessons.single().blocks.map { it.copy(id = "sample-block-002") }
        )
        val bundle = base.copy(
            levels = base.levels + secondLevel,
            chapters = base.chapters + secondChapter,
            lessons = base.lessons + secondLesson
        )

        val locked = LearningPathEngine.buildDashboard(bundle, emptyList())
        assertFalse(locked.levels.last().isUnlocked)
        assertEquals(base.lessons.single().id, locked.nextLessonId)

        // رکورد قدیمی یا آزمایشی نباید Continue Learning را از قفل Level عبور دهد.
        val lockedLessonProgress = LessonProgress(
            lessonId = secondLesson.id,
            status = LessonStatus.IN_PROGRESS,
            progressPercent = 20,
            lastOpenedAtEpochMillis = 200,
            courseId = "sample"
        )
        assertEquals(
            base.lessons.single().id,
            LearningPathEngine.buildDashboard(bundle, listOf(lockedLessonProgress)).nextLessonId
        )

        val completedFirst = LessonProgress(
            lessonId = base.lessons.single().id,
            status = LessonStatus.COMPLETED,
            progressPercent = 100,
            courseId = "sample"
        )
        val unlocked = LearningPathEngine.buildDashboard(bundle, listOf(completedFirst))
        assertTrue(unlocked.levels.last().isUnlocked)
        assertEquals(secondLesson.id, unlocked.nextLessonId)
    }

    @Test
    fun `placement first lesson lookup follows level chapter and lesson order`() {
        val base = validCourseBundle()
        val advanced = CourseLevel("sample-advanced", "sample", CourseLevelType.ADVANCED, "پیشرفته", 1)
        val laterChapter = Chapter("sample-advanced-later", advanced.id, "بعد", "فصل دوم", 2)
        val firstChapter = Chapter("sample-advanced-first", advanced.id, "اول", "فصل اول", 1)
        val laterLesson = base.lessons.single().copy(
            id = "sample-lesson-later",
            chapterId = laterChapter.id,
            order = 0,
            blocks = base.lessons.single().blocks.map { it.copy(id = "sample-block-later") }
        )
        val secondLessonInFirstChapter = base.lessons.single().copy(
            id = "sample-lesson-first-2",
            chapterId = firstChapter.id,
            order = 2,
            blocks = base.lessons.single().blocks.map { it.copy(id = "sample-block-first-2") }
        )
        val firstLesson = base.lessons.single().copy(
            id = "sample-lesson-first-1",
            chapterId = firstChapter.id,
            order = 1,
            blocks = base.lessons.single().blocks.map { it.copy(id = "sample-block-first-1") }
        )
        val bundle = base.copy(
            levels = base.levels + advanced,
            chapters = base.chapters + laterChapter + firstChapter,
            lessons = base.lessons + laterLesson + secondLessonInFirstChapter + firstLesson
        )

        assertEquals(
            "sample-lesson-first-1",
            LearningPathEngine.firstLessonIdForLevelType(bundle, CourseLevelType.ADVANCED)
        )
        assertNull(LearningPathEngine.firstLessonIdForLevelType(bundle, CourseLevelType.SPECIALIST))
    }

    @Test
    fun `an empty locked level does not unlock the level after it`() {
        val base = validCourseBundle()
        val emptyLevel = CourseLevel("sample-empty", "sample", CourseLevelType.INTERMEDIATE, "میانی", 1)
        val thirdLevel = CourseLevel("sample-advanced", "sample", CourseLevelType.ADVANCED, "پیشرفته", 2)
        val thirdChapter = Chapter("sample-advanced-intro", thirdLevel.id, "ادامه", "سطح سوم", 0)
        val thirdLesson = base.lessons.single().copy(
            id = "sample-lesson-003",
            chapterId = thirdChapter.id,
            blocks = base.lessons.single().blocks.map { it.copy(id = "sample-block-003") }
        )
        val bundle = base.copy(
            levels = base.levels + emptyLevel + thirdLevel,
            chapters = base.chapters + thirdChapter,
            lessons = base.lessons + thirdLesson
        )

        val dashboard = LearningPathEngine.buildDashboard(bundle, emptyList())

        assertFalse(dashboard.levels[1].isUnlocked)
        assertFalse(dashboard.levels[2].isUnlocked)
        assertEquals(base.lessons.single().id, dashboard.nextLessonId)
    }
}
