package com.asdevelopers.academy.core.ui

/** مسیرهای استاندارد Navigation برای تمام اپ‌های AS Academy. */
object AcademyRoutes {
    const val HOME = "home"
    const val PROGRESS = "progress"
    const val BOOKMARKS = "bookmarks"
    const val SEARCH = "search"
    const val EXERCISES = "exercises"
    const val EXERCISE = "exercise/{exerciseId}"
    const val QUIZZES = "quizzes"
    const val QUIZ = "quiz/{quizId}"
    const val PROJECTS = "projects"
    const val PROJECT = "project/{projectId}"
    const val GLOSSARY = "glossary"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
    const val CHAPTERS = "chapters/{levelId}"
    const val LESSONS = "lessons/{chapterId}"
    const val LESSON = "lesson/{lessonId}"

    fun chapters(levelId: String) = "chapters/$levelId"
    fun lessons(chapterId: String) = "lessons/$chapterId"
    fun lesson(lessonId: String) = "lesson/$lessonId"
    fun exercise(exerciseId: String) = "exercise/$exerciseId"
    fun quiz(quizId: String) = "quiz/$quizId"
    fun project(projectId: String) = "project/$projectId"
}
