package com.asdevelopers.academy.course.model

import kotlinx.serialization.Serializable

/** سطح‌های استاندارد آموزشی AS Academy. */
@Serializable
enum class CourseLevelType {
    FUNDAMENTALS,
    BEGINNER,
    INTERMEDIATE,
    ADVANCED,
    SPECIALIST,
    PROJECT_BASED
}

/** نوع بلوک‌هایی که Lesson Renderer مرکزی قادر به نمایش آن‌هاست. */
@Serializable
enum class LessonBlockType {
    TITLE,
    SUBTITLE,
    PARAGRAPH,
    LIST,
    TABLE,
    IMAGE,
    DIAGRAM,
    CODE,
    OUTPUT,
    TIP,
    WARNING,
    NOTE,
    IMPORTANT,
    EXERCISE,
    QUIZ,
    /** لینک رسمی به پروژه عملی؛ نام آن با Course Contract یکسان نگه داشته می‌شود. */
    PROJECT_LINK,
    /** برای سازگاری محتوای آزمایشی اولیه نگه داشته شده و در Packageهای جدید استفاده نمی‌شود. */
    @Deprecated("Use PROJECT_LINK so the JSON contract and renderer stay consistent")
    PROJECT,
    REFERENCE
}

/** مدل عمومی سطح؛ مستقل از زبان برنامه‌نویسی یا موضوع دوره است. */
@Serializable
data class CourseLevel(
    val id: String,
    val courseId: String,
    val type: CourseLevelType,
    val title: String,
    val order: Int,
    /** توضیح کوتاه برای صفحه معرفی سطح اختیاری است. */
    val description: String = ""
)

/** مدل فصل آموزشی. */
@Serializable
data class Chapter(
    val id: String,
    val levelId: String,
    val title: String,
    val description: String,
    val order: Int,
    /** شناسه درس‌های پیش‌نیاز یا فصل‌های مرتبط بدون تغییر Stable ID نگهداری می‌شود. */
    val prerequisites: List<String> = emptyList()
)

/** مدل اصلی درس؛ متن و عناصر درس در blocks نگهداری می‌شوند. */
@Serializable
data class Lesson(
    val id: String,
    val chapterId: String,
    val title: String,
    val summary: String,
    val order: Int,
    val estimatedMinutes: Int,
    val blocks: List<LessonBlock>,
    /** Tagها برای جست‌وجو و تحلیل نقاط ضعف کاربر استفاده می‌شوند. */
    val tags: Set<String> = emptySet(),
    /** پیش‌نیازها با Stable ID درس‌های قبلی معرفی می‌شوند. */
    val prerequisites: List<String> = emptyList()
)

/**
 * یک واحد مستقل از محتوای درس.
 * content متن اصلی است و metadata برای اطلاعات اختیاری مانند language، assetId یا caption استفاده می‌شود.
 */
@Serializable
data class LessonBlock(
    val id: String,
    val type: LessonBlockType,
    val content: String,
    val metadata: Map<String, String> = emptyMap(),
    /** متن جایگزین برای تصویر و نمودار به دسترس‌پذیری کمک می‌کند. */
    val accessibilityLabel: String? = null
)
