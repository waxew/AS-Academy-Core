package com.asdevelopers.academy.core.project

import kotlinx.serialization.Serializable

/** مرحله قابل ارزیابی یک پروژه عملی. */
@Serializable
data class ProjectMilestone(
    val id: String,
    val title: String,
    val description: String,
    val order: Int,
    val acceptanceCriteria: List<String> = emptyList()
)

/** پروژه آموزشی استاندارد که Course فقط محتوای اختصاصی آن را فراهم می‌کند. */
@Serializable
data class LearningProject(
    val id: String,
    val courseId: String,
    val title: String,
    val description: String,
    val difficulty: String,
    val estimatedMinutes: Int,
    val relatedLessonIds: List<String>,
    val milestones: List<ProjectMilestone>,
    val starterAssetId: String? = null,
    val solutionAssetId: String? = null,
    val tags: Set<String> = emptySet()
)

/** وضعیت ذخیره‌شده پروژه برای ادامه کار در اجرای بعدی برنامه. */
@Serializable
data class ProjectProgress(
    val courseId: String,
    val projectId: String,
    val completedMilestoneIds: Set<String> = emptySet(),
    val draft: String = "",
    val updatedAtEpochMillis: Long,
    val completedAtEpochMillis: Long? = null
)
