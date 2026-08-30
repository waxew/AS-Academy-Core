package com.asdevelopers.academy.core.flashcard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** موتور مرور باید برای تمام Courseها زمان‌بندی deterministic و قابل پیش‌بینی تولید کند. */
class FlashcardReviewEngineTest {
    private val initialState = FlashcardReviewState(
        courseId = "sample-course",
        flashcardId = "sample-flashcard"
    )

    @Test
    fun `good first review schedules card for one day`() {
        val now = 1_000L
        val result = FlashcardReviewEngine.review(initialState, FlashcardRecallRating.GOOD, now)

        assertEquals(1, result.scheduledIntervalDays)
        assertEquals(1, result.state.repetitions)
        assertEquals(now, result.state.lastReviewedAtEpochMillis)
        assertTrue(result.state.dueAtEpochMillis > now)
    }

    @Test
    fun `easy first review creates a longer interval`() {
        val result = FlashcardReviewEngine.review(initialState, FlashcardRecallRating.EASY, 2_000L)

        assertEquals(4, result.scheduledIntervalDays)
        assertEquals(1, result.state.repetitions)
        assertTrue(result.state.easeFactor > initialState.easeFactor)
    }

    @Test
    fun `again resets repetitions and lowers ease without crossing minimum`() {
        val learned = initialState.copy(repetitions = 5, intervalDays = 30, easeFactor = 1.35)
        val result = FlashcardReviewEngine.review(learned, FlashcardRecallRating.AGAIN, 3_000L)

        assertEquals(0, result.state.repetitions)
        assertEquals(1, result.state.intervalDays)
        assertEquals(1.3, result.state.easeFactor)
    }

    @Test
    fun `due cards are filtered and sorted by oldest due time`() {
        val states = listOf(
            initialState.copy(flashcardId = "future", dueAtEpochMillis = 9_000L),
            initialState.copy(flashcardId = "second", dueAtEpochMillis = 2_000L),
            initialState.copy(flashcardId = "first", dueAtEpochMillis = 1_000L)
        )

        val due = FlashcardReviewEngine.dueCards(states, nowEpochMillis = 5_000L)

        assertEquals(listOf("first", "second"), due.map { it.flashcardId })
    }
}
