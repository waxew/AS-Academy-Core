package com.asdevelopers.academy.core.update

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Hash و تصمیم Update از نصب فایل دستکاری‌شده یا Downgrade جلوگیری می‌کنند. */
class PackageIntegrityTest {
    @Test
    fun `known sha256 is stable`() {
        val hash = Sha256.digest("AS Academy".encodeToByteArray())
        assertEquals(64, hash.length)
        assertTrue(Sha256.matches("AS Academy".encodeToByteArray(), hash.uppercase()))
    }

    @Test
    fun `older candidate is blocked`() {
        assertEquals(
            UpdateDecision.DOWNGRADE_BLOCKED,
            CourseUpdatePlanner.decide("2.0.0", "1.9.0", "1.0.0", "1.0.0")
        )
    }

    @Test
    fun `new course requiring newer core is rejected`() {
        assertEquals(
            UpdateDecision.CORE_UPDATE_REQUIRED,
            CourseUpdatePlanner.decide("1.0.0", "1.1.0", "2.0.0", "1.5.0")
        )
    }

    @Test
    fun `invalid installed content version is not treated as first install`() {
        assertEquals(
            UpdateDecision.INVALID_VERSION,
            CourseUpdatePlanner.decide("broken", "1.1.0", "1.0.0", "1.0.0")
        )
    }
}
