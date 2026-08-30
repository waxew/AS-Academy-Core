package com.asdevelopers.academy.core.content

import com.asdevelopers.academy.core.validCourseBundle
import com.asdevelopers.academy.course.model.Flashcard
import com.asdevelopers.academy.course.model.FlashcardDifficulty
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Flashcard باید هم در JSON contract پایدار بماند و هم قبل از Release کامل Validation شود. */
class FlashcardContractTest {
    private fun validFlashcard() = Flashcard(
        id = "sample-flashcard-001",
        courseId = "sample",
        lessonId = "sample-lesson-001",
        front = "سؤال نمونه",
        back = "پاسخ نمونه",
        hint = "راهنما",
        tags = setOf("basics"),
        difficulty = FlashcardDifficulty.MEDIUM
    )

    @Test
    fun `valid flashcard passes package validation`() {
        val bundle = validCourseBundle().copy(flashcards = listOf(validFlashcard()))
        val result = CoursePackageValidator().validate(bundle)

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `flashcard cannot reference a missing lesson`() {
        val broken = validFlashcard().copy(lessonId = "sample-missing-lesson")
        val result = CoursePackageValidator().validate(validCourseBundle().copy(flashcards = listOf(broken)))

        assertFalse(result.isValid)
        assertTrue(result.errors.any { "sample-missing-lesson" in it })
    }

    @Test
    fun `flashcard front and back are required`() {
        val broken = validFlashcard().copy(front = "", back = "")
        val result = CoursePackageValidator().validate(validCourseBundle().copy(flashcards = listOf(broken)))

        assertFalse(result.isValid)
        assertTrue(result.errors.any { "empty front" in it })
        assertTrue(result.errors.any { "empty back" in it })
    }

    @Test
    fun `codec round trip preserves flashcard data`() {
        val original = validCourseBundle().copy(flashcards = listOf(validFlashcard()))
        val codec = CoursePackageCodec()
        val restored = codec.decode(codec.encode(original))

        assertEquals(original.flashcards, restored.flashcards)
    }

    @Test
    fun `old bundle without flashcards still decodes`() {
        val codec = CoursePackageCodec()
        val raw = codec.encode(validCourseBundle()).replace(Regex(",?\\s*\"flashcards\"\\s*:\\s*\\[\\s*]"), "")
        val restored = codec.decode(raw)

        assertTrue(restored.flashcards.isEmpty())
    }
}
