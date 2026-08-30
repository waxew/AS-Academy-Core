package com.asdevelopers.academy.core.progress

import com.asdevelopers.academy.core.validCourseBundle
import com.asdevelopers.academy.course.model.Chapter
import com.asdevelopers.academy.course.model.CourseLevel
import com.asdevelopers.academy.course.model.CourseLevelType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Outline باید ترتیب محتوای Course، Progress و قفل Level را بدون منطق Host تولید کند. */
class CourseOutlineEngineTest {
    @Test
    fun `outline groups lessons under ordered chapters and levels`() {
        val base = validCourseBundle()
        val advanced = CourseLevel("sample-advanced", "sample", CourseLevelType.ADVANCED, "پیشرفته", 1)
        val advancedChapter = Chapter("sample-advanced-chapter", advanced.id, "فصل پیشرفته", "ادامه مسیر", 0)
        val advancedLesson = base.lessons.single().copy(
            id = "sample-lesson-advanced",
            chapterId = advancedChapter.id,
            order = 0,
            title = "درس پیشرفته",
            blocks = base.lessons.single().blocks.map { it.copy(id = "sample-block-advanced") }
        )
        val bundle = base.copy(
            levels = base.levels + advanced,
            chapters = base.chapters + advancedChapter,
            lessons = base.lessons + advancedLesson
        )
        val firstLesson = base.lessons.single()
        val progress = listOf(
            LessonProgress(
                courseId = "sample",
                lessonId = firstLesson.id,
                status = LessonStatus.COMPLETED,
                progressPercent = 100,
                completedAtEpochMillis = 10
            )
        )

        val outline = CourseOutlineEngine.build(bundle, progress)

        assertEquals(2, outline.levels.size)
        assertEquals(firstLesson.id, outline.levels[0].chapters.single().lessons.single().id)
        assertEquals(advancedLesson.id, outline.levels[1].chapters.single().lessons.single().id)
        assertEquals(1, outline.completedLessons)
        assertEquals(50, outline.progressPercent)
        assertTrue(outline.levels[1].isUnlocked)
        assertEquals(advancedLesson.id, outline.nextLessonId)
    }

    @Test
    fun `outline keeps later level locked until threshold`() {
        val base = validCourseBundle()
        val advanced = CourseLevel("sample-advanced", "sample", CourseLevelType.ADVANCED, "پیشرفته", 1)
        val advancedChapter = Chapter("sample-advanced-chapter", advanced.id, "فصل پیشرفته", "ادامه مسیر", 0)
        val advancedLesson = base.lessons.single().copy(
            id = "sample-lesson-advanced",
            chapterId = advancedChapter.id,
            blocks = base.lessons.single().blocks.map { it.copy(id = "sample-block-advanced") }
        )
        val bundle = base.copy(
            levels = base.levels + advanced,
            chapters = base.chapters + advancedChapter,
            lessons = base.lessons + advancedLesson
        )

        val outline = CourseOutlineEngine.build(bundle, emptyList())

        assertTrue(outline.levels.first().isUnlocked)
        assertFalse(outline.levels.last().isUnlocked)
        assertEquals(base.lessons.single().id, outline.nextLessonId)
    }

    @Test
    fun `course specific progress wins over legacy duplicate`() {
        val bundle = validCourseBundle()
        val lessonId = bundle.lessons.single().id
        val outline = CourseOutlineEngine.build(
            bundle,
            listOf(
                LessonProgress(
                    courseId = "",
                    lessonId = lessonId,
                    status = LessonStatus.COMPLETED,
                    progressPercent = 100,
                    lastOpenedAtEpochMillis = 500
                ),
                LessonProgress(
                    courseId = "sample",
                    lessonId = lessonId,
                    status = LessonStatus.IN_PROGRESS,
                    progressPercent = 60,
                    lastOpenedAtEpochMillis = 100
                )
            )
        )

        val lesson = outline.levels.single().chapters.single().lessons.single()
        assertEquals(LessonStatus.IN_PROGRESS, lesson.status)
        assertEquals(60, lesson.progressPercent)
    }
}
