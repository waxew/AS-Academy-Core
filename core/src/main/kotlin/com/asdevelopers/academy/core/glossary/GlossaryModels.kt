package com.asdevelopers.academy.core.glossary

/** واژه‌نامه مشترک تمام دوره‌ها. */
data class GlossaryEntry(
    val id: String,
    val term: String,
    val translation: String,
    val definition: String,
    val related: List<String> = emptyList()
)
