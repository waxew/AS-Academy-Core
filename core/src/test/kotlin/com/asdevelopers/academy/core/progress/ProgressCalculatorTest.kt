package com.asdevelopers.academy.core.progress

import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressCalculatorTest {
    @Test fun percentIsClampedAndSafe() {
        assertEquals(0, ProgressCalculator.percent(0, 0))
        assertEquals(50, ProgressCalculator.percent(5, 10))
        assertEquals(100, ProgressCalculator.percent(12, 10))
    }
}
