package com.asdevelopers.academy.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

/** Routeهای عمومی در Core تعریف می‌شوند تا Back stack میان اپ‌ها رفتار یکسان داشته باشد. */
object AcademyRoutes {
    const val HOME = "academy/home"
    const val SETTINGS = "academy/settings"
    const val ABOUT = "academy/about"
    /** صفحه مرور Flashcard مقصد عمومی Course است و خود Deck داده را از Host دریافت می‌کند. */
    const val FLASHCARDS = "academy/flashcards"
    const val LESSON = "academy/lesson/{lessonId}"
    const val QUIZ = "academy/quiz/{quizId}"
    const val EXERCISE = "academy/exercise/{exerciseId}"
    const val PROJECT = "academy/project/{projectId}"

    /** ساخت Route درس در یک مکان از خطای encode شناسه جلوگیری می‌کند. */
    fun lesson(lessonId: String): String = "academy/lesson/${encode(lessonId)}"

    /** Quiz نیز با Stable ID و encode مرکزی باز می‌شود. */
    fun quiz(quizId: String): String = "academy/quiz/${encode(quizId)}"

    /** Exercise Route منطق ساخت URL را در Courseها تکرار نمی‌کند. */
    fun exercise(exerciseId: String): String = "academy/exercise/${encode(exerciseId)}"

    /** Project Route برای تمام Courseها قرارداد یکسان دارد. */
    fun project(projectId: String): String = "academy/project/${encode(projectId)}"

    /** شناسه‌های محتوا قبل از قرار گرفتن در Route به شکل امن encode می‌شوند. */
    private fun encode(value: String): String =
        java.net.URLEncoder.encode(value, Charsets.UTF_8.name())
}

/**
 * NavHost پایه صفحه‌های عمومی و فعالیت‌های یادگیری را ثبت می‌کند.
 * Course فقط مدل محتوا را پیدا می‌کند و Composable مشترک متناظر را به Slot می‌دهد.
 */
@Composable
fun AcademyNavHost(
    navController: NavHostController,
    home: @Composable () -> Unit,
    settings: @Composable () -> Unit,
    about: @Composable () -> Unit,
    lesson: @Composable (lessonId: String) -> Unit,
    quiz: @Composable (quizId: String) -> Unit = {},
    exercise: @Composable (exerciseId: String) -> Unit = {},
    project: @Composable (projectId: String) -> Unit = {},
    /** Slot عمومی Flashcard مقدار پیش‌فرض دارد تا Hostهای قدیمی بدون تغییر Source-compatible بمانند. */
    flashcards: @Composable () -> Unit = {},
    additionalGraph: NavGraphBuilder.() -> Unit = {}
) {
    // تمام مقصدهای مشترک در یک Graph قرار می‌گیرند تا Back behavior بین Courseها یکسان بماند.
    NavHost(navController = navController, startDestination = AcademyRoutes.HOME) {
        // صفحه اصلی توسط Host هر Course تأمین می‌شود.
        composable(AcademyRoutes.HOME) { home() }
        // Settings و About UI مشترک دارند اما داده/Branding از Host می‌آید.
        composable(AcademyRoutes.SETTINGS) { settings() }
        composable(AcademyRoutes.ABOUT) { about() }
        // Flashcard یک مقصد ثابت Course است و Stable ID کارت داخل Deck/Repository مدیریت می‌شود.
        composable(AcademyRoutes.FLASHCARDS) { flashcards() }
        // Stable ID درس decode و سپس به Host داده می‌شود.
        composable(AcademyRoutes.LESSON) { entry ->
            val lessonId = entry.arguments?.getString("lessonId")
            if (lessonId == null) home() else lesson(decode(lessonId))
        }
        // Stable ID آزمون به Screen عمومی Quiz متصل می‌شود.
        composable(AcademyRoutes.QUIZ) { entry ->
            val quizId = entry.arguments?.getString("quizId")
            if (quizId == null) home() else quiz(decode(quizId))
        }
        // Stable ID تمرین به workflow عمومی Exercise متصل می‌شود.
        composable(AcademyRoutes.EXERCISE) { entry ->
            val exerciseId = entry.arguments?.getString("exerciseId")
            if (exerciseId == null) home() else exercise(decode(exerciseId))
        }
        // Stable ID پروژه به workflow عمومی Project متصل می‌شود.
        composable(AcademyRoutes.PROJECT) { entry ->
            val projectId = entry.arguments?.getString("projectId")
            if (projectId == null) home() else project(decode(projectId))
        }
        // Course هنوز می‌تواند مقصد واقعاً اختصاصی خودش را بدون Fork کردن NavHost اضافه کند.
        additionalGraph()
    }
}

/** Decode شناسه در تمام Routeها با یک روش انجام می‌شود. */
private fun decode(value: String): String =
    java.net.URLDecoder.decode(value, Charsets.UTF_8.name())

/** Navigation به صفحات عمومی جایگزین ساخت Route دستی در Drawer و Lesson می‌شود. */
fun NavHostController.openSettings() = navigate(AcademyRoutes.SETTINGS) { launchSingleTop = true }
fun NavHostController.openAbout() = navigate(AcademyRoutes.ABOUT) { launchSingleTop = true }
fun NavHostController.openFlashcards() = navigate(AcademyRoutes.FLASHCARDS) { launchSingleTop = true }
fun NavHostController.openLesson(lessonId: String) = navigate(AcademyRoutes.lesson(lessonId)) { launchSingleTop = true }
fun NavHostController.openQuiz(quizId: String) = navigate(AcademyRoutes.quiz(quizId)) { launchSingleTop = true }
fun NavHostController.openExercise(exerciseId: String) = navigate(AcademyRoutes.exercise(exerciseId)) { launchSingleTop = true }
fun NavHostController.openProject(projectId: String) = navigate(AcademyRoutes.project(projectId)) { launchSingleTop = true }
