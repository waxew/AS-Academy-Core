package com.asdevelopers.academy.core.project

/** مدل عمومی پروژه مرحله‌ای؛ Courseهای مختلف فقط داده این مدل را تولید می‌کنند. */
data class LearningProject(
    val id: String,
    val title: String,
    val summary: String,
    val difficulty: String,
    val steps: List<String>
)
