package com.asdevelopers.academy.core.version

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** رفتار Versioning مستقل App/Core/Content را تثبیت می‌کند. */
class SemanticVersionTest {
    @Test
    fun `stable version is newer than prerelease`() {
        assertTrue(SemanticVersion.parse("1.0.0") > SemanticVersion.parse("1.0.0-beta.1"))
    }

    @Test
    fun `invalid short version is rejected`() {
        assertNull(SemanticVersion.parseOrNull("1.0"))
    }

    @Test
    fun `build metadata does not change parsed numeric version`() {
        assertEquals(SemanticVersion(2, 1, 3), SemanticVersion.parse("2.1.3+build.7"))
    }

    @Test
    fun `numeric prerelease identifiers use numeric order`() {
        assertTrue(SemanticVersion.parse("1.0.0-beta.10") > SemanticVersion.parse("1.0.0-beta.2"))
    }

    @Test
    fun `invalid prerelease numeric leading zero is rejected`() {
        assertNull(SemanticVersion.parseOrNull("1.0.0-beta.01"))
    }
}
