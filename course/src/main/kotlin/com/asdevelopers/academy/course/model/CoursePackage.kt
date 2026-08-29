package com.asdevelopers.academy.course.model

/**
 * بسته محتوایی خوانده‌شده از assets یا منبع دانلودی.
 * این مدل فقط داده‌های اصلی درس را نگه می‌دارد و داده شخصی کاربر در Room Core ذخیره می‌شود.
 */
data class CoursePackage(
    val manifest: CourseManifest,
    val levels: List<CourseLevel>,
    val chapters: List<Chapter>,
    val lessons: List<Lesson>
) {
    fun chaptersFor(levelId: String): List<Chapter> =
        chapters.filter { it.levelId == levelId }.sortedBy { it.order }

    fun lessonsFor(chapterId: String): List<Lesson> =
        lessons.filter { it.chapterId == chapterId }.sortedBy { it.order }

    fun lesson(id: String): Lesson? = lessons.firstOrNull { it.id == id }
}
