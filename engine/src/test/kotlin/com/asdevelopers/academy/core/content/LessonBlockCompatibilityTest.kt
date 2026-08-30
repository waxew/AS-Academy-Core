package com.asdevelopers.academy.core.content

import com.asdevelopers.academy.course.model.LessonBlock
import com.asdevelopers.academy.course.model.LessonBlockType
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regression tests for Course JSON values that have already existed in AS Academy packages.
 *
 * Stable IDs protect user progress, while these serialization tests protect the textual JSON contract itself.
 * Removing a published enum value without a migration would make older offline Course Packages unreadable.
 */
class LessonBlockCompatibilityTest {

    @Test
    fun `legacy exercise link block remains readable`() {
        // This is the exact shape used by early Basic content packages before EXERCISE became canonical.
        val json =
            """
            {
              "id": "compat-exercise-link",
              "type": "EXERCISE_LINK",
              "content": "تمرین سازگاری",
              "metadata": {"exerciseId": "basic-ex-001"}
            }
            """.trimIndent()

        val block = Json.decodeFromString<LessonBlock>(json)

        // Reader compatibility is intentional; new writers should still emit the canonical EXERCISE value.
        assertEquals(LessonBlockType.EXERCISE_LINK, block.type)
        assertEquals("basic-ex-001", block.metadata["exerciseId"])
    }

    @Test
    fun `canonical exercise block remains preferred`() {
        val json =
            """
            {
              "id": "canonical-exercise",
              "type": "EXERCISE",
              "content": "تمرین استاندارد",
              "metadata": {"exerciseId": "basic-ex-002"}
            }
            """.trimIndent()

        val block = Json.decodeFromString<LessonBlock>(json)

        assertEquals(LessonBlockType.EXERCISE, block.type)
        assertEquals("basic-ex-002", block.metadata["exerciseId"])
    }
}
