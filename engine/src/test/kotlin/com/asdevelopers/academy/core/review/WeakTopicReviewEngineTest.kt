package com.asdevelopers.academy.core.review

import com.asdevelopers.academy.core.quiz.QuizScore
import com.asdevelopers.academy.core.validCourseBundle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** پیشنهاد مرور باید ضعف تکرارشونده را بالاتر از ضعف تک‌بار رتبه‌بندی کند. */
class WeakTopicReviewEngineTest {
    @Test
    fun `repeated weak tags rank matching lessons first`() {
        val base = validCourseBundle()
        val original = base.lessons.single()
        val bundle = base.copy(
            lessons = listOf(
                original.copy(id = "sample-lesson-basics", title = "Basics", order = 0, tags = setOf("basics")),
                original.copy(id = "sample-lesson-memory", title = "Memory", order = 1, tags = setOf("memory")),
                original.copy(id = "sample-lesson-unrelated", title = "Other", order = 2, tags = setOf("other"))
            )
        )
        val scores = listOf(
            score(setOf("basics", "memory")),
            score(setOf("basics"))
        )

        val recommendations = WeakTopicReviewEngine.recommendLessons(bundle, scores)

        assertEquals("sample-lesson-basics", recommendations.first().lessonId)
        assertEquals(2, recommendations.first().priority)
        assertEquals("sample-lesson-memory", recommendations[1].lessonId)
        assertEquals(1, recommendations[1].priority)
        assertTrue(recommendations.none { it.lessonId == "sample-lesson-unrelated" })
    }

    @Test
    fun `empty weakness produces no recommendations`() {
        val recommendations = WeakTopicReviewEngine.recommendLessons(
            validCourseBundle(),
            listOf(score(emptySet()))
        )
        assertTrue(recommendations.isEmpty())
    }

    private fun score(weakTags: Set<String>) = QuizScore(
        scorePercent = if (weakTags.isEmpty()) 100 else 50,
        earnedPoints = if (weakTags.isEmpty()) 2 else 1,
        totalPoints = 2,
        correctQuestionIds = emptySet(),
        wrongQuestionIds = emptySet(),
        weakTags = weakTags,
        passed = weakTags.isEmpty()
    )
}
