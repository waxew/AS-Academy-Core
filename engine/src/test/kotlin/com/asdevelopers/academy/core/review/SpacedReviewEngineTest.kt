package com.asdevelopers.academy.core.review

import com.asdevelopers.academy.course.model.GlossaryEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** مرور فاصله‌دار باید deterministic باشد تا همه Courseها زمان‌بندی یکسان داشته باشند. */
class SpacedReviewEngineTest {
    @Test
    fun `glossary entries become reusable flashcards`() {
        val cards = SpacedReviewEngine.fromGlossary(
            listOf(
                GlossaryEntry(
                    id = "sample-glossary-algorithm",
                    courseId = "sample",
                    term = "Algorithm",
                    definition = "گام‌های حل مسئله",
                    aliases = listOf("الگوریتم"),
                    tags = setOf("algorithm")
                )
            )
        )

        assertEquals(1, cards.size)
        assertEquals("flashcard-sample-glossary-algorithm", cards.single().id)
        assertEquals("Algorithm", cards.single().front)
        assertEquals(setOf("algorithm"), cards.single().tags)
    }

    @Test
    fun `good reviews expand interval while again resets repetitions`() {
        val day = 20_000L
        val first = SpacedReviewEngine.schedule(
            cardId = "flashcard-one",
            rating = ReviewRating.GOOD,
            reviewedEpochDay = day
        )
        val second = SpacedReviewEngine.schedule(
            cardId = "flashcard-one",
            current = first,
            rating = ReviewRating.GOOD,
            reviewedEpochDay = first.dueEpochDay
        )
        val reset = SpacedReviewEngine.schedule(
            cardId = "flashcard-one",
            current = second,
            rating = ReviewRating.AGAIN,
            reviewedEpochDay = second.dueEpochDay
        )

        assertEquals(1, first.intervalDays)
        assertEquals(3, second.intervalDays)
        assertEquals(2, second.repetitions)
        assertEquals(0, reset.repetitions)
        assertEquals(1, reset.intervalDays)
        assertTrue(reset.easeFactor < second.easeFactor)
    }

    @Test
    fun `due cards include unseen cards and overdue progress`() {
        val cards = listOf(
            Flashcard("flashcard-a", "A", "A"),
            Flashcard("flashcard-b", "B", "B"),
            Flashcard("flashcard-c", "C", "C")
        )
        val progress = mapOf(
            "flashcard-a" to FlashcardProgress("flashcard-a", dueEpochDay = 50),
            "flashcard-b" to FlashcardProgress("flashcard-b", dueEpochDay = 80)
        )

        val due = SpacedReviewEngine.dueCards(cards, progress, currentEpochDay = 60)

        assertEquals(listOf("flashcard-a", "flashcard-c"), due.map { it.id })
    }
}
