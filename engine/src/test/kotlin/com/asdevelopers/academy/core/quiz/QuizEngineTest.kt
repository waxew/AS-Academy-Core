package com.asdevelopers.academy.core.quiz

import com.asdevelopers.academy.core.validCourseBundle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Quiz Engine باید در همه Courseها نتیجه دقیق یکسان بسازد. */
class QuizEngineTest {
    @Test
    fun `exact correct answer passes quiz`() {
        val quiz = validCourseBundle().quizzes.single()
        val result = QuizEngine.score(
            quiz,
            listOf(QuestionSubmission("sample-question-001", setOf("sample-answer-001")))
        )
        assertEquals(100, result.scorePercent)
        assertTrue(result.passed)
        assertTrue(result.weakTags.isEmpty())
    }

    @Test
    fun `wrong answer exposes weak topic`() {
        val quiz = validCourseBundle().quizzes.single()
        val result = QuizEngine.score(
            quiz,
            listOf(QuestionSubmission("sample-question-001", setOf("sample-answer-002")))
        )
        assertEquals(0, result.scorePercent)
        assertFalse(result.passed)
        assertEquals(setOf("basics"), result.weakTags)
    }

    @Test
    fun `fill code compares normalized text answer`() {
        val question = QuizQuestion(
            id = "sample-fill-001",
            type = QuestionType.FILL_CODE,
            question = "کد را کامل کنید",
            explanation = "پاسخ نمونه",
            answers = listOf(QuizAnswer("sample-fill-answer", "const value = 1", true))
        )
        val quiz = validCourseBundle().quizzes.single().copy(questions = listOf(question))
        val result = QuizEngine.score(
            quiz,
            listOf(QuestionSubmission(question.id, textAnswer = "const value = 1  \n"))
        )
        assertEquals(100, result.scorePercent)
    }

    @Test
    fun `ordered question requires exact answer order`() {
        val question = QuizQuestion(
            id = "sample-order-001",
            type = QuestionType.ORDER_STEPS,
            question = "مرتب کنید",
            explanation = "ترتیب درست",
            answers = listOf(
                QuizAnswer("sample-order-second", "دوم", false, order = 1),
                QuizAnswer("sample-order-first", "اول", false, order = 0)
            )
        )
        val quiz = validCourseBundle().quizzes.single().copy(questions = listOf(question))
        val result = QuizEngine.score(
            quiz,
            listOf(QuestionSubmission(question.id, orderedAnswerIds = listOf("sample-order-first", "sample-order-second")))
        )
        assertTrue(result.passed)
    }
}
