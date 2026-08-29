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
    const val LESSON = "academy/lesson/{lessonId}"

    /** ساخت Route درس در یک مکان از خطای encode شناسه جلوگیری می‌کند. */
    fun lesson(lessonId: String): String = "academy/lesson/${java.net.URLEncoder.encode(lessonId, Charsets.UTF_8.name())}"
}

/**
 * NavHost پایه صفحه‌های عمومی را ثبت می‌کند و Course فقط destinationهای اختصاصی را به Builder می‌افزاید.
 */
@Composable
fun AcademyNavHost(
    navController: NavHostController,
    home: @Composable () -> Unit,
    settings: @Composable () -> Unit,
    about: @Composable () -> Unit,
    lesson: @Composable (lessonId: String) -> Unit,
    additionalGraph: NavGraphBuilder.() -> Unit = {}
) {
    NavHost(navController = navController, startDestination = AcademyRoutes.HOME) {
        composable(AcademyRoutes.HOME) { home() }
        composable(AcademyRoutes.SETTINGS) { settings() }
        composable(AcademyRoutes.ABOUT) { about() }
        composable(AcademyRoutes.LESSON) { entry ->
            // نبود شناسه به رشته خالی تبدیل نمی‌شود؛ مقصد با ورودی نامعتبر به Home بازمی‌گردد.
            val lessonId = entry.arguments?.getString("lessonId")
            if (lessonId == null) home() else lesson(java.net.URLDecoder.decode(lessonId, Charsets.UTF_8.name()))
        }
        additionalGraph()
    }
}

/** Navigation به صفحات عمومی جایگزین ساخت Route دستی در Drawer می‌شود. */
fun NavHostController.openSettings() = navigate(AcademyRoutes.SETTINGS) { launchSingleTop = true }
fun NavHostController.openAbout() = navigate(AcademyRoutes.ABOUT) { launchSingleTop = true }
fun NavHostController.openLesson(lessonId: String) = navigate(AcademyRoutes.lesson(lessonId)) { launchSingleTop = true }
