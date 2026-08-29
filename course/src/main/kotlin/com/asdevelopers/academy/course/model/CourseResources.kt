package com.asdevelopers.academy.course.model

import kotlinx.serialization.Serializable

/**
 * رنگ‌ها و اطلاعات ظاهری مختص هر دوره در Branding نگهداری می‌شوند.
 * Core فقط این مقادیر را مصرف می‌کند و هیچ رنگ اختصاصی زبان را Hard-code نمی‌کند.
 */
@Serializable
data class CourseBranding(
    val primaryColorHex: String,
    val secondaryColorHex: String,
    val accentColorHex: String,
    val logoAssetId: String? = null,
    val heroAssetId: String? = null,
    val iconAssetId: String? = null
)

/** نوع فایل‌های قابل قرارگیری در Course Package. */
@Serializable
enum class CourseAssetType {
    IMAGE,
    DIAGRAM,
    DOCUMENT,
    AUDIO,
    VIDEO,
    SOURCE_CODE,
    OTHER
}

/**
 * فهرست Assetها باعث می‌شود Loader قبل از نمایش محتوا وجود و صحت فایل‌ها را بررسی کند.
 */
@Serializable
data class CourseAsset(
    val id: String,
    val relativePath: String,
    val type: CourseAssetType,
    val mimeType: String,
    val sha256: String? = null,
    val sizeBytes: Long? = null,
    val accessibilityLabel: String? = null
)

/** واژه‌نامه واحدی که موتور Search و Glossary تمام دوره‌ها مصرف می‌کنند. */
@Serializable
data class GlossaryEntry(
    val id: String,
    val courseId: String,
    val term: String,
    val definition: String,
    val aliases: List<String> = emptyList(),
    val relatedLessonIds: List<String> = emptyList(),
    val tags: Set<String> = emptySet()
)

/** منبع معتبر یک درس یا فصل بدون وابستگی UI در قرارداد محتوا ذخیره می‌شود. */
@Serializable
data class CourseReference(
    val id: String,
    val title: String,
    val url: String,
    val lessonId: String? = null,
    val publisher: String? = null,
    val accessedAt: String? = null
)
