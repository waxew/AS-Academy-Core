package com.asdevelopers.academy.core.content

import com.asdevelopers.academy.core.validCourseBundle
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Validator انتشار Package ناقص یا دارای Stable ID تکراری را مسدود می‌کند. */
class CoursePackageValidatorTest {
    private val validator = CoursePackageValidator()

    @Test
    fun `valid bundle passes validation`() {
        val result = validator.validate(validCourseBundle())
        assertTrue(result.isValid, result.errors.joinToString())
    }

    @Test
    fun `missing chapter reference fails validation`() {
        val bundle = validCourseBundle()
        val broken = bundle.copy(lessons = bundle.lessons.map { it.copy(chapterId = "missing-chapter") })
        val result = validator.validate(broken)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { "missing chapter" in it })
    }

    @Test
    fun `codec round trip preserves valid bundle`() {
        val bundle = validCourseBundle()
        val decoded = CoursePackageCodec().decode(CoursePackageCodec().encode(bundle))
        assertTrue(validator.validate(decoded).isValid)
    }
}
