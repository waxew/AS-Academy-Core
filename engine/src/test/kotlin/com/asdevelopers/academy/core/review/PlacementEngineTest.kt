package com.asdevelopers.academy.core.review

import com.asdevelopers.academy.course.model.CourseLevelType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Placement باید مرزهای چهار سطح و Fast Track را بدون ambiguity نگاشت کند. */
class PlacementEngineTest {
    private val policy = PlacementEngine.fourLevelPolicy()

    @Test
    fun `four level boundaries map to expected starting levels`() {
        assertEquals(CourseLevelType.FUNDAMENTALS, PlacementEngine.recommend(34, policy).levelType)
        assertEquals(CourseLevelType.BEGINNER, PlacementEngine.recommend(35, policy).levelType)
        assertEquals(CourseLevelType.ADVANCED, PlacementEngine.recommend(55, policy).levelType)
        assertEquals(CourseLevelType.SPECIALIST, PlacementEngine.recommend(75, policy).levelType)
    }

    @Test
    fun `high score enables fast track while fundamentals does not require weak review`() {
        val fundamentals = PlacementEngine.recommend(20, policy)
        val fastTrack = PlacementEngine.recommend(95, policy)

        assertFalse(fundamentals.reviewWeakTopics)
        assertFalse(fundamentals.fastTrack)
        assertTrue(fastTrack.reviewWeakTopics)
        assertTrue(fastTrack.fastTrack)
        assertEquals("مسیر فشرده تخصصی", fastTrack.title)
    }
}
