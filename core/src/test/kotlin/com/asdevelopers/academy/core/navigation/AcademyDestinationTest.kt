package com.asdevelopers.academy.core.navigation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AcademyDestinationTest {
    @Test
    fun `content destinations round trip stable ids`() {
        val destinations = listOf(
            AcademyDestination.Chapters("foundation level"),
            AcademyDestination.Lessons("chapter/fa-01"),
            AcademyDestination.Lesson("lesson fa 01"),
            AcademyDestination.LessonNotes("lesson fa 01"),
            AcademyDestination.Quiz("quiz+01"),
            AcademyDestination.QuizHistory("quiz+01"),
            AcademyDestination.Exercise("exercise/01"),
            AcademyDestination.Project("project 01")
        )

        destinations.forEach { destination ->
            assertEquals(destination, AcademyDestinationCodec.decode(AcademyDestinationCodec.encode(destination)))
        }
    }

    @Test
    fun `shared destinations have stable routes`() {
        assertEquals("academy/home", AcademyDestinationCodec.encode(AcademyDestination.Home))
        assertEquals("academy/search", AcademyDestinationCodec.encode(AcademyDestination.Search))
        assertEquals("academy/bookmarks", AcademyDestinationCodec.encode(AcademyDestination.Bookmarks))
        assertEquals("academy/settings", AcademyDestinationCodec.encode(AcademyDestination.Settings))
        assertEquals("academy/review/flashcards", AcademyDestinationCodec.encode(AcademyDestination.FlashcardReview))
    }

    @Test
    fun `unknown route safely falls back home`() {
        assertEquals(AcademyDestination.Home, AcademyDestinationCodec.decode("unknown/path"))
    }
}
