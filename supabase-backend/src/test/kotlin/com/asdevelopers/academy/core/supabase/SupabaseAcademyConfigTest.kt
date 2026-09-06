package com.asdevelopers.academy.core.supabase

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertEquals

class SupabaseAcademyConfigTest {
    @Test
    fun acceptsPublishableClientKey() {
        val config = SupabaseAcademyConfig(
            projectUrl = "https://example.supabase.co",
            publishableKey = "sb_publishable_example"
        )
        assertEquals("academy_course_releases", config.releaseTable)
        assertEquals("academy-courses", config.courseBucket)
    }

    @Test
    fun rejectsModernSecretKey() {
        assertFailsWith<IllegalArgumentException> {
            SupabaseAcademyConfig(
                projectUrl = "https://example.supabase.co",
                publishableKey = "sb_secret_should_never_be_in_an_apk"
            )
        }
    }

    @Test
    fun rejectsNonHttpsProjectUrl() {
        assertFailsWith<IllegalArgumentException> {
            SupabaseAcademyConfig(
                projectUrl = "http://example.supabase.co",
                publishableKey = "sb_publishable_example"
            )
        }
    }
}
