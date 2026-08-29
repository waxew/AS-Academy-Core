package com.asdevelopers.academy.core.quiz

import org.junit.Assert.assertEquals
import org.junit.Test

class QuizScorerTest {
    @Test
    fun calculatesScoreAndWeakTags() {
        val quiz = Quiz("q", null, "Test", 70, listOf(
            QuizQuestion("q1", QuestionType.MULTIPLE_CHOICE, "one", "", setOf("scope"), listOf(QuizAnswer("a", "A", true), QuizAnswer("b", "B", false))),
            QuizQuestion("q2", QuestionType.MULTIPLE_CHOICE, "two", "", setOf("promise"), listOf(QuizAnswer("a", "A", true), QuizAnswer("b", "B", false)))
        ))
        val score = QuizScorer.score(quiz, mapOf("q1" to "a", "q2" to "b"))
        assertEquals(50, score.percent)
        assertEquals(listOf("promise"), score.weakTags)
    }
}
