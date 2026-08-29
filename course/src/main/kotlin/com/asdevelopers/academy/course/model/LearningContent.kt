package com.asdevelopers.academy.course.model

/** سطح‌های استاندارد آموزشی AS Academy. */
enum class CourseLevelType {
    FUNDAMENTALS,
    BEGINNER,
    INTERMEDIATE,
    ADVANCED,
    SPECIALIST,
    PROJECT_BASED
}

/** نوع بلوک‌هایی که Lesson Renderer مرکزی قادر به نمایش آن‌هاست. */
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
    PROJECT,
    REFERENCE
}

/** مدل عمومی سطح؛ مستقل از زبان برنامه‌نویسی یا موضوع دوره است. */
data class CourseLevel(
    val id: String,
    val courseId: String,
    val type: CourseLevelType,
    val title: String,
    val order: Int
)

/** مدل فصل آموزشی. */
data class Chapter(
    val id: String,
    val levelId: String,
    val title: String,
    val description: String,
    val order: Int
)

/** مدل اصلی درس؛ متن و عناصر درس در blocks نگهداری می‌شوند. */
data class Lesson(
    val id: String,
    val chapterId: String,
    val title: String,
    val summary: String,
    val order: Int,
    val estimatedMinutes: Int,
    val blocks: List<LessonBlock>
)

/**
 * یک واحد مستقل از محتوای درس.
 * content متن اصلی است و metadata برای اطلاعات اختیاری مانند language، assetId یا caption استفاده می‌شود.
 */
data class LessonBlock(
    val id: String,
    val type: LessonBlockType,
    val content: String,
    val metadata: Map<String, String> = emptyMap()
)
