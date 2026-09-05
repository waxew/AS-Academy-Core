package com.asdevelopers.academy.core.navigation

import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Platform-wide destination contract shared by Core, MainUi and Course hosts.
 *
 * This model is intentionally UI-agnostic: Core owns route identity and stable encoding while
 * MainUi owns how each destination is rendered. Course repositories should never define a second
 * route schema for destinations that already exist here.
 */
sealed interface AcademyDestination {
    data object Home : AcademyDestination
    data object Catalog : AcademyDestination
    data object Search : AcademyDestination
    data object Bookmarks : AcademyDestination
    data object Achievements : AcademyDestination
    data object Progress : AcademyDestination
    data object Settings : AcademyDestination
    data object Profile : AcademyDestination
    data object AppInfo : AcademyDestination
    data object Placement : AcademyDestination
    data object WeakTopicReview : AcademyDestination
    data object FlashcardReview : AcademyDestination

    data class Chapters(val levelId: String) : AcademyDestination
    data class Lessons(val chapterId: String) : AcademyDestination
    data class Lesson(val lessonId: String) : AcademyDestination
    data class LessonNotes(val lessonId: String) : AcademyDestination
    data class Quiz(val quizId: String) : AcademyDestination
    data class QuizHistory(val quizId: String) : AcademyDestination
    data class Exercise(val exerciseId: String) : AcademyDestination
    data class Project(val projectId: String) : AcademyDestination
}

/** Stable route codec used by saved state, deep links and navigation hosts. */
object AcademyDestinationCodec {
    fun encode(destination: AcademyDestination): String = when (destination) {
        AcademyDestination.Home -> "academy/home"
        AcademyDestination.Catalog -> "academy/catalog"
        AcademyDestination.Search -> "academy/search"
        AcademyDestination.Bookmarks -> "academy/bookmarks"
        AcademyDestination.Achievements -> "academy/achievements"
        AcademyDestination.Progress -> "academy/progress"
        AcademyDestination.Settings -> "academy/settings"
        AcademyDestination.Profile -> "academy/profile"
        AcademyDestination.AppInfo -> "academy/about"
        AcademyDestination.Placement -> "academy/placement"
        AcademyDestination.WeakTopicReview -> "academy/review/weak-topics"
        AcademyDestination.FlashcardReview -> "academy/review/flashcards"
        is AcademyDestination.Chapters -> "academy/level/${escape(destination.levelId)}"
        is AcademyDestination.Lessons -> "academy/chapter/${escape(destination.chapterId)}"
        is AcademyDestination.Lesson -> "academy/lesson/${escape(destination.lessonId)}"
        is AcademyDestination.LessonNotes -> "academy/lesson/${escape(destination.lessonId)}/notes"
        is AcademyDestination.Quiz -> "academy/quiz/${escape(destination.quizId)}"
        is AcademyDestination.QuizHistory -> "academy/quiz/${escape(destination.quizId)}/history"
        is AcademyDestination.Exercise -> "academy/exercise/${escape(destination.exerciseId)}"
        is AcademyDestination.Project -> "academy/project/${escape(destination.projectId)}"
    }

    fun decode(route: String): AcademyDestination {
        val segments = route.trim('/').split('/').filter(String::isNotBlank)
        if (segments.firstOrNull() != "academy") return AcademyDestination.Home

        return when {
            segments == listOf("academy", "home") -> AcademyDestination.Home
            segments == listOf("academy", "catalog") -> AcademyDestination.Catalog
            segments == listOf("academy", "search") -> AcademyDestination.Search
            segments == listOf("academy", "bookmarks") -> AcademyDestination.Bookmarks
            segments == listOf("academy", "achievements") -> AcademyDestination.Achievements
            segments == listOf("academy", "progress") -> AcademyDestination.Progress
            segments == listOf("academy", "settings") -> AcademyDestination.Settings
            segments == listOf("academy", "profile") -> AcademyDestination.Profile
            segments == listOf("academy", "about") -> AcademyDestination.AppInfo
            segments == listOf("academy", "placement") -> AcademyDestination.Placement
            segments == listOf("academy", "review", "weak-topics") -> AcademyDestination.WeakTopicReview
            segments == listOf("academy", "review", "flashcards") -> AcademyDestination.FlashcardReview
            segments.size == 3 && segments[1] == "level" -> AcademyDestination.Chapters(unescape(segments[2]))
            segments.size == 3 && segments[1] == "chapter" -> AcademyDestination.Lessons(unescape(segments[2]))
            segments.size == 3 && segments[1] == "lesson" -> AcademyDestination.Lesson(unescape(segments[2]))
            segments.size == 4 && segments[1] == "lesson" && segments[3] == "notes" -> AcademyDestination.LessonNotes(unescape(segments[2]))
            segments.size == 3 && segments[1] == "quiz" -> AcademyDestination.Quiz(unescape(segments[2]))
            segments.size == 4 && segments[1] == "quiz" && segments[3] == "history" -> AcademyDestination.QuizHistory(unescape(segments[2]))
            segments.size == 3 && segments[1] == "exercise" -> AcademyDestination.Exercise(unescape(segments[2]))
            segments.size == 3 && segments[1] == "project" -> AcademyDestination.Project(unescape(segments[2]))
            else -> AcademyDestination.Home
        }
    }

    private fun escape(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())
    private fun unescape(value: String): String = URLDecoder.decode(value, Charsets.UTF_8.name())
}
