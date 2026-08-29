package com.asdevelopers.academy.core.ui

/** Routeها در Core تعریف می‌شوند تا تمام اپ‌های Academy یک ساختار Navigation یکسان داشته باشند. */
object AcademyRoutes {
    const val HOME = "home"
    const val PROGRESS = "progress"
    const val BOOKMARKS = "bookmarks"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
    const val CHAPTERS = "chapters/{levelId}"
    const val LESSONS = "lessons/{chapterId}"
    const val LESSON = "lesson/{lessonId}"

    fun chapters(levelId: String) = "chapters/$levelId"
    fun lessons(chapterId: String) = "lessons/$chapterId"
    fun lesson(lessonId: String) = "lesson/$lessonId"
}
